package org.blackum.blackaddons.feature.dungeon.solver.puzzle.waterboard;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.event.DungeonEvent;
import org.blackum.blackaddons.common.event.PlayerInteractEvent;

import org.blackum.blackaddons.common.util.mc.LocationUtils;
import org.blackum.blackaddons.common.util.ThreadUtils;
import org.blackum.blackaddons.feature.dungeon.util.ScanUtils;
import org.blackum.blackaddons.feature.dungeon.map.DungeonMap;
import org.blackum.blackaddons.feature.dungeon.map.Room;
import org.blackum.blackaddons.client.render.RenderContext;

public class WaterBoardHandler {

    public static void register() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(context -> {
            if (!ConfigManager.data.waterBoardSolverEnabled)
                return;

            MultiBufferSource.BufferSource bufSource;
            if (context.consumers() instanceof MultiBufferSource.BufferSource bs) {
                bufSource = bs;
            } else {
                bufSource = Minecraft.getInstance().renderBuffers().bufferSource();
            }

            RenderContext ctx = new RenderContext(
                    context.matrices(),
                    bufSource,
                    0.0f);
            WaterBoardSolver.onRenderWorld(ctx);
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!ConfigManager.data.waterBoardSolverEnabled)
                return InteractionResult.PASS;
            if (!world.isClientSide())
                return InteractionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            PlayerInteractEvent.RIGHT_CLICK.BLOCK event = new PlayerInteractEvent.RIGHT_CLICK.BLOCK(pos);
            WaterBoardSolver.onInteract(event);

            return InteractionResult.PASS;
        });

        ThreadUtils.loop(200, () -> !ConfigManager.data.waterBoardSolverEnabled, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null)
                return;

            mc.execute(() -> {
                if (WaterBoardSolver.INSTANCE.isInactive()) {
                    manualTrigger();
                } else {
                    BlockPos playerPos = mc.player.blockPosition();
                    BlockPos center = WaterBoardSolver.INSTANCE.getRoomCenter();

                    if (center != null) {
                        double dx = playerPos.getX() - center.getX();
                        double dz = playerPos.getZ() - center.getZ();
                        double distSq2D = dx * dx + dz * dz;
                        if (distSq2D > 64 * 64) {
                            WaterBoardSolver.reset();
                            return;
                        }
                    }

                    int idx = (playerPos.getX() + 185) / 32 * 6 + (playerPos.getZ() + 185) / 32;
                    if (idx >= 0 && idx < 36) {
                        Room.Tile tile = DungeonMap.getTileGrid()[idx];
                        if (tile != null && tile.owner != null && tile.owner.data != null) {
                            if (!"Water Board".equals(tile.owner.data.name)) {
                                double dx = playerPos.getX() - center.getX();
                                double dz = playerPos.getZ() - center.getZ();
                                if (dx * dx + dz * dz > 28 * 28) {
                                    WaterBoardSolver.reset();
                                }
                            }
                        }
                    }
                }
            });
        });
    }

    public static void manualTrigger() {
        if (!ConfigManager.data.waterBoardSolverEnabled)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
            return;
        if (!LocationUtils.inDungeons())
            return;

        BlockPos playerPos = mc.player.blockPosition();

        for (int x = -16; x <= 16; x++) {
            for (int z = -16; z <= 16; z++) {
                for (int y = 56; y <= 75; y++) {
                    BlockPos pos = new BlockPos(playerPos.getX() + x, y, playerPos.getZ() + z);
                    if (!mc.level.getBlockState(pos).is(Blocks.LEVER))
                        continue;

                    for (int rot : new int[] { 0, 90, 180, 270 }) {
                        int woolCount = 0;
                        int validCount = 0;
                        for (int i = 0; i < 5; i++) {
                            BlockPos gateOffset = new BlockPos(0, -4, 10 + i);
                            BlockPos gatePos = ScanUtils.getRealCoord(gateOffset, pos, rot);
                            BlockState state = mc.level.getBlockState(gatePos);
                            String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                                    .getKey(state.getBlock()).getPath();
                            if (path.contains("wool") || path.contains("clay") || path.contains("terracotta")) {
                                woolCount++;
                                validCount++;
                            } else if (state.isAir() || path.contains("water") || path.contains("glass")) {
                                validCount++;
                            }
                        }
                        if (validCount == 5 && woolCount >= 1) {
                            triggerRoomEntry(pos, rot, pos);
                            return;
                        }
                    }
                }
            }
        }
    }

    public static void triggerRoomEntry(BlockPos center, int rotation, BlockPos leverPos) {
        DungeonEvent.RoomEvent.onEnter event = new DungeonEvent.RoomEvent.onEnter(center, rotation, leverPos);
        WaterBoardSolver.onRoomEnter(event);
    }
}
