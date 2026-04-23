package org.blackum.blackaddons.gui.hud;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.util.mc.LocationUtils;
import org.blackum.blackaddons.feature.dungeon.map.Door;
import org.blackum.blackaddons.feature.dungeon.map.DungeonMap;
import org.blackum.blackaddons.feature.dungeon.map.DungeonScoreboard;
import org.blackum.blackaddons.feature.dungeon.map.Room;
import org.blackum.blackaddons.feature.dungeon.map.Vec2i;
import org.blackum.blackaddons.gui.render.RoundedFillRenderState;
import org.blackum.blackaddons.mixin.gui.GuiGraphicsAccessor;
import org.joml.Matrix3x2f;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;

public class DungeonMapHud implements HudElement {

    public static void register() {
        HudRegistry.register(new DungeonMapHud());
    }

    @Override
    public String id() {
        return "dungeon_map";
    }

    @Override
    public String displayName() {
        return "Dungeon Map";
    }

    @Override
    public boolean enabled() {
        return ConfigManager.data.dungeonMapEnabled && LocationUtils.inDungeons();
    }

    @Override
    public int x() {
        return ConfigManager.data.dungeonMapX;
    }

    @Override
    public int y() {
        return ConfigManager.data.dungeonMapY;
    }

    @Override
    public void setPos(int x, int y) {
        ConfigManager.data.dungeonMapX = x;
        ConfigManager.data.dungeonMapY = y;
    }

    @Override
    public int width() {
        return ConfigManager.data.dungeonMapSize;
    }

    @Override
    public int height() {
        return ConfigManager.data.dungeonMapSize;
    }

    @Override
    public void reset() {
        ConfigManager.data.dungeonMapX = 4;
        ConfigManager.data.dungeonMapY = 4;
        ConfigManager.data.dungeonMapSize = 120;
    }

    @Override
    public boolean resizable() {
        return true;
    }

    @Override
    public void setSize(int w, int h) {
        int s = Math.max(32, Math.min(400, Math.max(w, h)));
        ConfigManager.data.dungeonMapSize = s;
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker tracker) {
        render(graphics);
    }

    private static final Object MARKER_SELF = safeRL("blackaddons", "textures/map/marker_self.png");

