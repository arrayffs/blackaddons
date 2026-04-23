package org.blackum.blackaddons.gui.render.font;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public class GoogleFontsList {

    private static final String FONT_CONTENTS_BASE = "https://api.github.com/repos/google/fonts/contents/ofl";

    private static final Path CACHE_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("blackaddons").resolve("data").resolve("fontnames.txt");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final List<String> nameList     = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, String> urlByDir     = new ConcurrentHashMap<>();
    private static final Map<String, String> dirByDisplay = new ConcurrentHashMap<>();
    private static final Map<String, String> shaByDir     = new ConcurrentHashMap<>();

    private static volatile boolean lazyStarted = false;
    private static volatile boolean lazyDone    = false;
    private static final List<Runnable> pendingCallbacks =
            Collections.synchronizedList(new ArrayList<>());

    public static List<String> get() { return nameList; }

    public static boolean isLazyDone() { return lazyDone; }

    public static String getUrl(String displayName) {
        String dir = dirByDisplay.get(displayName.toLowerCase(Locale.ROOT));
        return dir != null ? urlByDir.get(dir) : null;
    }

    public static String getDirName(String displayName) {
        String dir = dirByDisplay.get(displayName.toLowerCase(Locale.ROOT));
        return dir != null ? dir : displayName.toLowerCase(Locale.ROOT).replace(" ", "");
    }

    public static String getSha(String displayName) {
        String dir = dirByDisplay.get(displayName.toLowerCase(Locale.ROOT));
        return dir != null ? shaByDir.get(dir) : null;
    }

    public static void cacheUrl(String displayName, String url) {
        urlByDir.put(getDirName(displayName), url);
    }

    public static void startLazyLoad(Runnable onReady) {
        if (lazyDone) {
            if (onReady != null) Minecraft.getInstance().execute(onReady);
            return;
        }
        if (onReady != null) pendingCallbacks.add(onReady);
        if (lazyStarted) return;
        lazyStarted = true;

        Thread t = new Thread(() -> {
            try {
                List<String[]> entries;
                if (Files.exists(CACHE_FILE)) {
                    entries = loadFromCache();
                } else {
                    entries = fetchDirNames();
                    saveToCache(entries);
                }

                if (entries.isEmpty()) {
                }

                Set<String> existing = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                synchronized (nameList) { existing.addAll(nameList); }

                List<String> newNames = new ArrayList<>();
                for (String[] e : entries) {
                    dirByDisplay.put(e[0].toLowerCase(Locale.ROOT), e[1]);
                    shaByDir.put(e[1], e[2]);
                    if (existing.add(e[0])) newNames.add(e[0]);
                }
                newNames.sort(String.CASE_INSENSITIVE_ORDER);
                synchronized (nameList) { nameList.addAll(newNames); }

                lazyDone = true;
            } catch (Exception e) {
            } finally {
                lazyStarted = false;
                if (lazyDone) lazyStarted = true;
                List<Runnable> cbs = new ArrayList<>(pendingCallbacks);
                pendingCallbacks.clear();
                Minecraft.getInstance().execute(() -> cbs.forEach(Runnable::run));
            }
        }, "CustomFont-FontList");
        t.setDaemon(true);
        t.start();
    }

    public static void invalidateCache() {
        lazyDone    = false;
        lazyStarted = false;
        synchronized (nameList) { nameList.clear(); }
        urlByDir.clear();
        dirByDisplay.clear();
        shaByDir.clear();
        try { Files.deleteIfExists(CACHE_FILE); } catch (Exception ignored) {}
    }

    private static List<String[]> fetchDirNames() throws Exception {
        String json = httpGet(FONT_CONTENTS_BASE + "?ref=main");
        List<String[]> result = parseDirEntries(json);
        return result;
    }

    static List<String[]> parseDirEntries(String json) {
        List<String[]> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int i = 0, len = json.length();

        while (i < len) {
            int start = json.indexOf('{', i);
            if (start < 0) break;

            int depth = 0, end = -1;
            boolean inStr = false;
            for (int j = start; j < len; j++) {
                char c = json.charAt(j);
                if (c == '"' && (j == 0 || json.charAt(j - 1) != '\\')) inStr = !inStr;
                if (!inStr) {
                    if (c == '{') depth++;
                    else if (c == '}') { depth--; if (depth == 0) { end = j; break; } }
                }
            }
            if (end < 0) break;

            String obj = json.substring(start, end + 1);
            i = end + 1;

            String type = extractField(obj, "type");
            if (!"dir".equals(type)) continue;

            String name = extractField(obj, "name");
            String sha  = extractField(obj, "sha");

            if (name == null || sha == null) {
                continue;
            }

            if (!seen.add(name)) continue;
            result.add(new String[]{dirToDisplayName(name), name, sha});
        }

        result.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a[0], b[0]));
        return result;
    }

    private static String extractField(String obj, String key) {
        String search = "\"" + key + "\"";
        int idx = obj.indexOf(search);
        if (idx < 0) return null;
        idx += search.length();
        while (idx < obj.length() && (obj.charAt(idx) == ' ' || obj.charAt(idx) == '\t' || obj.charAt(idx) == ':')) idx++;
        if (idx >= obj.length() || obj.charAt(idx) != '"') return null;
        idx++;
        int end = obj.indexOf('"', idx);
        return end < 0 ? null : obj.substring(idx, end);
    }

    static String dirToDisplayName(String dir) {
        if (dir.isEmpty()) return dir;
        return Character.toUpperCase(dir.charAt(0)) + dir.substring(1);
    }

    private static List<String[]> loadFromCache() throws Exception {
        List<String[]> result = new ArrayList<>();
        int skipped = 0;
        for (String line : Files.readAllLines(CACHE_FILE)) {
            String[] parts = line.split("\t", 2);
            if (parts.length == 2 && !parts[0].isBlank() && parts[1].matches("[0-9a-f]{40}")) {
                result.add(new String[]{dirToDisplayName(parts[0]), parts[0], parts[1]});
            } else {
                skipped++;
            }
        }
        if (result.isEmpty()) {
            Files.deleteIfExists(CACHE_FILE);
            result = fetchDirNames();
            saveToCache(result);
        }
        return result;
    }

    private static void saveToCache(List<String[]> entries) throws Exception {
        Files.createDirectories(CACHE_FILE.getParent());
        List<String> lines = new ArrayList<>(entries.size());
        for (String[] e : entries) lines.add(e[1] + "\t" + e[2]);
        Files.write(CACHE_FILE, lines);
    }

    static String httpGet(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "blackaddons-minecraft-mod")
                .GET().timeout(Duration.ofSeconds(30)).build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new RuntimeException("HTTP " + resp.statusCode() + ": " + url);
        return resp.body();
    }
}
