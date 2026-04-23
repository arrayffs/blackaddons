package org.blackum.blackaddons.feature.dungeon.map;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.blackum.blackaddons.common.constants.Constants;
import org.blackum.blackaddons.common.util.mc.LocationUtils;
import org.blackum.blackaddons.common.util.mc.ScoreboardUtils;
import org.blackum.blackaddons.common.util.mc.TabListUtils;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

public class DungeonScoreboard {

    public static class DungeonPlayer {
        public final String name;
        public String dungeonClass;
        public boolean dead;

        public volatile float mapX = 0f;
        public volatile float mapZ = 0f;
        public volatile float yaw = 0f;
        public volatile boolean hasMapPos = false;

        public DungeonPlayer(String name, String dungeonClass) {
            this.name = name;
            this.dungeonClass = dungeonClass;
        }
    }

    private static final java.util.concurrent.ExecutorService playerHeadScope = java.util.concurrent.Executors
            .newCachedThreadPool();
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.Future<?>> playerJobs = new java.util.concurrent.ConcurrentHashMap<>();

    private static void smoothUpdatePlayer(DungeonPlayer player, float targetX, float targetZ, float targetYaw) {
        if (player.mapX == 0f && player.mapZ == 0f && player.yaw == 0f) {
            player.mapX = targetX;
            player.mapZ = targetZ;
            player.yaw = targetYaw;
            return;
        }

        if (player.mapX == targetX && player.mapZ == targetZ && player.yaw == targetYaw) {
            java.util.concurrent.Future<?> oldJob = playerJobs.remove(player.name);
            if (oldJob != null)
                oldJob.cancel(true);
            return;
        }

        java.util.concurrent.Future<?> existingJob = playerJobs.get(player.name);
        if (existingJob != null) {
            existingJob.cancel(true);
        }

        java.util.concurrent.Future<?> newJob = playerHeadScope.submit(() -> {
            float startX = player.mapX;
            float startZ = player.mapZ;
            float startYaw = player.yaw;

            long animationDuration = 250L;
            long startTime = System.currentTimeMillis();
            float progress = 0f;

            while (progress < 1f && !Thread.currentThread().isInterrupted()) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                progress = Math.min((float) elapsedTime / animationDuration, 1f);

                player.mapX = startX + (targetX - startX) * progress;
                player.mapZ = startZ + (targetZ - startZ) * progress;

                float diff = targetYaw - startYaw;
                while (diff > 180f)
                    diff -= 360f;
                while (diff < -180f)
                    diff += 360f;
                player.yaw = startYaw + diff * progress;

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (!Thread.currentThread().isInterrupted()) {
                player.mapX = targetX;
                player.mapZ = targetZ;
                player.yaw = targetYaw;
            }
        });
        playerJobs.put(player.name, newJob);
    }

    public static class Stats {
        public String elapsedTime = "0s";
        public int secretsFound = 0;
        public float secretsPercent = 0f;
        public int crypts = 0;
        public int deaths = 0;
        public int puzzleCount = 0;
        public int openedRooms = 0;
        public int completedRooms = 0;
        public int percentCleared = 0;
        public boolean princeKilled = false;
        public boolean mimicKilled = false;
    }

    private static final Pattern TAB_PLAYER = Pattern
            .compile("^\\[(\\d+)] (?:\\[\\w+] )*(\\w+) .*?\\((\\w+)(?:\\s+\\w+)*\\)$");
    private static final Pattern P_TIME = Pattern.compile("^\\s*Time(?:\\s+Elapsed)?:\\s*(.+)$");
    private static final Pattern P_CRYPTS = Pattern.compile("^\\s*Crypts:\\s*(\\d+).*$");
    private static final Pattern P_DEATHS = Pattern.compile("^\\s*(?:Team )?Deaths:\\s*(\\d+).*$");
    private static final Pattern P_PUZZLES = Pattern.compile("^\\s*Puzzles:\\s*\\((\\d+)\\).*$");
    private static final Pattern P_OPENED = Pattern.compile("^\\s*Opened Rooms:\\s*(\\d+).*$");
    private static final Pattern P_SECRETS = Pattern.compile("^\\s*Secrets Found:\\s*(\\d+).*$");
    private static final Pattern P_SECRETS_PCT = Pattern.compile("^\\s*Secrets Found:\\s*([\\d.]+)%.*$");
    private static final Pattern P_COMPLETED = Pattern.compile("^\\s*Completed Rooms:\\s*(\\d+).*$");
    private static final Pattern P_CLEARED = Pattern.compile("^\\s*Cleared:\\s*(\\d+)%.*$");
    private static final Pattern P_PRINCE = Pattern.compile("^A Prince falls\\. \\+1 Bonus Score$");
    private static final Pattern P_MIMIC = Pattern.compile("^Mimic Killed!.*$");

