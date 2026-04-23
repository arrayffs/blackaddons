package org.blackum.blackaddons.gui.screen.main.tabs;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.font.CustomFontRenderer;
import org.blackum.blackaddons.gui.render.font.GoogleFontsList;
import org.blackum.blackaddons.gui.screen.main.BaseScreen;
import org.blackum.blackaddons.gui.screen.main.BlackAddonsGUI;
import org.blackum.blackaddons.gui.screen.overlay.OverlayEditScreen;
import org.blackum.blackaddons.gui.widget.base.Button;
import org.blackum.blackaddons.gui.widget.base.Label;
import org.blackum.blackaddons.gui.widget.input.AutocompleteTextField;
import org.blackum.blackaddons.gui.widget.input.Slider;
import org.blackum.blackaddons.gui.widget.input.SmallColorPicker;
import org.blackum.blackaddons.gui.widget.input.ToggleSwitch;
import org.blackum.blackaddons.gui.widget.layout.CardContainer;
import org.blackum.blackaddons.gui.widget.layout.ListView;
import org.blackum.blackaddons.gui.widget.layout.ResizableCard;
import org.blackum.blackaddons.gui.widget.layout.TabPanel;

public class LegitTabController extends SimpleTabController {
    private ResizableCard visualsCard;
    private ResizableCard debuggersCard;
    private ResizableCard customTextCard;

