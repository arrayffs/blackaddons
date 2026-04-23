package org.blackum.blackaddons.feature.rng;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.constants.Constants;
import org.blackum.blackaddons.common.util.mc.LocationUtils;
import org.blackum.blackaddons.service.BotIntegration;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class RngTracker {

    // Regex patterns ported from SkyHanni
    // private static final Pattern DROP_PATTERN = Pattern.compile(
    // "§[0-9a-fk-or]§l(RARE|CRAZY RARE|PRAY TO RNGESUS) DROP! (?:§.)*(.*?)(?:
    // §r§b\\(\\+§r§b([0-9,.]+)%? §r§b✯ Magic Find§r§b\\))?.*");

    public static void onChatMessage(Component message) {
        if (!ConfigManager.data.rngTrackerEnabled)
            return;

        String stripped = message.getString();

        if (stripped.contains(Constants.RARE_DROP_KEYWORD) || stripped.contains(Constants.PRAY_DROP_KEYWORD)) {
            handleDrop(stripped);
        }
    }

    private static final Pattern STRIPPED_PATTERN = Pattern.compile(
            "(RARE|CRAZY RARE|PRAY TO RNGESUS) DROP! (.*?)(?: \\(\\+([0-9,.]+)%? " + Constants.MAGIC_FIND_LABEL
                    + "\\))?$");

    private static void handleDrop(String text) {
        Matcher matcher = STRIPPED_PATTERN.matcher(text);
        if (matcher.find()) {
            String rarity = matcher.group(1);
            String item = matcher.group(2).trim();
            String mf = matcher.group(3);

            if (mf == null)
                mf = "0";

            String player = Minecraft.getInstance().getUser().getName();

            Blackaddons.LOGGER.info("RNG Drop Detected: " + item + " (" + rarity + ")");

            String location = LocationUtils.getLocation();

            String localCat = "Unknown";
            if (location.contains("Catacombs")) {
                localCat = "Dungeons";
            } else if (rarity.contains("PRAY")) {
                localCat = "Slayers";
            }

            LocalRngManager.getInstance().addDrop(localCat, item, 1);

            BotIntegration.sendRngDrop(player, item, rarity, location);
        }
    }
}
