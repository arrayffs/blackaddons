package org.blackum.blackaddons.feature.dungeon.solver.puzzle.tpmaze;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.util.ThreadUtils;
import org.blackum.blackaddons.common.util.mc.LocationUtils;
import org.blackum.blackaddons.feature.dungeon.map.DungeonMap;
import org.blackum.blackaddons.feature.dungeon.map.Room;
import org.blackum.blackaddons.client.render.RenderContext;

public class TpMazeHandler {

    private static final int RESET_DISTANCE_SQ = 30 * 30;

    public static void register() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ConfigManager.data.teleportMazeSolverEnabled) {
                TpMazeSolver.onClientTick();
            }
        });
        
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(context -> {
            if (!ConfigManager.data.teleportMazeSolverEnabled) return;

            MultiBufferSource.BufferSource bufSource;
            if (context.consumers() instanceof MultiBufferSource.BufferSource bs) {
                bufSource = bs;
            } else {
                bufSource = Minecraft.getInstance().renderBuffers().bufferSource();
            }

            RenderContext ctx = new RenderContext(context.matrices(), bufSource, 0.0f);
            TpMazeSolver.onRenderWorld(ctx);
        });

        ThreadUtils.loop(200, () -> false, () -> {
            if (!ConfigManager.data.teleportMazeSolverEnabled) {
                if (TpMazeSolver.isActive()) TpMazeSolver.reset();
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            mc.execute(() -> {
                if (!LocationUtils.inDungeons()) {
                    TpMazeSolver.reset();
                    return;
                }

                BlockPos playerPos = mc.player.blockPosition();

                if (TpMazeSolver.isActive()) {
                    BlockPos center = TpMazeSolver.getMazeCenter();
                    if (center != null) {
                        double dx = playerPos.getX() - center.getX();
                        double dz = playerPos.getZ() - center.getZ();
                        if (dx * dx + dz * dz > RESET_DISTANCE_SQ) TpMazeSolver.reset();
                    }
                    return;
                }

                int idx = (playerPos.getX() + 185) / 32 * 6 + (playerPos.getZ() + 185) / 32;
                if (idx < 0 || idx >= 36) return;

                Room.Tile[] grid = DungeonMap.getTileGrid();
                Room.Tile tile = (grid != null) ? grid[idx] : null;
                if (tile == null || tile.owner == null || tile.owner.data == null) return;
                if (!"Teleport Maze".equals(tile.owner.data.name)) return;

                int gx = (playerPos.getX() + 185) / 32;
                int gz = (playerPos.getZ() + 185) / 32;
                BlockPos roomCenter = new BlockPos(-185 + gx * 32 + 15, 69, -185 + gz * 32 + 15);
                TpMazeSolver.scan(roomCenter);
            });
        });
    }

    public static void onServerTeleportPre(ClientboundPlayerPositionPacket packet) {
        if (!ConfigManager.data.teleportMazeSolverEnabled) return;
        if (!LocationUtils.inDungeons()) return;
        TpMazeSolver.onServerTeleportPacket(packet);
    }

    public static void onServerTeleportPost() {
        if (!ConfigManager.data.teleportMazeSolverEnabled) return;
        if (!LocationUtils.inDungeons()) return;
        TpMazeSolver.onServerTeleportPost();
    }
}
