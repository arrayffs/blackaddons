package org.blackum.blackaddons.feature.profile;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.constants.Constants;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

public class PlayerProfileManager {

    private static final String PLAYER_PROFILES_FILE = "player_profiles.json";
    private static final String PLAYER_DB_ID_PATH = "data.player.id";
    private static final String PLAYER_DB_NAME_PATH = "data.player.username";

    private static PlayerProfileManager instance;

    private final Map<String, String> uuidToNameCache = new ConcurrentHashMap<>();
    private final HttpClient httpClient;
    private final ExecutorService saveExecutor = Executors.newSingleThreadExecutor();
    private File cacheFile;

    private PlayerProfileManager() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
                .build();
        this.cacheFile = FabricLoader.getInstance().getConfigDir()
                .resolve(Constants.CONFIG_DIR_NAME)
                .resolve(Constants.DATA_DIR_NAME)
                .resolve(PLAYER_PROFILES_FILE)
                .toFile();
        loadCache();
    }

    public static PlayerProfileManager getInstance() {
        if (instance == null) {
            instance = new PlayerProfileManager();
        }
        return instance;
    }

    public CompletableFuture<String> getName(UUID uuid) {
        String key = uuid.toString().replace("-", "").toLowerCase();

        PlayerInfo onlineInfo = Minecraft.getInstance().getConnection() != null
                ? Minecraft.getInstance().getConnection().getPlayerInfo(uuid)
                : null;
        if (onlineInfo != null && onlineInfo.getProfile().name() != null) {
            String name = onlineInfo.getProfile().name();
            putAndSave(key, name);
            return CompletableFuture.completedFuture(name);
        }

        String cached = uuidToNameCache.get(key);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return fetchFromPlayerDb(uuid).thenApply(name -> {
            if (name != null) {
                putAndSave(key, name);
            }
            return name;
        });
    }

    private CompletableFuture<String> fetchFromPlayerDb(UUID uuid) {
        String url = Constants.PLAYER_DB_API + uuid.toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", Constants.BOT_USER_AGENT)
                .timeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200 || response.body() == null) {
                        return null;
                    }
                    try {
                        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                        JsonObject data = root.getAsJsonObject("data");
                        if (data == null) return null;
                        JsonObject player = data.getAsJsonObject("player");
                        if (player == null || !player.has("username")) return null;
                        return player.get("username").getAsString();
                    } catch (Exception e) {
                        Blackaddons.LOGGER.warn("Failed to parse PlayerDB response for " + uuid, e);
                        return null;
                    }
                })
                .exceptionally(e -> {
                    Blackaddons.LOGGER.warn("PlayerDB lookup failed for " + uuid, e);
                    return null;
                });
    }

    private void putAndSave(String key, String name) {
        uuidToNameCache.put(key, name);
        saveCache();
    }

    private void loadCache() {
        if (!cacheFile.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(cacheFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : json.entrySet()) {
                uuidToNameCache.put(entry.getKey(), entry.getValue().getAsString());
            }
            Blackaddons.LOGGER.info("Loaded " + uuidToNameCache.size() + " player profiles from cache.");
        } catch (Exception e) {
            Blackaddons.LOGGER.warn("Failed to load player profiles cache.", e);
        }
    }

    private void saveCache() {
        saveExecutor.submit(() -> {
            try {
                File parent = cacheFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                JsonObject json = new JsonObject();
                uuidToNameCache.forEach(json::addProperty);
                try (FileWriter writer = new FileWriter(cacheFile)) {
                    Constants.GSON.toJson(json, writer);
                }
            } catch (IOException e) {
                Blackaddons.LOGGER.warn("Failed to save player profiles cache.", e);
            }
        });
    }
}
