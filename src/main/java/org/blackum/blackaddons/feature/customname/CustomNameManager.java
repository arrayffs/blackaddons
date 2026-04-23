package org.blackum.blackaddons.feature.customname;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.constants.Constants;
import org.blackum.blackaddons.feature.chat.ChatUtils;
import org.blackum.blackaddons.feature.profile.PlayerProfileManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;

public class CustomNameManager {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
            Pattern.CASE_INSENSITIVE
    );

    private static CustomNameManager instance;
    private final Map<String, CustomName> customNames = new ConcurrentHashMap<>();
    private final HttpClient httpClient;

    private CustomNameManager() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
                .build();
    }

    public static CustomNameManager getInstance() {
        if (instance == null) {
            instance = new CustomNameManager();
        }
        return instance;
    }

    public void fetch() {
        HttpRequest githubRequest = HttpRequest.newBuilder()
                .uri(URI.create(Constants.GITHUB_NAMES_URL))
                .header("User-Agent", Constants.BOT_USER_AGENT)
                .timeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
                .GET()
                .build();

        httpClient.sendAsync(githubRequest, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 && response.body() != null) {
                        try {
                            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                            parseAndApplyNames(json);
                            return;
                        } catch (Exception e) {
                            Blackaddons.LOGGER.warn("Failed to parse GitHub custom names, falling back to bot.", e);
                        }
                    } else {
                        Blackaddons.LOGGER.warn("GitHub custom names returned status " + response.statusCode()
                                + ", falling back to bot.");
                    }
                    fetchFromBotFallback();
                })
                .exceptionally(ex -> {
                    Blackaddons.LOGGER.warn("Error fetching GitHub custom names, falling back to bot.", ex);
                    fetchFromBotFallback();
                    return null;
                });
    }

    private void fetchFromBotFallback() {
        if (ConfigManager.data == null || ConfigManager.data.botUrl == null || ConfigManager.data.botUrl.isEmpty()) {
            return;
        }

        String url = ConfigManager.data.botUrl + Constants.BOT_API_NAMES;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", Constants.BOT_USER_AGENT)
                .timeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 && response.body() != null) {
                        try {
                            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                            if (json.has("names")) {
                                parseAndApplyNames(json.getAsJsonObject("names"));
                            } else {
                                parseAndApplyNames(json);
                            }
                        } catch (Exception e) {
                            Blackaddons.LOGGER.error("Failed to parse bot custom names.", e);
                        }
                    }
                })
                .exceptionally(ex -> {
                    Blackaddons.LOGGER.error("Error fetching bot custom names.", ex);
                    return null;
                });
    }

    private void parseAndApplyNames(JsonObject namesObj) {
        customNames.clear();
        for (Map.Entry<String, JsonElement> entry : namesObj.entrySet()) {
            String key = entry.getKey();
            JsonObject data = entry.getValue().getAsJsonObject();

            CustomName customName = parseCustomName(data);

            if (data.has("uuid")) {
                resolveUuidAndRegister(data.get("uuid").getAsString(), customName);
            }

            if (UUID_PATTERN.matcher(key).matches()) {
                resolveUuidAndRegister(key, customName);
            } else {
                customNames.put(key.toLowerCase(), customName);
            }
        }
        Blackaddons.LOGGER.info("Loaded " + customNames.size() + " custom names.");
    }

    private void resolveUuidAndRegister(String uuidString, CustomName customName) {
        try {
            UUID uuid = UUID.fromString(uuidString);
            PlayerProfileManager.getInstance().getName(uuid).thenAccept(name -> {
                if (name != null) {
                    customNames.put(name.toLowerCase(), customName);
                    // Blackaddons.LOGGER.info("Resolved UUID " + uuidString + " -> " + name); 
                }
            });
        } catch (IllegalArgumentException e) {
            Blackaddons.LOGGER.warn("Invalid UUID in custom names: " + uuidString);
        }
    }

    private CustomName parseCustomName(JsonObject data) {
        String displayName = data.has("display") ? data.get("display").getAsString() : "";
        String color = data.has("color") ? data.get("color").getAsString() : "";
        boolean animated = data.has("animated") && data.get("animated").getAsBoolean();
        boolean chroma = data.has("chroma") && data.get("chroma").getAsBoolean();
        float speed = data.has("speed") ? data.get("speed").getAsFloat() : 1.0f;

        List<ChatUtils.ColorStop> gradientStops = parseGradient(data);

        return new CustomName(displayName, color, gradientStops, animated, chroma, speed);
    }

    private List<ChatUtils.ColorStop> parseGradient(JsonObject data) {
        if (!data.has("gradient")) return new ArrayList<>();

        JsonElement gradientElement = data.get("gradient");
        List<ChatUtils.ColorStop> stops = new ArrayList<>();

        if (gradientElement.isJsonArray()) {
            JsonArray gradient = gradientElement.getAsJsonArray();
            if (gradient.size() >= 2) {
                try {
                    int start = Integer.parseInt(gradient.get(0).getAsString().replace("#", ""), 16);
                    int end = Integer.parseInt(gradient.get(1).getAsString().replace("#", ""), 16);
                    stops.add(new ChatUtils.ColorStop(start, 0.0f));
                    stops.add(new ChatUtils.ColorStop(end, 1.0f));
                } catch (NumberFormatException ignored) {
                }
            }
        } else if (gradientElement.isJsonPrimitive()) {
            String gradientStr = gradientElement.getAsString();
            if (gradientStr.isEmpty()) return stops;

            boolean isLinear = gradientStr.startsWith("linear-gradient");
            boolean isRadial = gradientStr.startsWith("radial-gradient");

            if (isLinear || isRadial) {
                Matcher m = Pattern.compile(
                        "rgba\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*[^)]*\\)\\s*(\\d+)%")
                        .matcher(gradientStr);
                while (m.find()) {
                    int r = Integer.parseInt(m.group(1));
                    int g = Integer.parseInt(m.group(2));
                    int b = Integer.parseInt(m.group(3));
                    float fraction = Float.parseFloat(m.group(4)) / 100.0f;
                    int rgb = (r << 16) | (g << 8) | b;

                    if (isRadial) {
                        float leftFraction = 0.5f - (fraction / 2.0f);
                        float rightFraction = 0.5f + (fraction / 2.0f);
                        stops.add(new ChatUtils.ColorStop(rgb, leftFraction));
                        if (leftFraction != rightFraction) {
                            stops.add(new ChatUtils.ColorStop(rgb, rightFraction));
                        }
                    } else {
                        stops.add(new ChatUtils.ColorStop(rgb, fraction));
                    }
                }
                stops.sort((a, b2) -> Float.compare(a.fraction(), b2.fraction()));
            }
        }
        return stops;
    }

    public Component replaceNames(Component component) {
        if (customNames.isEmpty()) {
            return component;
        }
        return processComponent(component);
    }

    public String replaceInString(String text) {
        if (customNames.isEmpty() || text == null || text.isEmpty()) return text;
        String lower = text.toLowerCase();
        if (!containsAnyIgn(lower)) return text;

        String result = text;
        for (String ign : customNames.keySet()) {
            if (lower.contains(ign)) {
                CustomName custom = customNames.get(ign);
                result = result.replaceAll("(?i)" + Pattern.quote(ign), custom.display());
            }
        }
        return result;
    }

    public FormattedCharSequence replaceInSequence(FormattedCharSequence sequence) {
        if (customNames.isEmpty() || sequence == null) return sequence;

        StringBuilder sb = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        String plain = sb.toString();
        if (!containsAnyIgn(plain.toLowerCase())) return sequence;

        MutableComponent rebuilt = Component.literal("");
        List<TextNode> nodes = new ArrayList<>();

        sequence.accept((index, style, codePoint) -> {
            if (nodes.isEmpty() || !Objects.equals(nodes.get(nodes.size() - 1).style, style)) {
                nodes.add(new TextNode(new StringBuilder().appendCodePoint(codePoint), style));
            } else {
                nodes.get(nodes.size() - 1).content.appendCodePoint(codePoint);
            }
            return true;
        });

        for (TextNode node : nodes) {
            rebuilt.append(Component.literal(node.content.toString()).withStyle(node.style));
        }

        return net.minecraft.locale.Language.getInstance().getVisualOrder(replaceNames(rebuilt));
    }

    private record TextNode(StringBuilder content, Style style) {}

    private boolean containsAnyIgn(String lowerText) {
        for (String ign : customNames.keySet()) {
            if (lowerText.contains(ign)) return true;
        }
        return false;
    }

    private Component processComponent(Component component) {
        MutableComponent newComponent = Component.empty();
        newComponent.setStyle(component.getStyle());

        if (component.getContents() instanceof TranslatableContents translatable) {
            Object[] args = translatable.getArgs();
            Object[] newArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Component argComponent) {
                    newArgs[i] = processComponent(argComponent);
                } else {
                    newArgs[i] = args[i];
                }
            }
            MutableComponent rebuilt = MutableComponent.create(
                    new TranslatableContents(translatable.getKey(), translatable.getFallback(), newArgs));
            rebuilt.setStyle(component.getStyle());
            for (Component sibling : component.getSiblings()) {
                rebuilt.append(processComponent(sibling));
            }
            return rebuilt;
        } else if (component.getContents() instanceof net.minecraft.network.chat.contents.PlainTextContents literal) {
            String text = literal.text();
            String lowerText = text.toLowerCase();
            boolean found = false;

            for (String ign : customNames.keySet()) {
                if (lowerText.contains(ign)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                newComponent = component.plainCopy();
                newComponent.setStyle(component.getStyle());
            } else {
                int currentPos = 0;
                while (currentPos < text.length()) {
                    String earliestIgn = null;
                    int earliestIdx = -1;

                    for (String ign : customNames.keySet()) {
                        int idx = lowerText.indexOf(ign, currentPos);
                        if (idx != -1 && (earliestIdx == -1 || idx < earliestIdx)) {
                            earliestIdx = idx;
                            earliestIgn = ign;
                        }
                    }

                    if (earliestIgn == null) {
                        newComponent.append(Component.literal(text.substring(currentPos)));
                        break;
                    }

                    if (earliestIdx > currentPos) {
                        newComponent.append(Component.literal(text.substring(currentPos, earliestIdx)));
                    }

                    newComponent.append(applyCustomName(earliestIgn, Component.literal(earliestIgn)));
                    currentPos = earliestIdx + earliestIgn.length();
                }
            }
        } else {
            newComponent = component.plainCopy();
            newComponent.setStyle(component.getStyle());
        }

        for (Component sibling : component.getSiblings()) {
            newComponent.append(processComponent(sibling));
        }

        return newComponent;
    }

    public Component applyCustomName(String username, Component originalComponent) {
        CustomName custom = customNames.get(username.toLowerCase());
        if (custom == null) {
            return originalComponent;
        }

        if (custom.chroma()) {
            return ChatUtils.BuildChroma(custom.display(), custom.speed());
        }

        if (custom.gradientStops() != null && !custom.gradientStops().isEmpty()) {
            try {
                if (custom.animated()) {
                    return ChatUtils.BuildAnimatedMultiGradient(custom.display(), custom.gradientStops(), custom.speed());
                }
                return ChatUtils.BuildMultiGradient(custom.display(), custom.gradientStops());
            } catch (Exception e) {
            }
        }

        if (custom.display().contains("§")) {
            return Component.literal(custom.display());
        }

        if (custom.color() != null && !custom.color().isEmpty()) {
            try {
                TextColor color = TextColor.parseColor(custom.color()).getOrThrow();
                return Component.literal(custom.display()).withStyle(Style.EMPTY.withColor(color));
            } catch (Exception e) {
            }
        }

        return Component.literal(custom.display());
    }

    public CustomName getCustomName(String username) {
        return customNames.get(username.toLowerCase());
    }

    public record CustomName(String display, String color, List<ChatUtils.ColorStop> gradientStops, boolean animated,
            boolean chroma, float speed) {
    }
}
