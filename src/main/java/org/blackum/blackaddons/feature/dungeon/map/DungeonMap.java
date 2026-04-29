package org.blackum.blackaddons.feature.dungeon.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.model.DungeonFloor;
import org.blackum.blackaddons.common.util.mc.LocationUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class DungeonMap {

    private static Integer mapId      = null;
    private static Vec2i  startCoords = null;
    private static Vec2i  mapCenter   = null;
    private static Vec2i  mapSize     = null;
    private static Integer roomSize   = null;
    private static int specialColumn  = 5;

    private static final Set<Room> rooms    = new HashSet<>();
    private static final List<Door> doors   = new ArrayList<>();
    private static final Room.Tile[] tileGrid = new Room.Tile[36];
    private static Room bloodRoom = null;
    private static Room localRoom = null;

    public static void onMapPacket(ClientboundMapItemDataPacket packet) {
        if (!ConfigManager.data.dungeonMapEnabled) return;
        if (!LocationUtils.inDungeons()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        int packetMapId = packet.mapId().id();
        
        Integer invMapId = findDungeonMapId();
        if (invMapId != null) {
            mapId = invMapId;
        }

        if (mapId == null) {
            mapId = packetMapId;
        } else if (mapId != packetMapId) {
            return;
        }

        MapItemSavedData mapData = mc.level.getMapData(packet.mapId());
        if (mapData == null) return;
        byte[] colors = mapData.colors;
        int nonZero = 0;
        for (byte c : colors) if (c != 0) nonZero++;
        if (nonZero == 0) return;

        if (startCoords == null) {
            DungeonFloor floor = LocationUtils.getCurrentFloor();
            if (!initializeSizes(colors, getFloorNumber(floor))) {
                if (LocationUtils.debugDungeonMode) {
                    roomSize = 16;
                    startCoords = new Vec2i(11, 11);
                    mapCenter = new Vec2i(-121, -121);
                    mapSize = new Vec2i(5, 5);
                } else {
                    return;
                }
            }
        }

        updateRoomTiles(colors);
        updateRoomState(colors);
        scanDoors(colors);
        propagateBloodRoomType();
        DungeonScoreboard.updateMapPositions(packet);
    }

    private static boolean initializeSizes(byte[] colors, int floorNumber) {
        int start = -1, run = 0;
        int greenStart = -1, greenLength = -1;
        for (int i = 0; i < colors.length; i++) {
            int color = colors[i] & 0xFF;
            if (color == 30) {
                if (run++ == 0) start = i;
            } else {
                if (run >= 16) {
                    greenStart = start;
                    greenLength = run;
                    break;
                }
                run = 0;
            }
        }
        if (run >= 16 && greenStart == -1) { greenStart = start; greenLength = run; }

        if (greenStart == -1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(64, colors.length); i++) sb.append(colors[i] & 0xFF).append(",");
            Blackaddons.LOGGER.warn("[DungeonMap] initializeSizes failed: no green run found. Start colors: " + sb.toString());
            return false;
        }

        if (greenLength != 16 && greenLength != 18) {
            Blackaddons.LOGGER.warn("[DungeonMap] initializeSizes failed: invalid greenLength " + greenLength);
            return false;
        }

        int gl = greenLength;
        Vec2i sc, mc2, ms;
        switch (floorNumber) {
            case 0:
                sc = new Vec2i(22, 22); mc2 = new Vec2i(-137, -137); ms = new Vec2i(4, 4); break;
            case 1:
                sc = new Vec2i(22, 11); mc2 = new Vec2i(-137, -121); ms = new Vec2i(4, 5); break;
            case 2: case 3:
                sc = new Vec2i(11, 11); mc2 = new Vec2i(-121, -121); ms = new Vec2i(5, 5); break;
            default:
                Vec2i derivedStart = new Vec2i((greenStart & 127) % (gl + 4), (greenStart >> 7) % (gl + 4));
                Vec2i extra = new Vec2i(derivedStart.x == 5 ? 1 : 0, derivedStart.z == 5 ? 1 : 0);
                sc  = derivedStart;
                mc2 = new Vec2i(-121, -121).add(extra.x * 16, extra.z * 16);
                ms  = new Vec2i(5, 5).add(extra.x, extra.z);
                break;
        }

        roomSize = gl; startCoords = sc; mapCenter = mc2; mapSize = ms;

        if ((floorNumber == 5 || floorNumber == 6) && ms.x == 6 && ms.z == 6
                || floorNumber == 4 && ms.x == 6 && ms.z == 5) {
            specialColumn = 5;
        }

        Blackaddons.LOGGER.info("[DungeonMap] Floor " + floorNumber + " sc=" + sc + " ms=" + ms + " rs=" + gl);
        return true;
    }

    private static class Unique {
        final int id;
        final Vec2i coords;
        final List<Vec2i> tiles = new ArrayList<>();
        Unique(int id, Vec2i coords) { this.id = id; this.coords = coords; }
    }

    private static void updateRoomTiles(byte[] colors) {
        if (startCoords == null || roomSize == null || mapSize == null) return;

        int rs = roomSize;
        Vec2i sc = startCoords;
        int mw = mapSize.x, mh = mapSize.z;

        int[][] grid = new int[mw][mh];
        int nextId = 0;
        for (int ix = 0; ix < mw; ix++)
            for (int iz = 0; iz < mh; iz++) {
                int idx = sc.add(ix * (rs + 4), iz * (rs + 4)).mapIndex();
                grid[ix][iz] = (idx < colors.length && colors[idx] != 0) ? nextId++ : -1;
            }

        boolean changed;
        do {
            changed = false;
            for (int a = 0; a < 5; a++) {
                for (int b = 0; b < 6; b++) {
                    if (a + 1 < mw && b < mh) {
                        int hi = sc.add(rs + 1 + a * (rs + 4), b * (rs + 4) + 1).mapIndex();
                        if (hi < colors.length && grid[a][b] != -1 && grid[a+1][b] != -1
                                && grid[a+1][b] != grid[a][b] && colors[hi] != 0) {
                            grid[a+1][b] = grid[a][b]; changed = true;
                        }
                    }
                    if (b < mw && a + 1 < mh) {
                        int vi = sc.add(b * (rs + 4) + 1, rs + 1 + a * (rs + 4)).mapIndex();
                        if (vi < colors.length && grid[b][a] != -1 && grid[b][a+1] != -1
                                && grid[b][a+1] != grid[b][a] && colors[vi] != 0) {
                            grid[b][a+1] = grid[b][a]; changed = true;
                        }
                    }
                }
            }
        } while (changed);

        Map<Integer, Unique> uniqueMap = new LinkedHashMap<>();
        for (int ix = 0; ix < mw; ix++) {
            final int fIx = ix;
            for (int iz = 0; iz < mh; iz++) {
                final int fIz = iz;
                int id = grid[ix][iz];
                if (id == -1) continue;
                Unique u = uniqueMap.computeIfAbsent(id, k -> new Unique(k, new Vec2i(fIx, fIz)));
                u.tiles.add(new Vec2i(ix, iz));
            }
        }

        for (Unique unique : uniqueMap.values()) {
            int ci = unique.coords.multiply(rs + 4).add(sc).mapIndex();
            if (ci >= colors.length) continue;
            int color = colors[ci] & 0xFF;

            Room.Type type;
            switch (color) {
                case 18: type = Room.Type.BLOOD;    break;
                case 30: type = Room.Type.ENTRANCE; break;
                case 62: type = Room.Type.TRAP;     break;
                case 63: type = Room.Type.NORMAL;   break;
                case 66: type = Room.Type.PUZZLE;   break;
                case 74: type = Room.Type.CHAMPION; break;
                case 82: type = Room.Type.FAIRY;    break;
                case 85: type = Room.Type.UNKNOWN;  break;
                default: continue;
            }

            Room.Shape shape = inferShape(type, unique.tiles);
            Room existing = null;
            for (Vec2i tp : unique.tiles) {
                int idx = tp.x * 6 + tp.z;
                if (idx >= 0 && idx < tileGrid.length && tileGrid[idx] != null) {
                    existing = tileGrid[idx].owner; break;
                }
            }

            if (existing != null) {
                if (type != Room.Type.UNKNOWN && existing.type == Room.Type.UNKNOWN) existing.type = type;
                if (shape != Room.Shape.UNKNOWN && (existing.shape == Room.Shape.UNKNOWN || existing.shape == Room.Shape.S1x1)) existing.shape = shape;
                for (Vec2i tp : unique.tiles) {
                    int idx = tp.x * 6 + tp.z;
                    if (tileGrid[idx] != null && tileGrid[idx].owner != null && tileGrid[idx].owner != existing) {
                        Room other = tileGrid[idx].owner;
                        other.tiles.removeIf(t -> (t.pos.x + 185) / 32 == tp.x && (t.pos.z + 185) / 32 == tp.z);
                        if (other.tiles.isEmpty()) rooms.remove(other);
                    }
                    if (!existing.places.contains(tp)) existing.roomTile(tp.multiply(32).add(-185, -185), rs);
                }
            } else {
                Room room = new Room(type, shape);
                rooms.add(room);
                for (Vec2i tp : unique.tiles) room.roomTile(tp.multiply(32).add(-185, -185), rs);
            }
        }
    }

    private static Room.Shape inferShape(Room.Type type, List<Vec2i> tiles) {
        if (type == Room.Type.UNKNOWN) return Room.Shape.UNKNOWN;
        int n = tiles.size();
        if (n == 1) return Room.Shape.S1x1;
        if (n == 2) return Room.Shape.S2x1;
        if (n == 3) {
            Vec2i t0 = tiles.get(0), t1 = tiles.get(1), t2 = tiles.get(2);
            return (!( t0.x == t1.x && t0.x == t2.x) && !(t0.z == t1.z && t0.z == t2.z))
                    ? Room.Shape.SL : Room.Shape.S3x1;
        }
        if (n == 4) {
            Vec2i u0 = tiles.get(0), u1 = tiles.get(1), u2 = tiles.get(2);
            return (!(u0.x == u1.x && u0.x == u2.x) && !(u0.z == u1.z && u0.z == u2.z))
                    ? Room.Shape.S2x2 : Room.Shape.S4x1;
        }
        return Room.Shape.UNKNOWN;
    }

    private static List<Room.StateUpdated> updateRoomState(byte[] colors) {
        if (startCoords == null || roomSize == null) return Collections.emptyList();

        List<Room.StateUpdated> updated = new ArrayList<>();
        int rs = roomSize;
        Vec2i sc = startCoords.add(rs / 2, rs / 2);
        int tile = rs + 4;

        for (Room room : rooms) {
            Vec2i placement = minByScore(room.places);
            if (placement == null) continue;

            int ci = sc.add(placement.multiply(tile)).mapIndex();
            int color = (ci < colors.length) ? (colors[ci] & 0xFF) : 0;

            if (color == 0) {
                for (Vec2i p : room.places) {
                    int ci2 = sc.add(p.multiply(tile)).mapIndex();
                    if (ci2 < colors.length && colors[ci2] != 0) { placement = p; color = colors[ci2] & 0xFF; break; }
                }
            }

            Room.StateUpdated su = room.updateState(placement, color);
            if (su != null) {
                updated.add(su);
                if (su.newState == Room.State.GREEN && room.data != null && room.foundSecrets < 0) {
                    room.foundSecrets = room.data.secrets;
                }
            }
        }
        return updated;
    }

    private static Vec2i minByScore(List<Vec2i> list) {
        Vec2i min = null; int minScore = Integer.MAX_VALUE;
        for (Vec2i v : list) {
            int score = v.x * 1000 + v.z;
            if (score < minScore) { minScore = score; min = v; }
        }
        return min;
    }

    private static void scanDoors(byte[] colors) {
        if (startCoords == null || roomSize == null) return;

        int rs = roomSize, hrs = rs / 2;
        Vec2i sc = startCoords.add(hrs, hrs);
        final int TL = -185;

        for (int a = 0; a < 5; a++) {
            for (int b = 0; b < 6; b++) {
                int hIdx   = sc.add(rs + 1 + a * (rs + 4), hrs + b * (rs + 4)).mapIndex();
                int hGuard = sc.add(rs + a * (rs + 4),     hrs + b * (rs + 4)).mapIndex();
                if (hIdx < colors.length && hGuard < colors.length && colors[hGuard] == 0) {
                    int c = colors[hIdx] & 0xFF;
                    if (c != 0) handleDoor(new Vec2i(TL + 16 + a * 32, TL + 16 + b * 32), new Vec2i(a, b), new Vec2i(1, 0), c);
                }

                int vIdx   = sc.add(hrs + b * (rs + 4), rs + 1 + a * (rs + 4)).mapIndex();
                int vGuard = sc.add(hrs + b * (rs + 4), rs + a * (rs + 4)).mapIndex();
                if (vIdx < colors.length && vGuard < colors.length && colors[vGuard] == 0) {
                    int c = colors[vIdx] & 0xFF;
                    if (c != 0) handleDoor(new Vec2i(TL + 16 + b * 32, TL + 16 + a * 32), new Vec2i(b, a), new Vec2i(0, 1), c);
                }
            }
        }
    }

    private static void handleDoor(Vec2i pos, Vec2i place, Vec2i offset, int color) {
        Door.Type type;
        switch (color) {
            case 18: type = Door.Type.BLOOD; break;
            case 62: case 63: case 66: case 74: case 85: type = Door.Type.NORMAL; break;
            case 82: case 119: type = Door.Type.WITHER; break;
            default: return;
        }

        List<Room> adj = new ArrayList<>();
        int i1 = place.x * 6 + place.z;
        int i2 = (place.x + offset.x) * 6 + (place.z + offset.z);
        if (i1 >= 0 && i1 < tileGrid.length && tileGrid[i1] != null && tileGrid[i1].owner != null) adj.add(tileGrid[i1].owner);
        if (i2 >= 0 && i2 < tileGrid.length && tileGrid[i2] != null && tileGrid[i2].owner != null) adj.add(tileGrid[i2].owner);

        Door door = null;
        for (Door d : doors) { if (d.pos.equals(pos)) { door = d; break; } }
        if (door == null) {
            door = new Door(pos, type, adj);
            doors.add(door);
            for (Room r : adj) r.doors.add(door);
        }

        if (type == Door.Type.WITHER && door.type != Door.Type.WITHER) { door.type = Door.Type.WITHER; door.locked = true; }
        if (type == Door.Type.WITHER || type == Door.Type.BLOOD) for (Room r : door.rooms) r.rushRoom = true;
        if (type == Door.Type.NORMAL || color == 82) door.locked = false;
        if (color == 18) door.locked = true;
    }

    private static void propagateBloodRoomType() {
        Door bloodDoor = null;
        for (Door d : doors) { if (d.type == Door.Type.BLOOD) { bloodDoor = d; break; } }
        if (bloodDoor == null) return;

        for (Room r : bloodDoor.rooms) { if (r.type == Room.Type.BLOOD) return; }

        for (Room r : bloodDoor.rooms) {
            if (r.doors.size() == 1 && !r.rushRoom && r.shape == Room.Shape.S1x1) { r.type = Room.Type.BLOOD; break; }
        }
    }

    public static void addWorldDoor(Vec2i pos, Door.Type type, Room roomA, Room roomB) {
        for (Door d : doors) { if (d.pos.equals(pos)) return; }
        List<Room> adj = new ArrayList<>();
        if (roomA != null) adj.add(roomA);
        if (roomB != null) adj.add(roomB);
        Door door = new Door(pos, type, adj);
        door.worldScanned = true;
        doors.add(door);
        for (Room r : adj) r.doors.add(door);
    }

    public static void setBloodRoom(Room room) { bloodRoom = room; }
    public static int getSpecialColumn() { return specialColumn; }

    public static void registerTile(Room.Tile tile) {
        int idx = tile.listIndex();
        if (idx >= 0 && idx < tileGrid.length) tileGrid[idx] = tile;
    }

    public static void reset() {
        mapId = null; startCoords = null; mapCenter = null; mapSize = null; roomSize = null; bloodRoom = null;
        localRoom = null;
        rooms.clear(); doors.clear();
        Arrays.fill(tileGrid, null);
        Blackaddons.LOGGER.info("[DungeonMap] Reset.");
    }

    public static Set<Room>   getRooms()      { return rooms;       }
    public static List<Door>  getDoors()      { return doors;       }
    public static Room.Tile[] getTileGrid()   { return tileGrid;    }
    public static Room        getBloodRoom()  { return bloodRoom;   }
    public static Vec2i       getMapCenter()  { return mapCenter;   }
    public static Vec2i       getMapSize()    { return mapSize;     }
    public static Integer     getRoomSize()   { return roomSize;    }
    public static Vec2i       getStartCoords(){ return startCoords; }
    public static Room        getLocalRoom()  { return localRoom;   }

    public static void updateLocalRoom(Room room) {
        if (room == null || room == localRoom) return;
        localRoom = room;

        if (room.state == Room.State.UNDISCOVERED || room.state == Room.State.UNOPENED) {
            room.state = Room.State.DISCOVERED;
        }

        for (Door door : room.doors) {
            for (Room r : door.rooms) {
                if (r != room && r.state == Room.State.UNDISCOVERED) {
                    r.state = Room.State.UNOPENED;
                }
            }
        }
    }

    private static int getFloorNumber(DungeonFloor floor) {
        if (floor == null || floor == DungeonFloor.ENTRANCE) return 0;
        try { return Integer.parseInt(floor.getDisplayName().substring(1)); }
        catch (NumberFormatException e) { return -1; }
    }
    private static Integer findDungeonMapId() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() instanceof MapItem) {
                if (stack.has(DataComponents.CUSTOM_NAME) && stack.getHoverName().getString().contains("Dungeon Map")) {
                    MapId mid = stack.get(DataComponents.MAP_ID);
                    if (mid != null) return mid.id();
                }
            }
        }
        return null;
    }
}