    public LegitTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    @Override
    public void init(TabPanel.Tab legitTab) {
        int contentX = legitTab.getParent().getContentX();
        int contentY = legitTab.getParent().getContentY();
        int contentWidth = legitTab.getParent().getContentWidth();

        if (ConfigManager.data.useCardLayout) {
            Button resetLayout = new Button(contentX + 10, contentY, contentWidth - 20, "Reset Layout", () -> {
                screen.resetCardStates("legit_visuals", "legit_debuggers", "legit_custom_text");
            });
            legitTab.addWidget(resetLayout);

            CardContainer legitCardContainer = new CardContainer(contentX, contentY + 30, contentWidth, 570);
            legitTab.addWidget(legitCardContainer);

            int containerY = contentY + 30;
            int numCols = contentWidth < 680 ? 1 : (contentWidth < 1000 ? 2 : 3);
            int colWidth = 300;
            int spacing = 20;
            int[] colY = new int[numCols];
            for (int i = 0; i < numCols; i++) colY[i] = containerY + 20;

            List<ResizableCard> cards = new ArrayList<>();
            visualsCard = createVisualsCard(0, 0);
            cards.add(visualsCard);
            debuggersCard = createDebuggersCard(0, 0);
            cards.add(debuggersCard);
            customTextCard = createCustomTextCard(0, 0);
            cards.add(customTextCard);

            for (ResizableCard card : cards) {
                int shortestCol = 0;
                for (int i = 1; i < numCols; i++) {
                    if (colY[i] < colY[shortestCol]) shortestCol = i;
                }

                card.setX(contentX + spacing + shortestCol * (colWidth + spacing));
                card.setY(colY[shortestCol]);
                colY[shortestCol] += card.getHeight() + 10;
            }

            legitCardContainer.addCard(visualsCard);
            legitCardContainer.addCard(debuggersCard);
            legitCardContainer.addCard(customTextCard);
            return;
        }

        legitTab.addWidget(new Label(contentX, contentY, "Visuals", Label.Style.TITLE));

        ToggleSwitch fullbrightToggle = new ToggleSwitch(contentX, contentY + 30, contentWidth - 20,
                "Enable Fullbright",
                "Maximizes gamma (Night Vision)",
                ConfigManager.data.legitFullbrightEnabled, value -> {
                    ConfigManager.data.legitFullbrightEnabled = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(fullbrightToggle);

        ToggleSwitch fireOverlayToggle = new ToggleSwitch(contentX, contentY + 60, contentWidth - 20,
                "Remove Fire Overlay",
                "Hides the fire overlay when on fire",
                ConfigManager.data.removeFireOverlay, value -> {
                    ConfigManager.data.removeFireOverlay = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(fireOverlayToggle);

        ToggleSwitch hideStatusEffectsToggle = new ToggleSwitch(contentX, contentY + 90, contentWidth - 20,
                "Hide Status Effects",
                "Hides status effect icons from the HUD and inventory",
                ConfigManager.data.hideStatusEffects, value -> {
                    ConfigManager.data.hideStatusEffects = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(hideStatusEffectsToggle);

        ToggleSwitch disableNearbyParticlesToggle = new ToggleSwitch(contentX, contentY + 120, contentWidth - 20,
                "Disable Nearby Particles",
                "Visually disables particles within 3 blocks of the player",
                ConfigManager.data.disableNearbyParticles, value -> {
                    ConfigManager.data.disableNearbyParticles = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(disableNearbyParticlesToggle);

        legitTab.addWidget(new Label(contentX, contentY + 150, "Debuggers", Label.Style.TITLE));

        int debugY = contentY + 180;
        addDebuggerWidgets(legitTab, contentX, debugY, contentWidth - 20);

        legitTab.addWidget(new Label(contentX, debugY + 150, "Custom Text", Label.Style.TITLE));
        addCustomTextWidgets(legitTab, contentX, debugY + 180, contentWidth - 20);
    }

    private ResizableCard createVisualsCard(int x, int y) {
        visualsCard = screen.createResizableCard("legit_visuals", x, y, 300, 100, "Visuals");
        int contentX = visualsCard.getContentX();
        int contentY = visualsCard.getContentY();

        ToggleSwitch fullbrightToggle = new ToggleSwitch(contentX, contentY, 260,
                "Enable Fullbright",
                "Maximizes gamma (Night Vision)",
                ConfigManager.data.legitFullbrightEnabled, value -> {
                    ConfigManager.data.legitFullbrightEnabled = value;
                    ConfigManager.save();
                });
        visualsCard.addChild(fullbrightToggle);

        ToggleSwitch fireOverlayToggle = new ToggleSwitch(contentX, contentY + 30, 260,
                "Remove Fire Overlay",
                "Hides the fire overlay when on fire",
                ConfigManager.data.removeFireOverlay, value -> {
                    ConfigManager.data.removeFireOverlay = value;
                    ConfigManager.save();
                });
        visualsCard.addChild(fireOverlayToggle);

        ToggleSwitch hideStatusEffectsToggle = new ToggleSwitch(contentX, contentY + 60, 260,
                "Hide Status Effects",
                "Hides status effect icons from the HUD and inventory",
                ConfigManager.data.hideStatusEffects, value -> {
                    ConfigManager.data.hideStatusEffects = value;
                    ConfigManager.save();
                });
        visualsCard.addChild(hideStatusEffectsToggle);

        ToggleSwitch disableNearbyParticlesToggle = new ToggleSwitch(contentX, contentY + 90, 260,
                "Disable Nearby Particles",
                "Visually disables particles within 2 blocks of the player",
                ConfigManager.data.disableNearbyParticles, value -> {
                    ConfigManager.data.disableNearbyParticles = value;
                    ConfigManager.save();
                });
        visualsCard.addChild(disableNearbyParticlesToggle);

        visualsCard.updateLayout();
        return visualsCard;
    }

    private ResizableCard createDebuggersCard(int x, int y) {
        debuggersCard = screen.createResizableCard("legit_debuggers", x, y, 300, 250, "Debuggers");
        int contentX = debuggersCard.getContentX();
        int contentY = debuggersCard.getContentY();

        ListView listView = new ListView(contentX, contentY, 260, 200);
        addDebuggerItems(listView);

        debuggersCard.addChild(listView);
        debuggersCard.updateLayout();
        return debuggersCard;
    }

    private void addDebuggerWidgets(TabPanel.Tab legitTab, int x, int startY, int width) {
        int y = startY;

        Button editHudsButton = new Button(x, y, width, 20, "Edit HUD Positions", () -> {
            if (Blackaddons.screenOpener != null) {
                Blackaddons.screenOpener.accept(new OverlayEditScreen(screen));
            }
        });
        legitTab.addWidget(editHudsButton);
        y += 30;

        ToggleSwitch debugOverlayToggle = new ToggleSwitch(x, y, width,
                "Global Debug Overlay",
                "Shows the shared BlackAddons debug HUD",
                BaseScreen.showDebugOverlay, value -> {
                    BaseScreen.showDebugOverlay = value;
                    ConfigManager.data.showDebugOverlay = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(debugOverlayToggle);
        y += 30;

        ToggleSwitch autoSSDebugToggle = new ToggleSwitch(x, y, width,
                "AutoSS Debug",
                "Shows AutoSS overlay and writes debug logs",
                ConfigManager.data.AutoSSDebug, value -> {
                    ConfigManager.data.AutoSSDebug = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(autoSSDebugToggle);
        y += 30;

        ToggleSwitch rotationDebugToggle = new ToggleSwitch(x, y, width,
                "Rotation Debugger",
                "Shows target rotation info and world visuals",
                ConfigManager.data.showRotationDebug, value -> {
                    ConfigManager.data.showRotationDebug = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(rotationDebugToggle);
        y += 30;

        ToggleSwitch locationDebugToggle = new ToggleSwitch(x, y, width,
                "Location Utils Debug",
                "Shows location, floor, boss and phase debug info",
                ConfigManager.data.showLocationDebug, value -> {
                    ConfigManager.data.showLocationDebug = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(locationDebugToggle);
        y += 30;

        ToggleSwitch alignDebugToggle = new ToggleSwitch(x, y, width,
                "Align Debugger",
                "Shows expected align math and measured final position",
                ConfigManager.data.showAlignDebug, value -> {
                    ConfigManager.data.showAlignDebug = value;
                    ConfigManager.save();
                });
        legitTab.addWidget(alignDebugToggle);
    }

    private void addDebuggerItems(ListView listView) {
        listView.addItem(new Button(0, 0, 260, 20, "Edit HUD Positions", () -> {
            if (Blackaddons.screenOpener != null) {
                Blackaddons.screenOpener.accept(new OverlayEditScreen(screen));
            }
        }));

        listView.addItem(new ToggleSwitch(0, 0, 260,
                "Global Debug Overlay",
                "Shows the shared BlackAddons debug HUD",
                BaseScreen.showDebugOverlay, value -> {
                    BaseScreen.showDebugOverlay = value;
                    ConfigManager.data.showDebugOverlay = value;
                    ConfigManager.save();
                }));

        listView.addItem(new ToggleSwitch(0, 0, 260,
                "AutoSS Debug",
                "Shows AutoSS overlay and writes debug logs",
                ConfigManager.data.AutoSSDebug, value -> {
                    ConfigManager.data.AutoSSDebug = value;
                    ConfigManager.save();
                }));

        listView.addItem(new ToggleSwitch(0, 0, 260,
                "Rotation Debugger",
                "Shows target rotation info and world visuals",
                ConfigManager.data.showRotationDebug, value -> {
                    ConfigManager.data.showRotationDebug = value;
                    ConfigManager.save();
                }));

        listView.addItem(new ToggleSwitch(0, 0, 260,
                "Location Utils Debug",
                "Shows location, floor, boss and phase debug info",
                ConfigManager.data.showLocationDebug, value -> {
                    ConfigManager.data.showLocationDebug = value;
                    ConfigManager.save();
                }));

        listView.addItem(new ToggleSwitch(0, 0, 260,
                "Align Debugger",
                "Shows expected align math and measured final position",
                ConfigManager.data.showAlignDebug, value -> {
                    ConfigManager.data.showAlignDebug = value;
                    ConfigManager.save();
                }));
    }


    private ResizableCard createCustomTextCard(int x, int y) {
        customTextCard = screen.createResizableCard("legit_custom_text", x, y, 300, 360, "Custom Text");
        int cx = customTextCard.getContentX();
        int cy = customTextCard.getContentY();
        int W = 260;
        int listHeight = 320;

        ListView listView = new ListView(cx, cy, W, listHeight);
        listView.setItemSpacing(4);

        listView.addItem(new ToggleSwitch(0, 0, W, "Custom Text",
                "Enable resolution-independent GPU custom text rendering.",
                ConfigManager.data.customTextEnabled,
                val -> { ConfigManager.data.customTextEnabled = val; ConfigManager.save(); }));

        listView.addItem(new ToggleSwitch(0, 0, W, "Custom Text: GUI Only",
                "Restrict custom text rendering to BlackAddons GUI screens only.",
                ConfigManager.data.customTextGuiOnly,
                val -> { ConfigManager.data.customTextGuiOnly = val; ConfigManager.save(); }));

        listView.addItem(new Label(0, 0, "Custom Font (Google Fonts):", Label.Style.SUBTITLE));

        Label fontStatusLabel = new Label(0, 0, "", Label.Style.BODY);
        updateFontStatus(fontStatusLabel);

        String savedFont = ConfigManager.data.customFontGoogleName != null ? ConfigManager.data.customFontGoogleName : "";
        final AutocompleteTextField[] fontFieldRef = new AutocompleteTextField[1];
        org.blackum.blackaddons.gui.render.font.GoogleFontsList.startLazyLoad(() -> {
            if (fontFieldRef[0] != null) {
                fontFieldRef[0].setPlaceholder("Type to search " +
                        org.blackum.blackaddons.gui.render.font.GoogleFontsList.get().size() + " fonts...");
                fontFieldRef[0].refreshSuggestions();
            }
        });
        AutocompleteTextField fontField = new AutocompleteTextField(
                0, 0, W, Theme.TEXTFIELD_HEIGHT,
                "Type to search fonts...",
                GoogleFontsList::get);
        fontFieldRef[0] = fontField;
        fontField.setText(savedFont);
        fontField.setOnSelect(name -> {
            String trimmed = name.trim();
            ConfigManager.data.customFontGoogleName = trimmed;
            ConfigManager.save();
            fontStatusLabel.setText("Downloading...");
            CustomFontRenderer.getInstance().reloadAsync(
                    () -> updateFontStatus(fontStatusLabel),
                    progress -> {
                        String msg = formatDownloadProgress(progress);
                        net.minecraft.client.Minecraft.getInstance().execute(() -> fontStatusLabel.setText(msg));
                    });
        });
        listView.addItem(fontField);
        listView.addItem(fontStatusLabel);

        listView.addItem(new Button(0, 0, W, Theme.BUTTON_HEIGHT,
                "Use Default Font", () -> {
                    ConfigManager.data.customFontGoogleName = "";
                    ConfigManager.save();
                    fontField.setText("");
                    fontStatusLabel.setText("Loading...");
                    CustomFontRenderer.getInstance().reloadAsync(() -> updateFontStatus(fontStatusLabel));
                }));

        Label customTextScaleLabel = new Label(0, 0,
                "Size: " + String.format(Locale.ROOT, "%.2f", ConfigManager.data.customTextScale) + "px", Label.Style.BODY);
        listView.addItem(customTextScaleLabel);

        Slider customTextScaleSlider = new Slider(0, 0, W, 4f, 32f,
                ConfigManager.data.customTextScale, val -> {
                    float r = Math.round(val * 20.0f) / 20.0f;
                    customTextScaleLabel.setText("Size: " + String.format(Locale.ROOT, "%.2f", r) + "px");
                }).onRelease(val -> {
                    ConfigManager.data.customTextScale = Math.round(val * 20.0f) / 20.0f;
                    ConfigManager.save();
                });
        listView.addItem(customTextScaleSlider);

        listView.addItem(new Button(0, 0, W, Theme.BUTTON_HEIGHT, "Reset Size", () -> {
            float def = 12.5f;
            ConfigManager.data.customTextScale = def;
            ConfigManager.save();
            customTextScaleSlider.setValue(def);
            customTextScaleLabel.setText("Size: " + String.format(Locale.ROOT, "%.2f", def) + "px");
        }));

        listView.addItem(new Label(0, 0, "Antialiasing", Label.Style.SUBTITLE));
        ToggleSwitch aaToggle = new ToggleSwitch(0, 0, W, "Enable Antialiasing",
                "Smooths SDF edges to reduce jagged text.",
                ConfigManager.data.customFontAntiAliasing,
                val -> { ConfigManager.data.customFontAntiAliasing = val; ConfigManager.save(); });
        listView.addItem(aaToggle);

        Label aaWidthLabel = new Label(0, 0,
                "Antialias Width: " + String.format(Locale.ROOT, "%.2f", ConfigManager.data.customFontAntiAliasingWidth), Label.Style.BODY);
        listView.addItem(aaWidthLabel);
        Slider aaWidthSlider = new Slider(0, 0, W, 0.25f, 1.5f, ConfigManager.data.customFontAntiAliasingWidth, val -> {
            float r = Math.round(val * 100f) / 100f;
            aaWidthLabel.setText("Antialias Width: " + String.format(Locale.ROOT, "%.2f", r));
        });
        aaWidthSlider.onRelease(val -> {
            ConfigManager.data.customFontAntiAliasingWidth = Math.round(val * 100f) / 100f;
            ConfigManager.save();
        });
        listView.addItem(aaWidthSlider);
        listView.addItem(new Button(0, 0, W, Theme.BUTTON_HEIGHT, "Reset Antialiasing", () -> {
            ConfigManager.data.customFontAntiAliasing = true;
            ConfigManager.data.customFontAntiAliasingWidth = 0.5f;
            ConfigManager.save();
            aaToggle.setValue(true);
            aaWidthSlider.setValue(0.5f);
            aaWidthLabel.setText("Antialias Width: 0.50");
        }));

        listView.addItem(new Label(0, 0, "Bold", Label.Style.SUBTITLE));
        ToggleSwitch boldToggle = new ToggleSwitch(0, 0, W, "Enable Bold",
                "Expands the glyph shape instead of drawing duplicates.",
                ConfigManager.data.customFontBold,
                val -> { ConfigManager.data.customFontBold = val; ConfigManager.save(); });
        listView.addItem(boldToggle);

        Label boldStrLabel = new Label(0, 0,
                "Bold Strength: " + String.format(Locale.ROOT, "%.2f", ConfigManager.data.customFontBoldStrength), Label.Style.BODY);
        listView.addItem(boldStrLabel);
        Slider boldStrSlider = new Slider(0, 0, W, 0.0f, 1.0f, ConfigManager.data.customFontBoldStrength, val -> {
            float r = Math.round(val * 100f) / 100f;
            boldStrLabel.setText("Bold Strength: " + String.format(Locale.ROOT, "%.2f", r));
        });
        boldStrSlider.onRelease(val -> {
            ConfigManager.data.customFontBoldStrength = Math.round(val * 100f) / 100f;
            ConfigManager.save();
        });
        listView.addItem(boldStrSlider);
        listView.addItem(new Button(0, 0, W, Theme.BUTTON_HEIGHT, "Reset Bold", () -> {
            ConfigManager.data.customFontBold = false;
            ConfigManager.data.customFontBoldStrength = 0.5f;
            ConfigManager.save();
            boldToggle.setValue(false);
            boldStrSlider.setValue(0.5f);
            boldStrLabel.setText("Bold Strength: 0.50");
        }));

        listView.addItem(new Label(0, 0, "Italic", Label.Style.SUBTITLE));
        ToggleSwitch italicToggle = new ToggleSwitch(0, 0, W, "Enable Italic",
                "Skews glyphs to simulate italic style.",
                ConfigManager.data.customFontItalic,
                val -> { ConfigManager.data.customFontItalic = val; ConfigManager.save(); });
        listView.addItem(italicToggle);

        Label italicSlantLabel = new Label(0, 0,
                "Italic Slant: " + String.format(Locale.ROOT, "%.2f", ConfigManager.data.customFontItalicSlant), Label.Style.BODY);
        listView.addItem(italicSlantLabel);
        Slider italicSlantSlider = new Slider(0, 0, W, 0.0f, 0.5f, ConfigManager.data.customFontItalicSlant, val -> {
            float r = Math.round(val * 100f) / 100f;
            italicSlantLabel.setText("Italic Slant: " + String.format(Locale.ROOT, "%.2f", r));
        });
        italicSlantSlider.onRelease(val -> {
            ConfigManager.data.customFontItalicSlant = Math.round(val * 100f) / 100f;
            ConfigManager.save();
        });
        listView.addItem(italicSlantSlider);
        listView.addItem(new Button(0, 0, W, Theme.BUTTON_HEIGHT, "Reset Italic", () -> {
            ConfigManager.data.customFontItalic = false;
            ConfigManager.data.customFontItalicSlant = 0.25f;
            ConfigManager.save();
            italicToggle.setValue(false);
            italicSlantSlider.setValue(0.25f);
            italicSlantLabel.setText("Italic Slant: 0.25");
        }));

        listView.addItem(new Label(0, 0, "Shadow", Label.Style.SUBTITLE));
        ToggleSwitch shadowToggle = new ToggleSwitch(0, 0, W, "Enable Shadow",
                "Draws a drop shadow behind each glyph.",
                ConfigManager.data.customFontShadow,
                val -> { ConfigManager.data.customFontShadow = val; ConfigManager.save(); });
        listView.addItem(shadowToggle);

        listView.addItem(new Label(0, 0, "Shadow Color", Label.Style.BODY));
        SmallColorPicker shadowColorPicker = new SmallColorPicker(0, 0, ConfigManager.data.customFontShadowColor, color -> {
            ConfigManager.data.customFontShadowColor = color;
            ConfigManager.save();
        });
        listView.addItem(shadowColorPicker);

        Label shadowDxLabel = new Label(0, 0,
                "Offset X: " + String.format(Locale.ROOT, "%.1f", ConfigManager.data.customFontShadowOffsetX), Label.Style.BODY);
        listView.addItem(shadowDxLabel);
        Slider shadowDxSlider = new Slider(0, 0, W, -4f, 4f, ConfigManager.data.customFontShadowOffsetX, val -> {
            float r = Math.round(val * 10f) / 10f;
            shadowDxLabel.setText("Offset X: " + String.format(Locale.ROOT, "%.1f", r));
        });
        shadowDxSlider.onRelease(val -> {
            ConfigManager.data.customFontShadowOffsetX = Math.round(val * 10f) / 10f;
            ConfigManager.save();
        });
        listView.addItem(shadowDxSlider);

        Label shadowDyLabel = new Label(0, 0,
                "Offset Y: " + String.format(Locale.ROOT, "%.1f", ConfigManager.data.customFontShadowOffsetY), Label.Style.BODY);
        listView.addItem(shadowDyLabel);
        Slider shadowDySlider = new Slider(0, 0, W, -4f, 4f, ConfigManager.data.customFontShadowOffsetY, val -> {
            float r = Math.round(val * 10f) / 10f;
            shadowDyLabel.setText("Offset Y: " + String.format(Locale.ROOT, "%.1f", r));
        });
        shadowDySlider.onRelease(val -> {
            ConfigManager.data.customFontShadowOffsetY = Math.round(val * 10f) / 10f;
            ConfigManager.save();
        });
        listView.addItem(shadowDySlider);

        listView.addItem(new Button(0, 0, W, Theme.BUTTON_HEIGHT, "Reset Shadow", () -> {
            ConfigManager.data.customFontShadow = false;
            ConfigManager.data.customFontShadowColor = 0xAA000000;
            ConfigManager.data.customFontShadowOffsetX = 0.5f;
            ConfigManager.data.customFontShadowOffsetY = 0.5f;
            ConfigManager.save();
            shadowToggle.setValue(false);
            shadowColorPicker.setColor(0xAA000000);
            shadowDxSlider.setValue(0.5f);
            shadowDxLabel.setText("Offset X: 0.5");
            shadowDySlider.setValue(0.5f);
            shadowDyLabel.setText("Offset Y: 0.5");
        }));

        listView.addItem(new Label(0, 0, "Outline", Label.Style.SUBTITLE));
        ToggleSwitch outlineToggle = new ToggleSwitch(0, 0, W, "Enable Outline",
                "Draws one larger glyph behind the original as the outline.",
                ConfigManager.data.customFontOutline,
                val -> { ConfigManager.data.customFontOutline = val; ConfigManager.save(); });
        listView.addItem(outlineToggle);

        listView.addItem(new Label(0, 0, "Outline Color", Label.Style.BODY));
        SmallColorPicker outlineColorPicker = new SmallColorPicker(0, 0, ConfigManager.data.customFontOutlineColor, color -> {
            ConfigManager.data.customFontOutlineColor = color;
            ConfigManager.save();
        });
        listView.addItem(outlineColorPicker);

        Label outlineWidthLabel = new Label(0, 0,
                "Outline Width: " + String.format(Locale.ROOT, "%.1f", ConfigManager.data.customFontOutlineWidth), Label.Style.BODY);
        listView.addItem(outlineWidthLabel);
        Slider outlineWidthSlider = new Slider(0, 0, W, 0.0f, 1.0f, ConfigManager.data.customFontOutlineWidth, val -> {
            float r = Math.round(val * 10f) / 10f;
            outlineWidthLabel.setText("Outline Width: " + String.format(Locale.ROOT, "%.1f", r));
        });
        outlineWidthSlider.onRelease(val -> {
            ConfigManager.data.customFontOutlineWidth = Math.round(val * 10f) / 10f;
            ConfigManager.save();
        });
        listView.addItem(outlineWidthSlider);
 
        listView.addItem(new Button(0, 0, W, Theme.BUTTON_HEIGHT, "Reset Outline", () -> {
            ConfigManager.data.customFontOutline = false;
            ConfigManager.data.customFontOutlineColor = 0xFF000000;
            ConfigManager.data.customFontOutlineWidth = 0.5f;
            ConfigManager.save();
            outlineToggle.setValue(false);
            outlineColorPicker.setColor(0xFF000000);
            outlineWidthSlider.setValue(0.5f);
            outlineWidthLabel.setText("Outline Width: 0.5");
        }));

        customTextCard.addChild(listView);
        customTextCard.updateLayout();
        return customTextCard;
    }

    private static String formatDownloadProgress(long[] progress) {
        long dl  = progress[0];
        long tot = progress[1];
        String dlStr = formatSize(dl);
        return tot > 0 ? "Downloading: " + dlStr + " / " + formatSize(tot) : "Downloading: " + dlStr;
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024L)             return bytes + " B";
        if (bytes < 1024L * 1024L)     return (bytes / 1024L) + " KB";
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void updateFontStatus(Label label) {
        CustomFontRenderer renderer = CustomFontRenderer.getInstance();
        if (renderer.isLoading()) {
            label.setText("Fetching font...");
        } else if (renderer.isInitialized()) {
            String name = ConfigManager.data.customFontGoogleName;
            label.setText(name == null || name.isBlank() ? "Default font loaded" : "Loaded: " + name);
        } else {
            label.setText("Not initialized");
        }
    }

    private void addCustomTextWidgets(TabPanel.Tab legitTab, int x, int startY, int width) {
        int y = startY;

        legitTab.addWidget(new ToggleSwitch(x, y, width, "Custom Text",
                "Enable resolution-independent GPU custom text rendering.", ConfigManager.data.customTextEnabled,
                val -> { ConfigManager.data.customTextEnabled = val; ConfigManager.save(); }));
        y += 30;

        legitTab.addWidget(new ToggleSwitch(x, y, width, "Custom Text: GUI Only",
                "Restrict custom text rendering to BlackAddons GUI screens only.", ConfigManager.data.customTextGuiOnly,
                val -> { ConfigManager.data.customTextGuiOnly = val; ConfigManager.save(); }));
        y += 30;

        legitTab.addWidget(new Label(x, y, "Custom Font (Google Fonts):", Label.Style.BODY));
        y += 12;
        Label flatFontStatus = new Label(x, y + 22, "", Label.Style.BODY);
        updateFontStatus(flatFontStatus);
        final AutocompleteTextField[] flatFontFieldRef = new AutocompleteTextField[1];
        GoogleFontsList.startLazyLoad(() -> {
            if (flatFontFieldRef[0] != null) {
                flatFontFieldRef[0].setPlaceholder("Type to search " +
                        GoogleFontsList.get().size() + " fonts...");
                flatFontFieldRef[0].refreshSuggestions();
            }
        });
        AutocompleteTextField flatFontField = new AutocompleteTextField(
                x, y, width, Theme.TEXTFIELD_HEIGHT,
                "Type to search fonts...",
                GoogleFontsList::get);
        flatFontFieldRef[0] = flatFontField;
        flatFontField.setText(ConfigManager.data.customFontGoogleName != null ? ConfigManager.data.customFontGoogleName : "");
        flatFontField.setOnSelect(name -> {
            ConfigManager.data.customFontGoogleName = name.trim();
            ConfigManager.save();
            flatFontStatus.setText("Downloading...");
            CustomFontRenderer.getInstance().reloadAsync(
                    () -> updateFontStatus(flatFontStatus),
                    progress -> {
                        String msg = formatDownloadProgress(progress);
                        net.minecraft.client.Minecraft.getInstance().execute(() -> flatFontStatus.setText(msg));
                    });
        });
        legitTab.addWidget(flatFontField);
        legitTab.addWidget(flatFontStatus);
        legitTab.addWidget(new Button(x, y + 44, width, Theme.BUTTON_HEIGHT, "Use Default Font", () -> {
            ConfigManager.data.customFontGoogleName = "";
            ConfigManager.save();
            flatFontField.setText("");
            flatFontStatus.setText("Loading...");
            CustomFontRenderer.getInstance().reloadAsync(() -> updateFontStatus(flatFontStatus));
        }));
        y += 78;

        Label scaleLabel = new Label(x, y,
                "Size: " + String.format(Locale.ROOT, "%.2f", ConfigManager.data.customTextScale) + "px", Label.Style.BODY);
        legitTab.addWidget(scaleLabel);
        y += 13;
        Slider scaleSlider = new Slider(x, y, width, 4f, 32f, ConfigManager.data.customTextScale, val -> {
            float r = Math.round(val * 20.0f) / 20.0f;
            scaleLabel.setText("Size: " + String.format(Locale.ROOT, "%.2f", r) + "px");
        }).onRelease(val -> {
            ConfigManager.data.customTextScale = Math.round(val * 20.0f) / 20.0f;
            ConfigManager.save();
        });
        legitTab.addWidget(scaleSlider);
        y += 30;
        legitTab.addWidget(new Button(x, y, 100, Theme.BUTTON_HEIGHT, "Reset Size", () -> {
            float def = 12.5f;
            ConfigManager.data.customTextScale = def;
            ConfigManager.save();
            scaleSlider.setValue(def);
            scaleLabel.setText("Size: " + String.format(Locale.ROOT, "%.2f", def) + "px");
        }));
        y += 28;

        legitTab.addWidget(new Label(x, y, "Antialiasing", Label.Style.SUBTITLE));
        y += 14;
        ToggleSwitch aaToggle = new ToggleSwitch(x, y, width, "Enable Antialiasing",
                "Smooths SDF edges to reduce jagged text.", ConfigManager.data.customFontAntiAliasing,
                val -> { ConfigManager.data.customFontAntiAliasing = val; ConfigManager.save(); });
        legitTab.addWidget(aaToggle);
        y += 30;
        Label aaWidthLabel = new Label(x, y,
                "Antialias Width: " + String.format(Locale.ROOT, "%.2f", ConfigManager.data.customFontAntiAliasingWidth), Label.Style.BODY);
        legitTab.addWidget(aaWidthLabel);
        y += 13;
        Slider aaWidthSlider = new Slider(x, y, width, 0.05f, 1.5f, ConfigManager.data.customFontAntiAliasingWidth, val -> {
            float r = Math.round(val * 100f) / 100f;
            aaWidthLabel.setText("Antialias Width: " + String.format(Locale.ROOT, "%.2f", r));
        }).onRelease(val -> {
            ConfigManager.data.customFontAntiAliasingWidth = Math.round(val * 100f) / 100f;
            ConfigManager.save();
        });
        legitTab.addWidget(aaWidthSlider);
        y += 30;
        legitTab.addWidget(new Button(x, y, 140, Theme.BUTTON_HEIGHT, "Reset Antialiasing", () -> {
            ConfigManager.data.customFontAntiAliasing = true;
            ConfigManager.data.customFontAntiAliasingWidth = 0.5f;
            ConfigManager.save();
            aaToggle.setValue(true);
            aaWidthSlider.setValue(0.5f);
            aaWidthLabel.setText("Antialias Width: 0.50");
        }));
        y += 28;

        legitTab.addWidget(new Label(x, y, "Bold", Label.Style.SUBTITLE));
        y += 14;
        legitTab.addWidget(new ToggleSwitch(x, y, width, "Enable Bold",
                "Expands the glyph shape instead of drawing duplicates.", ConfigManager.data.customFontBold,
                val -> { ConfigManager.data.customFontBold = val; ConfigManager.save(); }));
        y += 30;
        Label boldStrLabel = new Label(x, y,
                "Bold Strength: " + String.format(Locale.ROOT, "%.2f", ConfigManager.data.customFontBoldStrength), Label.Style.BODY);
        legitTab.addWidget(boldStrLabel);
        y += 13;
        Slider boldStrSlider = new Slider(x, y, width, 0.0f, 1.0f, ConfigManager.data.customFontBoldStrength, val -> {
            float r = Math.round(val * 100f) / 100f;
            boldStrLabel.setText("Bold Strength: " + String.format(Locale.ROOT, "%.2f", r));
        }).onRelease(val -> {
            ConfigManager.data.customFontBoldStrength = Math.round(val * 100f) / 100f;
            ConfigManager.save();
        });
        legitTab.addWidget(boldStrSlider);
        y += 30;

        legitTab.addWidget(new Label(x, y, "Italic", Label.Style.SUBTITLE));
        y += 14;
        legitTab.addWidget(new ToggleSwitch(x, y, width, "Enable Italic",
                "Skews glyphs.", ConfigManager.data.customFontItalic,
                val -> { ConfigManager.data.customFontItalic = val; ConfigManager.save(); }));
        y += 30;
        Label italicSlantLabel = new Label(x, y,
                "Italic Slant: " + String.format(Locale.ROOT, "%.2f", ConfigManager.data.customFontItalicSlant), Label.Style.BODY);
        legitTab.addWidget(italicSlantLabel);
        y += 13;
        Slider italicSlantSlider = new Slider(x, y, width, 0.0f, 0.5f, ConfigManager.data.customFontItalicSlant, val -> {
            float r = Math.round(val * 100f) / 100f;
            italicSlantLabel.setText("Italic Slant: " + String.format(Locale.ROOT, "%.2f", r));
        }).onRelease(val -> {
            ConfigManager.data.customFontItalicSlant = Math.round(val * 100f) / 100f;
            ConfigManager.save();
        });
        legitTab.addWidget(italicSlantSlider);
        y += 30;

        legitTab.addWidget(new Label(x, y, "Shadow", Label.Style.SUBTITLE));
        y += 14;
        legitTab.addWidget(new ToggleSwitch(x, y, width, "Enable Shadow",
                "Draws a drop shadow behind each glyph.", ConfigManager.data.customFontShadow,
                val -> { ConfigManager.data.customFontShadow = val; ConfigManager.save(); }));
        y += 30;
        legitTab.addWidget(new Label(x, y, "Shadow Color", Label.Style.BODY));
        y += 12;
        SmallColorPicker shadowColorPicker = new SmallColorPicker(x, y, ConfigManager.data.customFontShadowColor, color -> {
            ConfigManager.data.customFontShadowColor = color;
            ConfigManager.save();
        });
        legitTab.addWidget(shadowColorPicker);
        y += SmallColorPicker.S_HEIGHT + 10;
        Label shadowDxLabel = new Label(x, y,
                "Offset X: " + String.format(Locale.ROOT, "%.1f", ConfigManager.data.customFontShadowOffsetX), Label.Style.BODY);
        legitTab.addWidget(shadowDxLabel);
        y += 13;
        Slider shadowDxSlider = new Slider(x, y, width, -4f, 4f, ConfigManager.data.customFontShadowOffsetX, val -> {
            float r = Math.round(val * 10f) / 10f;
            shadowDxLabel.setText("Offset X: " + String.format(Locale.ROOT, "%.1f", r));
        }).onRelease(val -> {
            ConfigManager.data.customFontShadowOffsetX = Math.round(val * 10f) / 10f;
            ConfigManager.save();
        });
        legitTab.addWidget(shadowDxSlider);
        y += 30;
        Label shadowDyLabel = new Label(x, y,
                "Offset Y: " + String.format(Locale.ROOT, "%.1f", ConfigManager.data.customFontShadowOffsetY), Label.Style.BODY);
        legitTab.addWidget(shadowDyLabel);
        y += 13;
        Slider shadowDySlider = new Slider(x, y, width, -4f, 4f, ConfigManager.data.customFontShadowOffsetY, val -> {
            float r = Math.round(val * 10f) / 10f;
            shadowDyLabel.setText("Offset Y: " + String.format(Locale.ROOT, "%.1f", r));
        }).onRelease(val -> {
            ConfigManager.data.customFontShadowOffsetY = Math.round(val * 10f) / 10f;
            ConfigManager.save();
        });
        legitTab.addWidget(shadowDySlider);
        y += 30;

        legitTab.addWidget(new Label(x, y, "Outline", Label.Style.SUBTITLE));
        y += 14;
        legitTab.addWidget(new ToggleSwitch(x, y, width, "Enable Outline",
                "Draws one larger glyph behind the original as the outline.", ConfigManager.data.customFontOutline,
                val -> { ConfigManager.data.customFontOutline = val; ConfigManager.save(); }));
        y += 30;
        legitTab.addWidget(new Label(x, y, "Outline Color", Label.Style.BODY));
        y += 12;
        SmallColorPicker outlineColorPicker = new SmallColorPicker(x, y, ConfigManager.data.customFontOutlineColor, color -> {
            ConfigManager.data.customFontOutlineColor = color;
            ConfigManager.save();
        });
        legitTab.addWidget(outlineColorPicker);
        y += SmallColorPicker.S_HEIGHT + 10;
        Label outlineWidthLabel = new Label(x, y,
                "Outline Width: " + String.format(Locale.ROOT, "%.1f", ConfigManager.data.customFontOutlineWidth), Label.Style.BODY);
        legitTab.addWidget(outlineWidthLabel);
        y += 13;
        Slider outlineWidthSlider = new Slider(x, y, width, 0.0f, 1.0f, ConfigManager.data.customFontOutlineWidth, val -> {
            float r = Math.round(val * 10f) / 10f;
            outlineWidthLabel.setText("Outline Width: " + String.format(Locale.ROOT, "%.1f", r));
        }).onRelease(val -> {
            ConfigManager.data.customFontOutlineWidth = Math.round(val * 10f) / 10f;
            ConfigManager.save();
        });
        legitTab.addWidget(outlineWidthSlider);
    }
}
