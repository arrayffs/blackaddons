package org.blackum.blackaddons.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.constants.Constants;
import org.blackum.blackaddons.common.util.io.EncryptionUtils;
import org.blackum.blackaddons.common.util.mc.MinecraftInstance;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
public class BotIntegration {
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
            .build();

    public static void sendRngDrop(String player, String item, String rarity, String floor) {
        if (ConfigManager.data.botUrl.isEmpty())
            return;

        JsonObject json = new JsonObject();
        json.addProperty("player", player);
        json.addProperty("item", item);
        json.addProperty("rarity", rarity);
        json.addProperty("floor", floor);
        json.addProperty("timestamp", System.currentTimeMillis() / 1000);

        sendPostRequest(Constants.BOT_API_RNG, json.toString());
    }

    public static CompletableFuture<Boolean> sendDailySync(String player) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(false);

        JsonObject json = new JsonObject();
        json.addProperty("player", player);

        return sendPostRequest(Constants.BOT_API_DAILY, json.toString()).thenApply(res -> {
            return res != null && res.statusCode() >= 200 && res.statusCode() < 300;
        });
    }

    public static CompletableFuture<JsonObject> getProfileStats(String player, String profileName, boolean force) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        String url = Constants.BOT_API_PROFILE + "?player=" + player;
        if (profileName != null) {
            url += "&profile=" + profileName;
        }
        if (force) {
            url += "&force=true";
        }

        return sendGetRequest(url).thenApply(res -> {
            if (res == null)
                return null;
            if (res.statusCode() == 200 || res.statusCode() == 400 || res.statusCode() == 404) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse profile stats response: " + e.getMessage());
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> getRtcaStats(String player, String profileName, String floor,
            Map<String, Double> bonuses) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        JsonObject json = new JsonObject();
        json.addProperty("player", player);
        if (profileName != null) {
            json.addProperty("profile", profileName);
        }
        json.addProperty("floor", floor);

        if (bonuses != null && !bonuses.isEmpty()) {
            JsonObject bonusJson = new JsonObject();
            for (Map.Entry<String, Double> entry : bonuses.entrySet()) {
                bonusJson.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("bonuses", bonusJson);
        }

        return sendPostRequest(Constants.BOT_API_RTCA, json.toString()).thenApply(res -> {
            if (res == null)
                return null;
            if (res.statusCode() == 200 || res.statusCode() == 400 || res.statusCode() == 404) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse RTCA stats response: " + e.getMessage());
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> getRngData(String player) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        return sendGetRequest(Constants.BOT_API_RNG + "?player=" + player).thenApply(res -> {
            if (res == null)
                return null;
            if (res.statusCode() == 200 || res.statusCode() == 400 || res.statusCode() == 404) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse RNG data response: " + e.getMessage());
                }
            }
            return null;
        });
    }

    public static CompletableFuture<Integer> updateRngDrop(String player, String category, String item, String action,
            Integer count) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        JsonObject json = new JsonObject();
        json.addProperty("player", player);
        json.addProperty("action", action);
        json.addProperty("category", category);
        json.addProperty("item", item);
        if (count != null) {
            json.addProperty("count", count);
        }

        return sendPostRequest(Constants.BOT_API_RNG, json.toString()).thenApply(res -> {
            if (res != null && res.statusCode() >= 200 && res.statusCode() < 300) {
                try {
                    JsonObject responseJson = JsonParser.parseString(res.body()).getAsJsonObject();
                    if (responseJson.has("count")) {
                        return responseJson.get("count").getAsInt();
                    }
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse RNG update response: " + e.getMessage());
                }
                return -1;
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> getLeaderboard(String period, String metric, int page) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        String endpoint = String.format(Constants.BOT_API_LEADERBOARD + "?period=%s&metric=%s&page=%d", period, metric,
                page);
        return sendGetRequest(endpoint).thenApply(res -> {
            if (res == null)
                return null;
            if (res.statusCode() == 200 || res.statusCode() == 400 || res.statusCode() == 404) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse leaderboard stats response: " + e.getMessage());
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> getLeaderboardWithPlayer(String period, String metric, String player) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        String endpoint = String.format(Constants.BOT_API_LEADERBOARD + "?period=%s&metric=%s&find_player=%s", period,
                metric, player);
        return sendGetRequest(endpoint).thenApply(res -> {
            if (res != null && (res.statusCode() == 200 || res.statusCode() == 404)) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> createParty(String floor, String note, JsonObject reqs, int maxSize) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        JsonObject json = new JsonObject();
        json.addProperty("player", MinecraftInstance.mc.getUser().getName());
        json.addProperty("floor", floor);
        json.addProperty("note", note);
        json.add("reqs", reqs);
        json.addProperty("max_size", maxSize);

        return sendPostRequest(Constants.BOT_API_PARTY_CREATE, json.toString()).thenApply(res -> {
            if (res != null && (res.statusCode() == 200 || res.statusCode() == 400)) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });
    }

    public static CompletableFuture<Boolean> unqueueParty() {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(false);

        JsonObject json = new JsonObject();
        json.addProperty("player", MinecraftInstance.mc.getUser().getName());

        return sendPostRequest(Constants.BOT_API_PARTY_UNQUEUE, json.toString()).thenApply(res -> {
            return res != null && res.statusCode() == 200;
        });
    }

    public static CompletableFuture<Boolean> updateParty(int memberCount, String note) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(false);

        JsonObject json = new JsonObject();
        json.addProperty("player", MinecraftInstance.mc.getUser().getName());
        json.addProperty("member_count", memberCount);
        if (note != null) {
            json.addProperty("note", note);
        }

        return sendPostRequest(Constants.BOT_API_PARTY_UPDATE, json.toString()).thenApply(res -> {
            return res != null && res.statusCode() == 200;
        });
    }

    public static CompletableFuture<JsonObject> getParties(String floor) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        String url = Constants.BOT_API_PARTY_LIST + (floor != null ? "?floor=" + floor : "");
        return sendGetRequest(url).thenApply(res -> {
            if (res != null && res.statusCode() == 200) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> getSoloLeaderboard(String floor) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        String url = Constants.BOT_API_SOLO_LEADERBOARD + "?floor=" + floor;
        return sendGetRequest(url).thenApply(res -> {
            if (res != null && res.statusCode() == 200) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse leaderboard response: " + e.getMessage());
                }
            }
            return null;
        });
    }

    public static CompletableFuture<JsonObject> sendSoloClear(String player, String floor, String time,
            int secrets, List<String> puzzles, boolean prince, boolean mimic, boolean needsVerification) {
        if (ConfigManager.data.botUrl.isEmpty())
            return CompletableFuture.completedFuture(null);

        JsonObject json = new JsonObject();
        json.addProperty("player", player);
        json.addProperty("floor", floor);
        json.addProperty("time", time);
        json.addProperty("secrets", secrets);
        json.addProperty("prince", prince);
        json.addProperty("mimic", mimic);
        json.addProperty("needs_verification", needsVerification);

        JsonArray puzzleArray = new JsonArray();
        if (puzzles != null) {
            for (String p : puzzles) {
                puzzleArray.add(p);
            }
        }
        json.add("puzzles", puzzleArray);

        return sendPostRequest(Constants.BOT_API_SOLO_CLEAR, json.toString()).thenApply(res -> {
            if (res != null && res.statusCode() >= 200 && res.statusCode() < 300) {
                try {
                    return JsonParser.parseString(res.body()).getAsJsonObject();
                } catch (Exception e) {
                    Blackaddons.LOGGER.error("Failed to parse solo clear response: " + e.getMessage());
                }
            }
            return null;
        });
    }

    private static boolean authKeyFetched = false;

    public static CompletableFuture<String> getAuthKey() {
        if (authKeyFetched && EncryptionUtils.isKeySet()) {
            return CompletableFuture.completedFuture("fetched_key_present");
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ConfigManager.data.botUrl + "/v1/key"))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", Constants.BOT_USER_AGENT)
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (responseJson.has("key")) {
                        String key = responseJson.get("key").getAsString();
                        EncryptionUtils.setKeyBase64(key);
                        authKeyFetched = true;
                        return "fetched_key_present";
                    }
                } else {
                    Blackaddons.LOGGER.warn("Bot /v1/key failed. Status: " + response.statusCode() + " Body: " + response.body());
                }
            } catch (Exception e) {
                Blackaddons.LOGGER.error("Failed to fetch bot key: " + e.getMessage());
            }
            return null;
        });
    }

    public static CompletableFuture<Void> authenticateWithBot() {
        return getAuthKey().thenAccept(key -> {
            if (key != null) {
                Blackaddons.LOGGER.info("Successfully fetched developer authentication key.");
            } else {
                Blackaddons.LOGGER.warn("Failed to get developer authentication key.");
            }
        });
    }

    private static CompletableFuture<HttpResponse<String>> sendPostRequest(String endpoint, String jsonBody) {
        return sendRequest("POST", endpoint, jsonBody, true);
    }

    private static CompletableFuture<HttpResponse<String>> sendGetRequest(String endpoint) {
        return sendRequest("GET", endpoint, null, true);
    }

    private static CompletableFuture<HttpResponse<String>> sendRequest(String method, String endpoint, String jsonBody,
            boolean allowRetry) {
        return getAuthKey().thenCompose(key -> {
            String url = ConfigManager.data.botUrl + endpoint;

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", Constants.BOT_USER_AGENT);

            if (ConfigManager.data.developerKey != null && !ConfigManager.data.developerKey.isEmpty()) {
                builder.header(Constants.HEADER_DEVELOPER_KEY, ConfigManager.data.developerKey);
            } else {
                if (EncryptionUtils.isKeySet()) {
                    try {
                        if (MinecraftInstance.mc.getUser() != null) {
                            String uuidStr = MinecraftInstance.mc.getUser().getProfileId().toString();
                            String encryptedId = EncryptionUtils.encryptIdentity(uuidStr);
                            if (encryptedId != null) {
                                builder.header("X-Encrypted-Identity", encryptedId);
                            }
                            builder.header("X-Player-Name", MinecraftInstance.mc.getUser().getName());
                            builder.header("X-Player-UUID", MinecraftInstance.mc.getUser().getProfileId().toString());
                        }
                    } catch (Exception ignored) {}
                }
            }

            if (method.equalsIgnoreCase("POST") && jsonBody != null) {
                builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            } else {
                builder.GET();
            }

            return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenCompose(res -> {
                        if (res.statusCode() == 403 && allowRetry) {
                            Blackaddons.LOGGER.info("Authentication failed. Regenerating key and retrying...");
                            authKeyFetched = false; // force re-fetch
                            return sendRequest(method, endpoint, jsonBody, false);
                        }

                        if (res.statusCode() >= 200 && res.statusCode() < 300) {
                            Blackaddons.LOGGER.info("Successfully communicated with bot (" + method + "): " + endpoint);
                        } else {
                            Blackaddons.LOGGER.warn("Bot communication failed. Status: " + res.statusCode() + " Body: " + res.body());
                        }
                        return CompletableFuture.completedFuture(res);
                    })
                    .exceptionally(e -> {
                        Blackaddons.LOGGER.error("Error communicating with bot: " + e.getMessage());
                        return null;
                    });
        });
    }

}
