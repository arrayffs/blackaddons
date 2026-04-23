package org.blackum.blackaddons.feature.cheat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.util.mc.LocationUtils;
import org.blackum.blackaddons.feature.chat.ChatActionExecutor;
import org.blackum.blackaddons.feature.chat.ChatUtils;
import org.blackum.blackaddons.feature.rotation.RotationManager;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class RelicLook {
    private static final Pattern RELIC_PICKUP_PATTERN = Pattern.compile("^([A-Za-z0-9_]+) picked the Corrupted (Red|Orange) Relic!$");
    private static final BlockPos ARCHER_TARGET = new BlockPos(51, 6, 42); // red
    private static final BlockPos BERSERK_TARGET = new BlockPos(57, 7, 42); // orange

    private static boolean isActive = false;
    private static BlockPos targetBlock = null;

    private static void debugMsg(String msg) {
        if (ConfigManager.data.RelicLookDebug) {
            ChatUtils.send_debug("§e[RelicLook] §f" + msg);
        }
    }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> onChatMessage(message));
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> onChatMessage(message));
        ClientTickEvents.END_CLIENT_TICK.register(RelicLook::onTick);
    }

    private static void onChatMessage(Component message) {
        if (!ConfigManager.data.RelicLookEnabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        if (!LocationUtils.inDungeons() || LocationUtils.getF7Phase() != 5) {
            if (isActive) {
                debugMsg("Phase changed, deactivating Relic Look.");
                isActive = false;
            }
            return;
        }

        String cleanText = message.getString().replaceAll("(?i)§[0-9A-FK-ORX]", "").trim();
        Matcher matcher = RELIC_PICKUP_PATTERN.matcher(cleanText);
        
        if (matcher.find()) {
            String playerName = matcher.group(1);
            String relicColor = matcher.group(2);
            debugMsg(playerName + " picked up " + relicColor + " Relic");
            
            if (playerName.equals(mc.player.getName().getString()) || playerName.equals(mc.player.getScoreboardName())) {
                debugMsg("We picked up the " + relicColor + " relic.");

                if ("Red".equalsIgnoreCase(relicColor)) {
                    targetBlock = ARCHER_TARGET;
                    isActive = true;
                    debugMsg("Activated for Red Relic. Rotating to " + targetBlock.toShortString());
                } else if ("Orange".equalsIgnoreCase(relicColor)) {
                    targetBlock = BERSERK_TARGET;
                    isActive = true;
                    debugMsg("Activated for Orange Relic. Rotating to " + targetBlock.toShortString());
                } else {
                    debugMsg("Color conditions not met for activation.");
                }
            }
        }
    }

    private static void onTick(Minecraft mc) {
        if (!ConfigManager.data.RelicLookEnabled || mc.player == null) {
            isActive = false;
            return;
        }
        
        if (isActive && LocationUtils.getF7Phase() != 5) {
            isActive = false;
            return;
        }

        if (isActive && targetBlock != null) {
            double dx = mc.player.getX() - (targetBlock.getX() + 0.5);
            double dy = mc.player.getY() - (targetBlock.getY());
            double dz = mc.player.getZ() - (targetBlock.getZ() + 0.5);
            double distSq = dx * dx + dy * dy + dz * dz;

            if (mc.player.tickCount % 20 == 0) {
                debugMsg(String.format("Distance to target: %.2f blocks", Math.sqrt(distSq)));
            }

            if (distSq <= 4.5 * 4.5) {
                debugMsg("Within " + 4.5 + " blocks! Executing target attack...");
                List<ConfigManager.ActionStep> actions = new ArrayList<>();
                actions.add(new ConfigManager.ActionStep(ConfigManager.ActionStepType.ATTACK, 0, "", 0, 0));
                ChatActionExecutor.getInstance().execute(actions, null);
                
                isActive = false;
                RotationManager.getInstance().clearSpline();
                debugMsg("Operation completed and deactivated.");
            } else {
                RotationManager.getInstance().rotateToBlock(targetBlock.getX() + 0.5, targetBlock.getY() + 0.5, targetBlock.getZ() + 0.5);
            }
        }
    }

}