    public static final List<DungeonPlayer> teammates = new ArrayList<>();
    public static DungeonPlayer selfPlayer = null;
    public static final Stats stats = new Stats();

    private static Room pendingRoom = null;
    private static int roomEntryConfirmation = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!LocationUtils.inDungeons())
                return;
            if (client.player != null && selfPlayer == null) {
                selfPlayer = new DungeonPlayer(client.player.getName().getString(), "UNKNOWN");
            }

            if (client.player != null) {
                int px = (int) client.player.getX();
                int pz = (int) client.player.getZ();

                int ox = Math.floorMod(px + 185, 32);
                int oz = Math.floorMod(pz + 185, 32);
                int b = Constants.DUNGEON_ROOM_DETECTION_BUFFER;

                Room detectedRoom = null;
                if (ox >= b && ox < 32 - b && oz >= b && oz < 32 - b) {
                    int idx = (px + 185) / 32 * 6 + (pz + 185) / 32;
                    if (idx >= 0 && idx < 36) {
                        Room.Tile tile = DungeonMap.getTileGrid()[idx];
                        if (tile != null && tile.owner != null) {
                            detectedRoom = tile.owner;
                        }
                    }
                }

                if (detectedRoom != null && detectedRoom != DungeonMap.getLocalRoom()) {
                    if (detectedRoom == pendingRoom) {
                        roomEntryConfirmation++;
                        if (roomEntryConfirmation >= 25) {
                            DungeonMap.updateLocalRoom(detectedRoom);
                            pendingRoom = null;
                            roomEntryConfirmation = 0;
                        }
                    } else {
                        pendingRoom = detectedRoom;
                        roomEntryConfirmation = 1;
                    }
                } else {
                    pendingRoom = null;
                    roomEntryConfirmation = 0;
                }
            }

            parseTabList();
            parseSidebar();
        });
    }



    public static void reset() {
        teammates.clear();
        selfPlayer = null;
        for (java.util.concurrent.Future<?> job : playerJobs.values())
            job.cancel(true);
        playerJobs.clear();
        stats.elapsedTime = "0s";
        stats.secretsFound = 0;
        stats.secretsPercent = 0f;
        stats.crypts = 0;
        stats.deaths = 0;
        stats.puzzleCount = 0;
        stats.openedRooms = 0;
        stats.completedRooms = 0;
        stats.percentCleared = 0;
        stats.princeKilled = false;
        stats.mimicKilled = false;
    }

    public static void onChatMessage(Component message) {
        String text = message.getString().replaceAll("§[0-9a-fk-or]", "").trim();
        if (P_PRINCE.matcher(text).matches())
            stats.princeKilled = true;
        if (P_MIMIC.matcher(text).matches())
            stats.mimicKilled = true;
    }

    public static void updateMapPositions(ClientboundMapItemDataPacket packet) {
        if (packet.decorations().isEmpty())
            return;
        List<MapDecoration> decorations = packet.decorations().get();

        Minecraft mc = Minecraft.getInstance();
        String selfName = mc.player != null ? mc.player.getName().getString() : null;

        List<DungeonPlayer> living = new ArrayList<>();
        for (DungeonPlayer p : teammates) {
            if (!p.dead)
                living.add(p);
        }

        int teammateIdx = 0;
        for (MapDecoration decor : decorations) {
            if (isPlayerMarker(decor)) {
                if (teammateIdx == 0) {
                    if (selfPlayer == null) {
                        selfPlayer = new DungeonPlayer(selfName != null ? selfName : "Unknown", "UNKNOWN");
                    }
                    float tx = ((decor.x() + 128) & 0xFF) >> 1;
                    float tz = ((decor.y() + 128) & 0xFF) >> 1;
                    float tyaw = (decor.rot() & 0xFF) * 360f / 16f;
                    selfPlayer.hasMapPos = true;
                    smoothUpdatePlayer(selfPlayer, tx, tz, tyaw);
                    teammateIdx++;
                } else {
                    int idx = teammateIdx - 1;
                    if (idx < living.size()) {
                        DungeonPlayer p = living.get(idx);
                        float tx = ((decor.x() + 128) & 0xFF) >> 1;
                        float tz = ((decor.y() + 128) & 0xFF) >> 1;
                        float tyaw = (decor.rot() & 0xFF) * 360f / 16f;
                        p.hasMapPos = true;
                        smoothUpdatePlayer(p, tx, tz, tyaw);
                    }
                    teammateIdx++;
                }
            }
        }
    }

    private static boolean isPlayerMarker(MapDecoration decor) {
        return true;
    }

    private static void parseTabList() {
        List<String> lines = TabListUtils.getTabListLines();
        Minecraft mc = Minecraft.getInstance();
        String selfName = mc.player != null ? mc.player.getName().getString() : null;

        List<DungeonPlayer> found = new ArrayList<>();
        for (String line : lines) {
            Matcher m = TAB_PLAYER.matcher(line);
            if (!m.matches())
                continue;
            String name = m.group(2);
            String cls = m.group(3);
            boolean dead = "DEAD".equals(cls);

            DungeonPlayer existing = findByName(name);
            if (existing == null) {
                existing = new DungeonPlayer(name, dead ? "UNKNOWN" : cls);
            }
            existing.dead = dead;
            if (!dead && !cls.equals(existing.dungeonClass))
                existing.dungeonClass = cls;
            found.add(existing);
        }

        if (!found.isEmpty()) {
            teammates.clear();
            for (DungeonPlayer p : found) {
                if (p.name.equals(selfName)) {
                    selfPlayer = p;
                } else {
                    teammates.add(p);
                }
            }
        }
    }

    private static void parseSidebar() {
        List<String> lines = ScoreboardUtils.getCleanSidebarLines();
        for (String line : lines) {
            Matcher m;
            if ((m = P_TIME.matcher(line)).matches()) {
                stats.elapsedTime = m.group(1);
                continue;
            }
            if ((m = P_CRYPTS.matcher(line)).matches()) {
                stats.crypts = parseInt(m.group(1));
                continue;
            }
            if ((m = P_DEATHS.matcher(line)).matches()) {
                stats.deaths = parseInt(m.group(1));
                continue;
            }
            if ((m = P_PUZZLES.matcher(line)).matches()) {
                stats.puzzleCount = parseInt(m.group(1));
                continue;
            }
            if ((m = P_OPENED.matcher(line)).matches()) {
                stats.openedRooms = parseInt(m.group(1));
                continue;
            }
            if ((m = P_SECRETS.matcher(line)).matches()) {
                stats.secretsFound = parseInt(m.group(1));
                continue;
            }
            if ((m = P_SECRETS_PCT.matcher(line)).matches()) {
                stats.secretsPercent = parseFloat(m.group(1));
                continue;
            }
            if ((m = P_COMPLETED.matcher(line)).matches()) {
                stats.completedRooms = parseInt(m.group(1));
                continue;
            }
            if ((m = P_CLEARED.matcher(line)).matches()) {
                stats.percentCleared = parseInt(m.group(1));
            }
        }
    }

    private static DungeonPlayer findByName(String name) {
        if (selfPlayer != null && selfPlayer.name.equals(name))
            return selfPlayer;
        for (DungeonPlayer p : teammates) {
            if (p.name.equals(name))
                return p;
        }
        return null;
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static float parseFloat(String s) {
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }
}
