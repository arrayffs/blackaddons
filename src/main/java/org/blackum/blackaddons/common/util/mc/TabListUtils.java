package org.blackum.blackaddons.common.util.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.regex.Pattern;

public class TabListUtils {
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");

    public static List<String> getTabListLines() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return Collections.emptyList();

        Collection<PlayerInfo> players = mc.getConnection().getOnlinePlayers();
        PlayerInfo[] playersArray;
        
        try {
            playersArray = players.toArray(new PlayerInfo[0]);
        } catch (ConcurrentModificationException e) {
            try {
                playersArray = players.toArray(new PlayerInfo[0]);
            } catch (ConcurrentModificationException e2) {
                return Collections.emptyList();
            }
        }

        List<String> lines = new ArrayList<>();
        for (PlayerInfo player : playersArray) {
            if (player == null) continue;
            Component name = player.getTabListDisplayName();
            String label = name != null ? name.getString() : player.getProfile().name();
            lines.add(STRIP_COLOR_PATTERN.matcher(label).replaceAll("").trim());
        }
        return lines;
    }
}