    private static Object safeRL(String namespace, String path) {
        try {
            Class<?> rlClass;
            try {
                rlClass = Class.forName("net.minecraft.util.Identifier");
            } catch (ClassNotFoundException e) {
                rlClass = Class.forName("net.minecraft.resources.ResourceLocation");
            }
            try {
                java.lang.reflect.Method m = rlClass.getMethod("fromNamespaceAndPath", String.class, String.class);
                return m.invoke(null, namespace, path);
            } catch (Exception e) {
            }
            try {
                return rlClass.getConstructor(String.class, String.class).newInstance(namespace, path);
            } catch (Exception e) {
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static void render(GuiGraphics g) {
        if (!ConfigManager.data.dungeonMapEnabled)
            return;
        if (!LocationUtils.inDungeons())
            return;

        int x = ConfigManager.data.dungeonMapX;
        int y = ConfigManager.data.dungeonMapY;
        int size = ConfigManager.data.dungeonMapSize;

        g.fill(x, y, x + size, y + size, org.blackum.blackaddons.gui.render.Theme.withAlpha(ConfigManager.data.dungeonMapBackgroundColor, ConfigManager.data.dungeonMapBackgroundOpacity));

        Vec2i sc = DungeonMap.getStartCoords();
        Integer roomSizeI = DungeonMap.getRoomSize();
        Vec2i ms = DungeonMap.getMapSize();
        boolean funnyMap = ConfigManager.data.dungeonFunnyMap;

        if (funnyMap) {
            if (ms == null) {
                int maxGx = 0, maxGz = 0;
                for (Room r : DungeonMap.getRooms()) {
                    for (Room.Tile t : r.tiles) {
                        int gx = (t.pos.x + 185) / 32;
                        int gz = (t.pos.z + 185) / 32;
                        if (gx > maxGx)
                            maxGx = gx;
                        if (gz > maxGz)
                            maxGz = gz;
                    }
                }
                if (maxGx > 0 || maxGz > 0)
                    ms = new Vec2i(maxGx + 1, maxGz + 1);
            }
        }

        if (roomSizeI == null)
            roomSizeI = 16;
        if (sc == null)
            sc = new Vec2i(11, 11);
        if (ms == null)
            ms = new Vec2i(6, 6);

        int rs = roomSizeI;
        int cellSize = rs + 4;
        int padding = 4;
        int inner = size - padding * 2;
        int cW = (ms.x - 1) * cellSize + rs;
        int cH = (ms.z - 1) * cellSize + rs;
        float scale = Math.min(inner / (float) cW, inner / (float) cH);
        int offX = (inner - (int) (cW * scale)) / 2;
        int offY = (inner - (int) (cH * scale)) / 2;
        int dX = x + padding + offX;
        int dY = y + padding + offY;

        g.enableScissor(x, y, x + size, y + size);

        Set<Room> rooms = DungeonMap.getRooms();

        int discoveredPuzzles = 0;
        boolean trapDiscovered = false;
        for (Room r : rooms) {
            Room.State s = r.state;
            if (s == Room.State.UNDISCOVERED || s == Room.State.UNOPENED)
                continue;
            if (r.type == Room.Type.PUZZLE)
                discoveredPuzzles++;
            if (r.type == Room.Type.TRAP)
                trapDiscovered = true;
        }
        int totalPuzzles = DungeonScoreboard.stats.puzzleCount;
        boolean allPuzzlesKnown = totalPuzzles > 0 && discoveredPuzzles >= totalPuzzles;

        float darknessFactor = 1.0f - ConfigManager.data.dungeonMapUndiscoveredDarkness;

        for (Room room : rooms) {
            if (room.tiles.isEmpty())
                continue;

            if (room.type == Room.Type.ENTRANCE && room.state == Room.State.DISCOVERED) {
                room.state = Room.State.CLEARED;
            }

            Room.Type eff = effectiveType(room, allPuzzlesKnown, trapDiscovered);
            drawTiles(g, room, eff, dX, dY, cellSize, rs, scale, allPuzzlesKnown, trapDiscovered, funnyMap, darknessFactor);
        }

        for (Room room : rooms) {
            if (room.tiles.isEmpty())
                continue;
            boolean undiscovered = room.state == Room.State.UNDISCOVERED;
            boolean unopened = room.state == Room.State.UNOPENED;
            boolean isSpecial = (room.type == Room.Type.PUZZLE || room.type == Room.Type.TRAP);
            if (!funnyMap && (undiscovered || (unopened && !isSpecial))) {
                drawUndiscoveredTile(g, room, dX, dY, cellSize, rs, scale);
            }
        }

        for (Door door : DungeonMap.getDoors()) {
            if (!door.isSeen() && !(funnyMap && door.worldScanned))
                continue;
            float[] dp = door.placement(8f, rs);
            Vec2i dsz = door.size(8f, rs);

            if (dsz.x <= 4 && dsz.z <= 4)
                continue;

            int dclr = doorColor(door);
            if (funnyMap && !door.isSeen()) {
                dclr = darken(dclr, darknessFactor);
            }
            float dx1 = dX + Math.round(dp[0] * scale);
            float dy1 = dY + Math.round(dp[1] * scale);
            float dx2 = dX + Math.round((dp[0] + dsz.x) * scale);
            float dy2 = dY + Math.round((dp[1] + dsz.z) * scale);
            fillRounded(g, dx1, dy1, dx2, dy2, 0f, dclr);
        }

        Minecraft mc = Minecraft.getInstance();
        for (Room room : rooms) {
            if (room.tiles.isEmpty())
                continue;

            Room.Type eff = effectiveType(room, allPuzzlesKnown, trapDiscovered);

            boolean undiscovered = room.state == Room.State.UNDISCOVERED;
            boolean unopened = room.state == Room.State.UNOPENED;
            boolean isSpecial = (room.type == Room.Type.PUZZLE || room.type == Room.Type.TRAP);

            int cx, cz;
            if (!funnyMap && (undiscovered || (unopened && !isSpecial))) {
                Vec2i gp = room.entryTile;
                if (gp == null) {
                    Room.Tile t = getAdjacentTile(room);
                    if (t != null)
                        gp = new Vec2i((t.pos.x + 185) / 32, (t.pos.z + 185) / 32);
                }

                if (gp == null)
                    continue;

                int gx = gp.x;
                int gz = gp.z;
                float x1 = snapX(dX, gx * cellSize, scale);
                float z1 = snapY(dY, gz * cellSize, scale);
                float x2 = snapX(dX, gx * cellSize + rs, scale);
                float z2 = snapY(dY, gz * cellSize + rs, scale);
                cx = (int) ((x1 + x2) * 0.5f);
                cz = (int) ((z1 + z2) * 0.5f);
            } else {
                cx = centerX(room, dX, cellSize, rs, scale);
                cz = centerZ(room, dY, cellSize, rs, scale);
            }

            boolean ambiguous = isSpecial && unopened && !allPuzzlesKnown && !trapDiscovered
                    && !(funnyMap && room.data != null);
            drawOverlay(g, mc, room, eff, cx, cz, scale, ambiguous, funnyMap, darknessFactor);
        }

        if (sc != null) {
            DungeonScoreboard.DungeonPlayer self = DungeonScoreboard.selfPlayer;
            if (self != null && self.hasMapPos) {
                int px = dX + (int) ((self.mapX - sc.x) * scale);
                int pz = dY + (int) ((self.mapZ - sc.z) * scale);
                float drawYaw = self.yaw;

                if (mc.player != null) {
                    float relX = (float) mc.player.getX() + 185f;
                    float relZ = (float) mc.player.getZ() + 185f;

                    float tx = sc.x + (rs / 2f) + (relX * (rs + 4f) / 32f);
                    float tz = sc.z + (rs / 2f) + (relZ * (rs + 4f) / 32f);

                    px = dX + (int) ((tx - sc.x) * scale);
                    pz = dY + (int) ((tz - sc.z) * scale);
                    drawYaw = mc.player.getYRot();
                }

                drawPlayerPin(g, px, pz, 0xFF00FF00, drawYaw);
            }
            for (DungeonScoreboard.DungeonPlayer p : DungeonScoreboard.teammates) {
                if (p.dead || !p.hasMapPos)
                    continue;
                int px = dX + (int) ((p.mapX - sc.x) * scale);
                int pz = dY + (int) ((p.mapZ - sc.z) * scale);
                drawPlayerDot(g, px, pz, classColor(p.dungeonClass), p.yaw);
            }
        }

        g.disableScissor();
        drawBorder(g, x, y, size);
    }

    private static void fillRounded(GuiGraphics g, float x0, float y0, float x1, float y1, float radius, int color) {
        GuiRenderState state = ((GuiGraphicsAccessor) g).getGuiRenderState();
        Matrix3x2f pose = new Matrix3x2f(g.pose());
        state.submitGuiElement(new RoundedFillRenderState(pose, x0, y0, x1, y1, radius, color, null));
    }

    private static void drawTiles(GuiGraphics g, Room room, Room.Type eff,
            int dX, int dY, int cellSize, int rs, float scale,
            boolean allPuzzlesKnown, boolean trapDiscovered, boolean funnyMap, float darkness) {
        boolean undiscovered = room.state == Room.State.UNDISCOVERED;
        boolean unopened = room.state == Room.State.UNOPENED;
        boolean isSpecial = (room.type == Room.Type.PUZZLE || room.type == Room.Type.TRAP);
        boolean ambiguous = isSpecial && unopened && !allPuzzlesKnown && !trapDiscovered;

        if (!funnyMap && (undiscovered || (unopened && !isSpecial))) {
            return;
        }

        float tileRadius = ConfigManager.data.dungeonMapCornerRadius * scale;
        List<float[]> rects = computeRoomRects(room, dX, dY, cellSize, rs, scale);

        if (ambiguous && !(funnyMap && room.data != null)) {
            int clrP = darken(ConfigManager.data.dungeonMapColorPuzzle, darkness);
            int clrT = darken(ConfigManager.data.dungeonMapColorTrap, darkness);
            for (float[] r : rects) {
                float mx = Math.round((r[0] + r[2]) * 0.5f);
                fillRounded(g, r[0], r[1], mx, r[3], tileRadius, clrP);
                fillRounded(g, mx, r[1], r[2], r[3], tileRadius, clrT);
            }
            return;
        }

        boolean shouldDarken = unopened || (funnyMap && undiscovered);
        int clr = room.mimic ? darken(ConfigManager.data.dungeonMapColorMimic, shouldDarken ? darkness : 1.0f)
                : shouldDarken ? darken(baseColor(eff), darkness)
                : baseColor(eff);

        for (float[] r : rects) {
            fillRounded(g, r[0], r[1], r[2], r[3], tileRadius, clr);
        }
    }

    private static void drawUndiscoveredTile(GuiGraphics g, Room room,
            int dX, int dY, int cellSize, int rs, float scale) {
        Vec2i gp = room.entryTile;
        if (gp == null) {
            Room.Tile t = getAdjacentTile(room);
            if (t != null)
                gp = new Vec2i((t.pos.x + 185) / 32, (t.pos.z + 185) / 32);
        }
        if (gp == null)
            return;

        int gx = gp.x;
        int gz = gp.z;
        float x1 = snapX(dX, gx * cellSize, scale);
        float z1 = snapY(dY, gz * cellSize, scale);
        float x2 = snapX(dX, gx * cellSize + rs, scale);
        float z2 = snapY(dY, gz * cellSize + rs, scale);
        int grey = ConfigManager.data.dungeonMapColorUndiscovered;
        float tileRadius = ConfigManager.data.dungeonMapCornerRadius * scale;
        fillRounded(g, x1, z1, x2, z2, tileRadius, grey);
    }

    private static float snapX(int dX, int mapX, float scale) {
        return dX + Math.round(mapX * scale);
    }

    private static float snapY(int dY, int mapY, float scale) {
        return dY + Math.round(mapY * scale);
    }

    private static List<float[]> computeRoomRects(Room room, int dX, int dY, int cellSize, int rs, float scale) {
        List<float[]> rects = new ArrayList<>();
        if (room.tiles.isEmpty()) return rects;

        int minGx = Integer.MAX_VALUE, maxGx = Integer.MIN_VALUE;
        int minGz = Integer.MAX_VALUE, maxGz = Integer.MIN_VALUE;
        for (Room.Tile t : room.tiles) {
            int gx = (t.pos.x + 185) / 32;
            int gz = (t.pos.z + 185) / 32;
            if (gx < minGx) minGx = gx;
            if (gx > maxGx) maxGx = gx;
            if (gz < minGz) minGz = gz;
            if (gz > maxGz) maxGz = gz;
        }

        int bbArea = (maxGx - minGx + 1) * (maxGz - minGz + 1);
        if (bbArea == room.tiles.size()) {
            rects.add(new float[] {
                    snapX(dX, minGx * cellSize, scale),
                    snapY(dY, minGz * cellSize, scale),
                    snapX(dX, maxGx * cellSize + rs, scale),
                    snapY(dY, maxGz * cellSize + rs, scale)
            });
            return rects;
        }

        boolean isLShape = room.tiles.size() == 3 && bbArea == 4
                && (maxGx - minGx) == 1 && (maxGz - minGz) == 1;
        if (isLShape) {
            int missingGx = minGx, missingGz = minGz;
            outer:
            for (int gx = minGx; gx <= maxGx; gx++) {
                for (int gz = minGz; gz <= maxGz; gz++) {
                    if (!hasTileAt(room, gx, gz)) {
                        missingGx = gx;
                        missingGz = gz;
                        break outer;
                    }
                }
            }

            int cornerGx = (minGx + maxGx) - missingGx;
            int cornerGz = (minGz + maxGz) - missingGz;

            rects.add(new float[] {
                    snapX(dX, minGx * cellSize, scale),
                    snapY(dY, cornerGz * cellSize, scale),
                    snapX(dX, maxGx * cellSize + rs, scale),
                    snapY(dY, cornerGz * cellSize + rs, scale)
            });
            rects.add(new float[] {
                    snapX(dX, cornerGx * cellSize, scale),
                    snapY(dY, minGz * cellSize, scale),
                    snapX(dX, cornerGx * cellSize + rs, scale),
                    snapY(dY, maxGz * cellSize + rs, scale)
            });
            return rects;
        }

        for (int gz = minGz; gz <= maxGz; gz++) {
            int startX = -1;
            for (int gx = minGx; gx <= maxGx + 1; gx++) {
                if (hasTileAt(room, gx, gz)) {
                    if (startX == -1) startX = gx;
                } else {
                    if (startX != -1) {
                        rects.add(new float[] {
                                snapX(dX, startX * cellSize, scale),
                                snapY(dY, gz * cellSize, scale),
                                snapX(dX, (gx - 1) * cellSize + rs, scale),
                                snapY(dY, gz * cellSize + rs, scale)
                        });
                        startX = -1;
                    }
                }
            }
        }

        for (int gx = minGx; gx <= maxGx; gx++) {
            int startZ = -1;
            for (int gz = minGz; gz <= maxGz + 1; gz++) {
                if (hasTileAt(room, gx, gz)) {
                    if (startZ == -1) startZ = gz;
                } else {
                    if (startZ != -1) {
                        rects.add(new float[] {
                                snapX(dX, gx * cellSize, scale),
                                snapY(dY, startZ * cellSize, scale),
                                snapX(dX, gx * cellSize + rs, scale),
                                snapY(dY, (gz - 1) * cellSize + rs, scale)
                        });
                        startZ = -1;
                    }
                }
            }
        }
        return rects;
    }

    private static void drawOverlay(GuiGraphics g, Minecraft mc, Room room, Room.Type eff,
            int cx, int cz, float scale, boolean ambiguous, boolean funnyMap, float darkness) {
        boolean hasName = room.data != null;
        if (ConfigManager.data.dungeonMapShowRoomNames && hasName && room.state != Room.State.UNDISCOVERED && room.state != Room.State.UNOPENED) {
            int nameColor = ConfigManager.data.dungeonMapColorNameDiscovered;
            if (room.state == Room.State.GREEN)
                nameColor = ConfigManager.data.dungeonMapColorNameCompleted;
            else if (room.state == Room.State.CLEARED)
                nameColor = ConfigManager.data.dungeonMapColorNameCleared;
            
            drawName(g, mc, cx, cz, room.data.name, nameColor);
            return;
        }

        switch (room.state) {
            case GREEN:
                if (eff != Room.Type.FAIRY && eff != Room.Type.ENTRANCE) {
                    int greenColor = ConfigManager.data.dungeonMapColorNameCompleted;
                    drawCheckProcedural(g, cx, cz, greenColor, darken(greenColor, 0.4f));
                }
                break;
            case CLEARED:
                if (eff != Room.Type.FAIRY && eff != Room.Type.ENTRANCE) {
                    int clearedColor = ConfigManager.data.dungeonMapColorNameCleared;
                    drawCheckProcedural(g, cx, cz, clearedColor, darken(clearedColor, 0.5f));
                }
                break;
            case FAILED:
                drawXMark(g, mc, cx, cz, 0xFFFF5555);
                break;
            case DISCOVERED:
                break;
            case UNDISCOVERED:
                if (hasName && funnyMap)
                    drawName(g, mc, cx, cz, room.data.name, darken(0xFFFFFFFF, darkness));
                else
                    drawQuestionMark(g, mc, cx, cz, darken(ConfigManager.data.dungeonMapColorUndiscovered, darkness));
                break;
            case UNOPENED:
                if (hasName && funnyMap) {
                    drawName(g, mc, cx, cz, room.data.name, darken(0xFFFFFFFF, darkness));
                } else if (ambiguous) {
                    drawQuestionMark(g, mc, cx, cz, darken(0xFFAAAAAA, darkness));
                } else if (room.type != Room.Type.PUZZLE && room.type != Room.Type.TRAP) {
                    drawQuestionMark(g, mc, cx, cz, darken(0xFFAAAAAA, darkness));
                }
                break;
            default:
                break;
        }
    }

    private static void drawCheckProcedural(GuiGraphics g, int cx, int cy, int color, int shadowColor) {
        int x = cx - 6;
        int y = cy - 5;

        g.fill(x + 1, y + 7, x + 3, y + 9, shadowColor);
        g.fill(x + 3, y + 9, x + 5, y + 11, shadowColor);
        g.fill(x + 5, y + 7, x + 7, y + 9, shadowColor);
        g.fill(x + 7, y + 5, x + 9, y + 7, shadowColor);
        g.fill(x + 9, y + 3, x + 11, y + 5, shadowColor);
        g.fill(x + 11, y + 1, x + 13, y + 3, shadowColor);

        g.fill(x, y + 6, x + 2, y + 8, color);
        g.fill(x + 2, y + 8, x + 4, y + 10, color);
        g.fill(x + 4, y + 6, x + 6, y + 8, color);
        g.fill(x + 6, y + 4, x + 8, y + 6, color);
        g.fill(x + 8, y + 2, x + 10, y + 4, color);
        g.fill(x + 10, y, x + 12, y + 2, color);
    }

    private static void drawCheckmark(GuiGraphics g, Minecraft mc, int cx, int cz, int color) {
        int h = mc.font.lineHeight / 2;
        scaled(g, cx, cz, 1.5f, () -> g.drawCenteredString(mc.font, "\u2713", 0, -h, color));
    }

    private static void drawXMark(GuiGraphics g, Minecraft mc, int cx, int cz, int color) {
        int h = mc.font.lineHeight / 2;
        scaled(g, cx, cz, 1.5f, () -> g.drawCenteredString(mc.font, "\u2717", 0, -h, color));
    }

    private static void drawQuestionMark(GuiGraphics g, Minecraft mc, int cx, int cz, int color) {
        int h = mc.font.lineHeight / 2;
        scaled(g, cx, cz, 1.5f, () -> g.drawCenteredString(mc.font, "?", 0, -h, color));
    }

    private static void drawName(GuiGraphics g, Minecraft mc, int cx, int cz, String name, int color) {
        int fontH = mc.font.lineHeight;
        float scale = ConfigManager.data.dungeonMapRoomNameScale;
        
        String[] words = name.split(" ");
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int maxWidth = (int)(24 / scale);
        
        for (String w : words) {
            if (sb.length() > 0 && mc.font.width(sb.toString() + " " + w) > maxWidth) {
                lines.add(sb.toString());
                sb = new StringBuilder(w);
            } else {
                if (sb.length() > 0) sb.append(" ");
                sb.append(w);
            }
        }
        if (sb.length() > 0) lines.add(sb.toString());

        float totalH = lines.size() * fontH * scale;
        float startY = cz - totalH / 2f;

        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            float curY = startY + i * fontH * scale + (fontH * scale / 2f);
            scaled(g, cx, (int)curY, scale, () -> g.drawCenteredString(mc.font, line, 0, -(fontH / 2), color));
        }
    }


    private static void scaled(GuiGraphics g, int cx, int cz, float s, Runnable draw) {
        g.pose().pushMatrix();
        g.pose().translate((float) cx, (float) cz);
        g.pose().scale(s, s);
        draw.run();
        g.pose().popMatrix();
    }

    private static Room.Type effectiveType(Room room, boolean allPuzzlesKnown, boolean trapDiscovered) {
        if (room.type == Room.Type.PUZZLE || room.type == Room.Type.TRAP) {
            if (room.state == Room.State.UNOPENED) {
                if (trapDiscovered)
                    return Room.Type.PUZZLE;
                if (allPuzzlesKnown)
                    return Room.Type.TRAP;
            }
        }
        return room.type;
    }

    private static int baseColor(Room.Type type) {
        switch (type) {
            case ENTRANCE:
                return ConfigManager.data.dungeonMapColorEntrance;
            case BLOOD:
                return ConfigManager.data.dungeonMapColorBlood;
            case PUZZLE:
                return ConfigManager.data.dungeonMapColorPuzzle;
            case TRAP:
                return ConfigManager.data.dungeonMapColorTrap;
            case CHAMPION:
                return ConfigManager.data.dungeonMapColorChampion;
            case RARE:
                return 0xFFFFFFFF;
            case FAIRY:
                return ConfigManager.data.dungeonMapColorFairy;
            default:
                return ConfigManager.data.dungeonMapColorNormal;
        }
    }

    private static int darken(int argb, float f) {
        int a = (argb >> 24) & 0xFF;
        int r = (int) (((argb >> 16) & 0xFF) * f);
        int ge = (int) (((argb >> 8) & 0xFF) * f);
        int b = (int) ((argb & 0xFF) * f);
        return (a << 24) | (r << 16) | (ge << 8) | b;
    }

    private static int doorColor(Door door) {
        if (door.type == Door.Type.BLOOD)
            return ConfigManager.data.dungeonMapColorBlood;
        if (door.type == Door.Type.WITHER)
            return 0xFF111111;
        return ConfigManager.data.dungeonMapColorNormal;
    }

    private static boolean hasTileAt(Room room, int gx, int gz) {
        for (Room.Tile t : room.tiles)
            if ((t.pos.x + 185) / 32 == gx && (t.pos.z + 185) / 32 == gz)
                return true;
        return false;
    }

    private static int centerX(Room room, int dX, int cellSize, int rs, float scale) {
        if (room.shape == Room.Shape.SL) {
            Room.Tile anchor = findAnchorTile(room);
            int gx = (anchor.pos.x + 185) / 32;
            return dX + (int) ((gx * cellSize + rs / 2f) * scale);
        }
        if (room.tiles.isEmpty())
            return dX;
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (Room.Tile t : room.tiles) {
            int g = (t.pos.x + 185) / 32;
            if (g < min)
                min = g;
            if (g > max)
                max = g;
        }
        return dX + (int) (((min + max) / 2f * cellSize + rs / 2f) * scale);
    }

    private static int centerZ(Room room, int dY, int cellSize, int rs, float scale) {
        if (room.shape == Room.Shape.SL) {
            Room.Tile anchor = findAnchorTile(room);
            int gz = (anchor.pos.z + 185) / 32;
            return dY + (int) ((gz * cellSize + rs / 2f) * scale);
        }
        if (room.tiles.isEmpty())
            return dY;
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (Room.Tile t : room.tiles) {
            int g = (t.pos.z + 185) / 32;
            if (g < min)
                min = g;
            if (g > max)
                max = g;
        }
        return dY + (int) (((min + max) / 2f * cellSize + rs / 2f) * scale);
    }

    private static Room.Tile findAnchorTile(Room room) {
        if (room.tiles.size() == 1)
            return room.tiles.get(0);

        Room.Tile best = null;
        int maxNeighbors = -1;
        double minDistance = Double.MAX_VALUE;

        double avgGx = 0, avgGz = 0;

        for (Room.Tile t : room.tiles) {
            avgGx += (t.pos.x + 185) / 32.0;
            avgGz += (t.pos.z + 185) / 32.0;
        }
        avgGx /= room.tiles.size();
        avgGz /= room.tiles.size();

        for (Room.Tile t : room.tiles) {
            int gx = (t.pos.x + 185) / 32;
            int gz = (t.pos.z + 185) / 32;
            int neighbors = 0;
            if (hasTileAt(room, gx + 1, gz))
                neighbors++;
            if (hasTileAt(room, gx - 1, gz))
                neighbors++;
            if (hasTileAt(room, gx, gz + 1))
                neighbors++;
            if (hasTileAt(room, gx, gz - 1))
                neighbors++;

            double dist = Math.pow(gx - avgGx, 2) + Math.pow(gz - avgGz, 2);

            if (neighbors > maxNeighbors || (neighbors == maxNeighbors && dist < minDistance)) {
                maxNeighbors = neighbors;
                minDistance = dist;
                best = t;
            }
        }
        return best != null ? best : room.tiles.get(0);
    }

    private static Room.Tile getAdjacentTile(Room room) {
        Room.Tile best = null;
        double minDist = Double.MAX_VALUE;
        for (Door door : room.doors) {
            for (Room adj : door.rooms) {
                if (adj == room)
                    continue;
                Room.State s = adj.state;
                if (s != Room.State.UNDISCOVERED && s != Room.State.UNOPENED) {
                    for (Room.Tile t : room.tiles) {
                        double dx = (t.pos.x + 16.0) - door.pos.x;
                        double dz = (t.pos.z + 16.0) - door.pos.z;

                        if (Math.abs(dx) > 36.0 || Math.abs(dz) > 36.0)
                            continue;

                        double dist = dx * dx + dz * dz;

                        if (dist < minDist) {
                            minDist = dist;
                            best = t;
                        } else if (Math.abs(dist - minDist) < 0.1 && best != null) {
                            if (Math.abs(t.pos.x + 16.0 - door.pos.x) < 2.0
                                    || Math.abs(t.pos.z + 16.0 - door.pos.z) < 2.0) {
                                best = t;
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    private static int classColor(String cls) {
        if (cls == null)
            return 0xFFAAAAAA;
        switch (cls.toUpperCase()) {
            case "HEALER":
                return 0xFFFF4444;
            case "MAGE":
                return 0xFF4444FF;
            case "BERSERK":
                return 0xFFFF4400;
            case "ARCHER":
                return 0xFF44FF44;
            case "TANK":
                return 0xFF44AAFF;
            default:
                return 0xFFAAAAAA;
        }
    }

    private static void drawPlayerPin(GuiGraphics g, int cx, int cy, int color, float yaw) {
        g.pose().pushMatrix();
        try {
            g.pose().translate((float) cx, (float) cy);

            for (Method m : g.pose().getClass().getMethods()) {
                if ((m.getName().equals("rotate") || m.getName().equals("rotateZ") || m.getName().equals("mulPose"))
                        && m.getParameterCount() == 1) {
                    try {
                        Class<?> pType = m.getParameterTypes()[0];
                        if (pType == float.class) {
                            m.invoke(g.pose(), (float) Math.toRadians(yaw + 180));
                            break;
                        } else if (pType.getName().endsWith("Quaternionf")) {
                            m.invoke(g.pose(), new org.joml.Quaternionf().rotationZ((float) Math.toRadians(yaw + 180)));
                            break;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            int size = 12;
            int x = -size / 2;
            int y = -size / 2;

            boolean drawn = false;
            if (MARKER_SELF != null) {
                for (Method m : g.getClass().getMethods()) {
                    if ((m.getName().equals("blit") || m.getName().equals("drawTexture"))
                            && m.getParameterCount() == 9) {
                        try {
                            m.invoke(g, MARKER_SELF, x, y, 0f, 0f, size, size, size, size);
                            drawn = true;
                            break;
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            if (!drawn) {
                int dark = 0xFF000000;
                g.fill(-1, -5, 2, -3, dark);
                g.fill(-3, -3, 4, 5, dark);
                g.fill(-2, 5, 3, 6, dark);
                g.fill(0, -4, 1, -3, color);
                g.fill(-2, -3, 3, 5, color);
                g.fill(-2, -3, -1, 4, 0x40FFFFFF);
            }
        } finally {
            g.pose().popMatrix();
        }
    }

    private static void drawPlayerDot(GuiGraphics g, int cx, int cy, int color, float yaw) {
        int r = (color >> 16) & 0xFF, ge = (color >> 8) & 0xFF, b = color & 0xFF;
        int bc = 0xFF000000 | ((r / 2) << 16) | ((ge / 2) << 8) | (b / 2);
        g.fill(cx - 2, cy - 1, cx + 3, cy, bc);
        g.fill(cx - 1, cy - 2, cx + 2, cy - 1, bc);
        g.fill(cx - 2, cy, cx + 3, cy + 1, color);
        g.fill(cx - 1, cy - 1, cx + 2, cy, color);
        double rad = Math.toRadians(yaw);
        int dx = (int) Math.round(-Math.sin(rad) * 2);
        int dz = (int) Math.round(Math.cos(rad) * 2);
        g.fill(cx + dx, cy + dz, cx + dx + 1, cy + dz + 1, 0xFFFFFFFF);
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int size) {
        if (!ConfigManager.data.dungeonMapBorderEnabled) return;
        int color = ConfigManager.data.dungeonMapColorBorder;
        int t = ConfigManager.data.dungeonMapBorderThickness;
        g.fill(x, y, x + size, y + t, color);
        g.fill(x, y + size - t, x + size, y + size, color);
        g.fill(x, y, x + t, y + size, color);
        g.fill(x + size - t, y, x + size, y + size, color);
    }
}
