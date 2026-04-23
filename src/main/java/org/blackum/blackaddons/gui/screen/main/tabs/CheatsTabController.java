package org.blackum.blackaddons.gui.screen.main.tabs;


import java.util.List;
import java.util.Locale;

import org.blackum.blackaddons.Blackaddons;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.feature.cheat.Freecam;
import org.blackum.blackaddons.feature.cheat.Perspective;
import org.blackum.blackaddons.gui.screen.main.BlackAddonsGUI;
import org.blackum.blackaddons.gui.screen.overlay.OverlayEditScreen;
import org.blackum.blackaddons.gui.widget.base.Button;
import org.blackum.blackaddons.gui.widget.base.KeybindButton;
import org.blackum.blackaddons.gui.widget.base.Label;
import org.blackum.blackaddons.gui.widget.input.Dropdown;
import org.blackum.blackaddons.gui.widget.input.Slider;
import org.blackum.blackaddons.gui.widget.input.ToggleSwitch;
import org.blackum.blackaddons.gui.widget.layout.CardContainer;
import org.blackum.blackaddons.gui.widget.layout.ListView;
import org.blackum.blackaddons.gui.widget.layout.ResizableCard;
import org.blackum.blackaddons.gui.widget.layout.TabPanel;

public class CheatsTabController extends SimpleTabController {
    private ResizableCard autoTntCard;
    private ResizableCard autoSSCard;
    private ResizableCard rotationCard;
    private ResizableCard autoBMCard;
    private ResizableCard freecamCard;
    private ResizableCard perspectiveCard;
    private Dropdown s2Dropdown;
    private Dropdown s3Dropdown;
    private Dropdown s4Dropdown;
    private Label startDelayLabel;
    private Slider startDelaySlider;

    public CheatsTabController(BlackAddonsGUI screen) {
        super(screen);
    }

    @Override
    public void init(TabPanel.Tab cheatsTab) {
        int contentX = cheatsTab.getParent().getContentX();
        int contentY = cheatsTab.getParent().getContentY();
        int contentWidth = cheatsTab.getParent().getContentWidth();

        if (!ConfigManager.data.useCardLayout) {
            cheatsTab.addWidget(new Label(contentX, contentY, "AutoTnt", Label.Style.TITLE));

            ToggleSwitch enableToggle = new ToggleSwitch(contentX, contentY + 30, contentWidth - 20,
                    "Enable AutoTnt",
                    "Automatically places TNT",
                    ConfigManager.data.autoTntConfig.AutoTNTEnabled, value -> {
                        ConfigManager.data.autoTntConfig.AutoTNTEnabled = value;
                        ConfigManager.save();
                    });
            cheatsTab.addWidget(enableToggle);

            Label tickLabel = new Label(contentX, contentY + 80,
                    "Tick Delay: " + ConfigManager.data.autoTntConfig.AutoTNTDelay + " ticks", Label.Style.BODY);
            cheatsTab.addWidget(tickLabel);

            Slider tickSlider = new Slider(contentX, contentY + 100, contentWidth - 20, 3, 10,
                    ConfigManager.data.autoTntConfig.AutoTNTDelay, val -> {
                        int ticks = Math.round(val);
                        if (ticks != ConfigManager.data.autoTntConfig.AutoTNTDelay) {
                            ConfigManager.data.autoTntConfig.AutoTNTDelay = ticks;
                            tickLabel.setText("Tick Delay: " + ticks + " ticks");
                            ConfigManager.save();
                        }
                    });
            cheatsTab.addWidget(tickSlider);

            cheatsTab.addWidget(new Label(contentX, contentY + 140, "AutoSS Solver", Label.Style.TITLE));

            ToggleSwitch ssEnableToggle = new ToggleSwitch(contentX, contentY + 170, contentWidth - 20,
                    "Enable AutoSS",
                    "Automatically solves F7/M7 Simon Says",
                    ConfigManager.data.AutoSSEnabled, value -> {
                        ConfigManager.data.AutoSSEnabled = value;
                        ConfigManager.save();
                    });
            cheatsTab.addWidget(ssEnableToggle);

            Label ssTickLabel = new Label(contentX, contentY + 220,
                    delayLabel(ConfigManager.data.AutoSSDelay), Label.Style.BODY);
            cheatsTab.addWidget(ssTickLabel);

            Slider ssTickSlider = new Slider(contentX, contentY + 240, contentWidth - 20, 0, 20,
                    ConfigManager.data.AutoSSDelay, val -> {
                        int ticks = Math.round(val);
                        if (ticks != ConfigManager.data.AutoSSDelay) {
                            ConfigManager.data.AutoSSDelay = ticks;
                            ssTickLabel.setText(delayLabel(ticks));
                            ConfigManager.save();
                        }
                    });
            cheatsTab.addWidget(ssTickSlider);

            Label ssDistLabel = new Label(contentX, contentY + 290,
                    String.format(java.util.Locale.ROOT, "Max Distance: %.1f blocks", ConfigManager.data.AutoSSDistanceLimit), Label.Style.BODY);
            cheatsTab.addWidget(ssDistLabel);

            Slider ssDistSlider = new Slider(contentX, contentY + 310, contentWidth - 20, 2.0f, 10.0f,
                    ConfigManager.data.AutoSSDistanceLimit, val -> {
                        ConfigManager.data.AutoSSDistanceLimit = val;
                        ssDistLabel.setText(String.format(java.util.Locale.ROOT, "Max Distance: %.1f blocks", val));
                        ConfigManager.save();
                    });
            cheatsTab.addWidget(ssDistSlider);
            
            cheatsTab.addWidget(new Label(contentX, contentY + 360, "Auto Ballista Mechanic", Label.Style.TITLE));
            
            ToggleSwitch bmEnableToggle = new ToggleSwitch(contentX, contentY + 390, contentWidth - 20,
                    "Enable AutoBM",
                    "Automatically clicks ballista upgrades",
                    ConfigManager.data.autoBMConfig.AutoBMEnabled, value -> {
                        ConfigManager.data.autoBMConfig.AutoBMEnabled = value;
                        ConfigManager.save();
                    });
            cheatsTab.addWidget(bmEnableToggle);

            return;
        }

        Button resetLayout = new Button(contentX + 10, contentY, contentWidth - 20, "Reset Layout", () -> {
            screen.resetCardStates("autoTnt", "autoSS", "rotationSet", "autoBM", "freecam", "perspective");
        });
        cheatsTab.addWidget(resetLayout);

        CardContainer cheatsCardContainer = new CardContainer(contentX, contentY + 50, contentWidth, 540);
        cheatsTab.addWidget(cheatsCardContainer);

        int containerY = contentY + 60;
        int numCols = contentWidth < 680 ? 1 : (contentWidth < 1000 ? 2 : 3);
        int colWidth = 300;
        int spacing = 20;
        int[] colY = new int[numCols];
        for (int i = 0; i < numCols; i++) colY[i] = containerY;

        autoTntCard = createAutoTntCard(0, 0);
        autoSSCard = createAutoSSCard(0, 0);
        rotationCard = createRotationCard(0, 0);
        autoBMCard = createAutoBM(0, 0);
        freecamCard = createFreecamCard(0, 0);
        perspectiveCard = createPerspectiveCard(0, 0);

        List<ResizableCard> cards = List.of(autoTntCard, autoSSCard, rotationCard, autoBMCard, freecamCard, perspectiveCard);
        for (ResizableCard card : cards) {
            int shortestCol = 0;
            for (int i = 1; i < numCols; i++) {
                if (colY[i] < colY[shortestCol]) shortestCol = i;
            }

            card.setX(contentX + spacing + shortestCol * (colWidth + spacing));
            card.setY(colY[shortestCol]);
            colY[shortestCol] += card.getHeight() + spacing;
        }

        cheatsCardContainer.addCard(autoTntCard);
        cheatsCardContainer.addCard(autoSSCard);
        cheatsCardContainer.addCard(rotationCard);
        cheatsCardContainer.addCard(autoBMCard);
        cheatsCardContainer.addCard(freecamCard);
        cheatsCardContainer.addCard(perspectiveCard);
    }

    private ResizableCard createAutoTntCard(int x, int y) {
        autoTntCard = screen.createResizableCard("autoTnt", x, y, 300, 260, "AutoTnt");

        int contentX = autoTntCard.getContentX();
        int contentY = autoTntCard.getContentY();

        ToggleSwitch enableToggle = new ToggleSwitch(contentX, contentY, 260,
                "Enable AutoTnt",
                "Automatically places TNT",
                ConfigManager.data.autoTntConfig.AutoTNTEnabled, value -> {
                    ConfigManager.data.autoTntConfig.AutoTNTEnabled = value;
                    ConfigManager.save();
                });
        autoTntCard.addChild(enableToggle);

        Label tickLabel = new Label(contentX, contentY + 50,
                "Tick Delay: " + ConfigManager.data.autoTntConfig.AutoTNTDelay + " ticks", Label.Style.BODY);
        autoTntCard.addChild(tickLabel);

        Slider tickSlider = new Slider(contentX, contentY + 70, 260, 3, 10,
                ConfigManager.data.autoTntConfig.AutoTNTDelay, val -> {
                    int ticks = Math.round(val);
                    if (ticks != ConfigManager.data.autoTntConfig.AutoTNTDelay) {
                        ConfigManager.data.autoTntConfig.AutoTNTDelay = ticks;
                        tickLabel.setText("Tick Delay: " + ticks + " ticks");
                        ConfigManager.save();
                    }
                });
        autoTntCard.addChild(tickSlider);

        Label unequipLabel = new Label(contentX, contentY + 100,
                "Unequip Delay: " + ConfigManager.data.autoTntConfig.UnequipDelay + " ticks", Label.Style.BODY);
        autoTntCard.addChild(unequipLabel);

        Slider unequipSlider = new Slider(contentX, contentY + 120, 260, 3, 10,
                ConfigManager.data.autoTntConfig.UnequipDelay, val -> {
                    int ticks = Math.round(val);
                    if (ticks != ConfigManager.data.autoTntConfig.UnequipDelay) {
                        ConfigManager.data.autoTntConfig.UnequipDelay = ticks;
                        unequipLabel.setText("Unequip Delay: " + ticks + " ticks");
                        ConfigManager.save();
                    }
                });
        autoTntCard.addChild(unequipSlider);

        ToggleSwitch swapBackToggle = new ToggleSwitch(contentX, contentY + 150, 260,
                "Swap Back",
                "Switch to original item after interaction",
                ConfigManager.data.autoTntConfig.SwapBack, value -> {
                    ConfigManager.data.autoTntConfig.SwapBack = value;
                    ConfigManager.save();
                });
        autoTntCard.addChild(swapBackToggle);

        autoTntCard.updateLayout();
        return autoTntCard;
    }

    private ResizableCard createAutoSSCard(int x, int y) {
        autoSSCard = screen.createResizableCard("autoSS", x, y, 300, 310, "AutoSS Solver");

        int contentX = autoSSCard.getContentX();
        int contentY = autoSSCard.getContentY();

        ListView listView = new ListView(contentX, contentY, 260, 260);

        ToggleSwitch enableToggle = new ToggleSwitch(0, 0, 260,
                "Enable AutoSS",
                "Automatically solves F7 devices",
                ConfigManager.data.AutoSSEnabled, value -> {
            ConfigManager.data.AutoSSEnabled = value;
            ConfigManager.save();
        });
        listView.addItem(enableToggle);

        Label tickLabel = new Label(0, 0,
                delayLabel(ConfigManager.data.AutoSSDelay), Label.Style.BODY);
        listView.addItem(tickLabel);

        Slider tickSlider = new Slider(0, 0, 260, 0, 20,
                ConfigManager.data.AutoSSDelay, val -> {
            int ticks = Math.round(val);
            if (ticks != ConfigManager.data.AutoSSDelay) {
                ConfigManager.data.AutoSSDelay = ticks;
                tickLabel.setText(delayLabel(ticks));
                ConfigManager.save();
            }
        });
        listView.addItem(tickSlider);

        Label distLabel = new Label(0, 0,
                String.format(java.util.Locale.ROOT, "Max Distance: %.1f blocks", ConfigManager.data.AutoSSDistanceLimit), Label.Style.BODY);
        listView.addItem(distLabel);

        Slider distSlider = new Slider(0, 0, 260, 2.0f, 10.0f,
                ConfigManager.data.AutoSSDistanceLimit, val -> {
            ConfigManager.data.AutoSSDistanceLimit = val;
            distLabel.setText(String.format(java.util.Locale.ROOT, "Max Distance: %.1f blocks", val));
            ConfigManager.save();
        });
        listView.addItem(distSlider);

        Label speedLabel = new Label(0, 0,
                String.format(java.util.Locale.ROOT, "Rotation Speed: %.1f", ConfigManager.data.AutoSSRotationSpeed), Label.Style.BODY);
        listView.addItem(speedLabel);

        Slider speedSlider = new Slider(0, 0, 260, 1.0f, 50.0f,
                ConfigManager.data.AutoSSRotationSpeed, val -> {
            ConfigManager.data.AutoSSRotationSpeed = val;
            speedLabel.setText(String.format(java.util.Locale.ROOT, "Rotation Speed: %.1f", val));
            ConfigManager.save();
        });
        listView.addItem(speedSlider);

        Label curveLabel = new Label(0, 0,
                String.format(java.util.Locale.ROOT, "Rotation Curve: %.0f%%", ConfigManager.data.AutoSSRotationCurve * 100), Label.Style.BODY);
        listView.addItem(curveLabel);

        Slider curveSlider = new Slider(0, 0, 260, 0.0f, 200.0f,
                ConfigManager.data.AutoSSRotationCurve * 100, val -> {
            ConfigManager.data.AutoSSRotationCurve = val / 100f;
            curveLabel.setText(String.format(java.util.Locale.ROOT, "Rotation Curve: %.0f%%", val));
            ConfigManager.save();
        });
        listView.addItem(curveSlider);

        ToggleSwitch trySkipToggle = new ToggleSwitch(0, 0, 260,
                "Try SS Skip",
                "Clicks start button 3 times for potential skip",
                ConfigManager.data.AutoSSTrySkip, value -> {
            ConfigManager.data.AutoSSTrySkip = value;
            ConfigManager.save();
        });
        trySkipToggle.setVisible(ConfigManager.data.AutoSSAutoStart);

        ToggleSwitch autoStartToggle = new ToggleSwitch(0, 0, 260,
                "Auto SS Start",
                "Automatically aims and clicks the start button",
                ConfigManager.data.AutoSSAutoStart, value -> {
            ConfigManager.data.AutoSSAutoStart = value;
            trySkipToggle.setVisible(value);
            startDelayLabel.setVisible(value);
            startDelaySlider.setVisible(value);
            ConfigManager.save();
        });
        listView.addItem(autoStartToggle);

        startDelayLabel = new Label(0, 0,
                startDelayLabel(ConfigManager.data.AutoSSAutoStartDelay), Label.Style.BODY);
        startDelayLabel.setVisible(ConfigManager.data.AutoSSAutoStart);
        listView.addItem(startDelayLabel);

        startDelaySlider = new Slider(0, 0, 260, 0, 40,
                ConfigManager.data.AutoSSAutoStartDelay, val -> {
            int ticks = Math.round(val);
            if (ticks != ConfigManager.data.AutoSSAutoStartDelay) {
                ConfigManager.data.AutoSSAutoStartDelay = ticks;
                startDelayLabel.setText(startDelayLabel(ticks));
                ConfigManager.save();
            }
        });
        startDelaySlider.setVisible(ConfigManager.data.AutoSSAutoStart);
        listView.addItem(startDelaySlider);

        listView.addItem(trySkipToggle);

        ToggleSwitch swapToItemToggle = new ToggleSwitch(0, 0, 260,
                "Swap to InfiniLeap on Complete",
                "Swaps to a specific item when device is finished",
                ConfigManager.data.AutoSSSwapToItem, value -> {
            ConfigManager.data.AutoSSSwapToItem = value;
            ConfigManager.save();
        });
        listView.addItem(swapToItemToggle);

        Dropdown swapModeDropdown = new Dropdown(0, 0, 260,
                "Select what to do when device is finished",
                List.of("Swap to InfiniLeap", "Swap and Open"),
                mode -> {
                    ConfigManager.data.AutoSSSwapMode = mode.equals("Swap and Open") ? 1 : 0;
                    ConfigManager.save();
                });
        swapModeDropdown.setSelectedIndex(ConfigManager.data.AutoSSSwapMode);
        listView.addItem(swapModeDropdown);

        ToggleSwitch debugToggle = new ToggleSwitch(0, 0, 260,
                "Debug Mode",
                "Shows debug overlay and logs to a file",
                ConfigManager.data.AutoSSDebug, value -> {
            ConfigManager.data.AutoSSDebug = value;
            ConfigManager.save();
        });
        listView.addItem(debugToggle);

        Button moveOverlayButton = new Button(0, 0, 260, 20, "Edit HUD Positions", () -> {
            if (Blackaddons.screenOpener != null) {
                Blackaddons.screenOpener.accept(new OverlayEditScreen(screen));
            }
        });

        listView.addItem(moveOverlayButton);

        autoSSCard.addChild(listView);
        autoSSCard.updateLayout();
        return autoSSCard;
    }

    private void collapseOtherDropdowns(Dropdown active) {
        if (s2Dropdown != null && s2Dropdown != active) s2Dropdown.collapse();
        if (s3Dropdown != null && s3Dropdown != active) s3Dropdown.collapse();
        if (s4Dropdown != null && s4Dropdown != active) s4Dropdown.collapse();
    }

    private ResizableCard createAutoBM(int x, int y) {
        autoBMCard = screen.createResizableCard("autoBM", x, y, 300, 310, "Auto Ballista Mechanic");

        int contentX = autoBMCard.getContentX();
        int contentY = autoBMCard.getContentY();

        ListView listView = new ListView(contentX, contentY, 260, 260);

        ToggleSwitch enabled = new ToggleSwitch(0, 0, 260,
                "Enabled",
                "Enabled and disables auto BM",
                ConfigManager.data.autoBMConfig.AutoBMEnabled, value -> {
            ConfigManager.data.autoBMConfig.AutoBMEnabled = value;
            ConfigManager.save();
        }
        );
        listView.addItem(enabled);

        Label min_fc_label = new Label(0, 0,
                String.format("Min first click delay: %.0fms", ConfigManager.data.autoBMConfig.min_fc_delay),
                Label.Style.BODY);
        listView.addItem(min_fc_label);

        Slider min_fc_slider = new Slider(0, 0, 260, 10.f, 1000.f,
                ConfigManager.data.autoBMConfig.min_fc_delay, val -> {
            ConfigManager.data.autoBMConfig.min_fc_delay = val;
            min_fc_label.setText(String.format("Min first click delay: %.0fms", val));
            ConfigManager.save();
        });
        listView.addItem(min_fc_slider);

        Label max_fc_label = new Label(0, 0,
                String.format("Max first click delay: %.0fms", ConfigManager.data.autoBMConfig.max_fc_delay),
                Label.Style.BODY);
        listView.addItem(max_fc_label);

        Slider max_fc_slider = new Slider(0, 0, 260, 10.f, 1000.f,
                ConfigManager.data.autoBMConfig.max_fc_delay, val -> {
            ConfigManager.data.autoBMConfig.max_fc_delay = val;
            max_fc_label.setText(String.format("Max first click delay: %.0fms", val));
            ConfigManager.save();
        });
        listView.addItem(max_fc_slider);

        Label min_between_click_label = new Label(0, 0,
                String.format("Min between click delay: %.0fms", ConfigManager.data.autoBMConfig.min_between_click_delay),
                Label.Style.BODY);
        listView.addItem(min_between_click_label);

        Slider min_between_click_slider = new Slider(0, 0, 260, 10.f, 1000.f,
                ConfigManager.data.autoBMConfig.min_between_click_delay, val -> {
            ConfigManager.data.autoBMConfig.min_between_click_delay = val;
            min_between_click_label.setText(String.format("Min between click delay: %.0fms", val));
            ConfigManager.save();
        });
        listView.addItem(min_between_click_slider);

        Label max_between_click_label = new Label(0, 0,
                String.format("Max between click delay: %.0fms", ConfigManager.data.autoBMConfig.max_between_click_delay),
                Label.Style.BODY);
        listView.addItem(max_between_click_label);

        Slider max_between_click_slider = new Slider(0, 0, 260, 10.f, 1000.f,
                ConfigManager.data.autoBMConfig.max_between_click_delay, val -> {
            ConfigManager.data.autoBMConfig.max_between_click_delay = val;
            max_between_click_label.setText(String.format("Max between click delay: %.0fms", val));
            ConfigManager.save();
        });
        listView.addItem(max_between_click_slider);

        autoBMCard.addChild(listView);
        autoBMCard.updateLayout();

        return autoBMCard;
    }

    private ResizableCard createRotationCard(int x, int y) {
        rotationCard = screen.createResizableCard("rotationSet", x, y, 300, 310, "Rotation Settings (custom actions)");
        int contentX = rotationCard.getContentX();
        int contentY = rotationCard.getContentY();

        ListView listView = new ListView(contentX, contentY, 260, 260);

        ToggleSwitch rotationDebugToggle = new ToggleSwitch(0, 0, 260,
                "Rotation Debugger",
                "Shows target rotation info and world visuals",
                ConfigManager.data.showRotationDebug, value -> {
                    ConfigManager.data.showRotationDebug = value;
                    ConfigManager.save();
                });
        listView.addItem(rotationDebugToggle);

        Button positionButton = new Button(0, 0, 260, 20, "Edit HUD Positions", () -> {
            if (Blackaddons.screenOpener != null) {
                Blackaddons.screenOpener.accept(new OverlayEditScreen(screen));
            }
        });

        listView.addItem(positionButton);

        ToggleSwitch humanizerToggle = new ToggleSwitch(0, 0, 260,
                "Enable Humanizer",
                "Applies curved paths and randomized targeting",
                ConfigManager.data.rotationHumanizerEnabled, value -> {
                    ConfigManager.data.rotationHumanizerEnabled = value;
                    ConfigManager.save();
                });
        listView.addItem(humanizerToggle);

        Label curveLabel = new Label(0, 0,
                String.format("Rotation Curve: %.0f%%", ConfigManager.data.rotationVariance * 100),
                Label.Style.BODY);
        listView.addItem(curveLabel);
        Slider curveSlider = new Slider(0, 0, 260, 0, 50,
                ConfigManager.data.rotationVariance * 100, val -> {
            ConfigManager.data.rotationVariance = val / 100f;
            curveLabel.setText(String.format("Rotation Curve: %.0f%%", val));
            ConfigManager.save();
        });
        listView.addItem(curveSlider);

        Label randomLabel = new Label(0, 0,
                String.format(java.util.Locale.ROOT, "Target Randomness: %.2f blocks", ConfigManager.data.rotationTargetRandomness),
                Label.Style.BODY);
        listView.addItem(randomLabel);
        Slider randomSlider = new Slider(0, 0, 260, 0.0f, 0.49f,
                ConfigManager.data.rotationTargetRandomness, val -> {
                    ConfigManager.data.rotationTargetRandomness = val;
                    randomLabel.setText(String.format(java.util.Locale.ROOT, "Target Randomness: %.2f blocks", val));
                    ConfigManager.save();
                });
        listView.addItem(randomSlider);

        Label speedLabel = new Label(0, 0,
                String.format(java.util.Locale.ROOT, "Speed: %.1f", ConfigManager.data.rotationSpeed),
                Label.Style.BODY);
        listView.addItem(speedLabel);
        Slider speedSlider = new Slider(0, 0, 260, 0.5f, 50.0f,
                ConfigManager.data.rotationSpeed, val -> {
                    ConfigManager.data.rotationSpeed = val;
                    speedLabel.setText(String.format(java.util.Locale.ROOT, "Speed: %.1f", val));
                    ConfigManager.save();
                });
        listView.addItem(speedSlider);

        Label slowdownLabel = new Label(0, 0,
                String.format("Distance Slowdown Rate: %.0f%%", ConfigManager.data.rotationDistanceSlowdown * 100),
                Label.Style.BODY);
        listView.addItem(slowdownLabel);
        Slider slowdownSlider = new Slider(0, 0, 260, 0.0f, 500.0f,
                ConfigManager.data.rotationDistanceSlowdown * 100, val -> {
                    ConfigManager.data.rotationDistanceSlowdown = val / 100f;
                    slowdownLabel.setText(String.format("Distance Slowdown Rate: %.0f%%", val));
                    ConfigManager.save();
                });
        listView.addItem(slowdownSlider);

        Label slowdownRadiusLabel = new Label(0, 0,
                String.format(java.util.Locale.ROOT, "Max Slowdown Distance: %.0f blocks", ConfigManager.data.rotationDistanceRadius),
                Label.Style.BODY);
        listView.addItem(slowdownRadiusLabel);
        Slider slowdownRadiusSlider = new Slider(0, 0, 260, 5.0f, 150.0f,
                ConfigManager.data.rotationDistanceRadius, val -> {
                    ConfigManager.data.rotationDistanceRadius = val;
                    slowdownRadiusLabel.setText(String.format(java.util.Locale.ROOT, "Max Slowdown Distance: %.0f blocks", val));
                    ConfigManager.save();
                });
        listView.addItem(slowdownRadiusSlider);

        Label fovLabel = new Label(0, 0,
                String.format(java.util.Locale.ROOT, "Distance Slowdown FOV: %.0f°", ConfigManager.data.rotationFovSlowdown),
                Label.Style.BODY);
        listView.addItem(fovLabel);
        Slider fovSlider = new Slider(0, 0, 260, 0.0f, 180.0f,
                ConfigManager.data.rotationFovSlowdown, val -> {
                    ConfigManager.data.rotationFovSlowdown = val;
                    fovLabel.setText(String.format(java.util.Locale.ROOT, "Distance Slowdown FOV: %.0f°", val));
                    ConfigManager.save();
                });
        listView.addItem(fovSlider);

        Label smoothLabel = new Label(0, 0,
                String.format(java.util.Locale.ROOT, "Smoothness: %.2f", ConfigManager.data.rotationSmoothness),
                Label.Style.BODY);
        listView.addItem(smoothLabel);
        Slider smoothSlider = new Slider(0, 0, 260, 0.0f, 1.0f,
                ConfigManager.data.rotationSmoothness, val -> {
                    ConfigManager.data.rotationSmoothness = val;
                    smoothLabel.setText(String.format(java.util.Locale.ROOT, "Smoothness: %.2f", val));
                    ConfigManager.save();
                });
        listView.addItem(smoothSlider);

        Label thresholdLabel = new Label(0, 0,
                String.format(java.util.Locale.ROOT, "Stop Threshold: %.2f", ConfigManager.data.rotationStopThreshold),
                Label.Style.BODY);
        listView.addItem(thresholdLabel);
        Slider thresholdSlider = new Slider(0, 0, 260, 0.01f, 5.0f,
                ConfigManager.data.rotationStopThreshold, val -> {
                    ConfigManager.data.rotationStopThreshold = val;
                    thresholdLabel.setText(String.format(java.util.Locale.ROOT, "Stop Threshold: %.2f", val));
                    ConfigManager.save();
                });
        listView.addItem(thresholdSlider);

        rotationCard.addChild(listView);
        rotationCard.updateLayout();
        return rotationCard;
    }

    private ResizableCard createFreecamCard(int x, int y) {
        freecamCard = screen.createResizableCard("freecam", x, y, 300, 300, "Freecam");
        int contentX = freecamCard.getContentX();
        int contentY = freecamCard.getContentY();

        ListView listView = new ListView(contentX, contentY, 260, 250);

        ToggleSwitch enableToggle = new ToggleSwitch(0, 0, 260,
                "Enabled",
                "Detaches camera from player",
                ConfigManager.data.freecamEnabled, value -> {
            if (value) {
                Freecam.getInstance().activate();
            } else {
                Freecam.getInstance().deactivate();
            }
            ConfigManager.data.freecamEnabled = Freecam.getInstance().isActive();
            ConfigManager.save();
        });
        listView.addItem(enableToggle);

        KeybindButton keybindButton = new KeybindButton(0, 0, 260,
                "Keybind",
                ConfigManager.data.freecamKeyCode,
                keyCode -> {
                    ConfigManager.data.freecamKeyCode = keyCode;
                    ConfigManager.save();
                });
        listView.addItem(keybindButton);

        Dropdown activationModeDropdown = new Dropdown(0, 0, 260,
                "Activation Mode",
                List.of("Toggle On/Off", "Hold Key"),
                mode -> {
                    ConfigManager.data.freecamHoldMode = mode.equals("Hold Key");
                    ConfigManager.save();
                });
        activationModeDropdown.setSelectedIndex(ConfigManager.data.freecamHoldMode ? 1 : 0);
        listView.addItem(activationModeDropdown);

        Label speedLabel = new Label(0, 0,
                String.format(Locale.ROOT, "Speed: %.1f", ConfigManager.data.freecamSpeed),
                Label.Style.BODY);
        listView.addItem(speedLabel);

        Slider speedSlider = new Slider(0, 0, 260, 0.1f, 10.0f,
                ConfigManager.data.freecamSpeed, val -> {
            ConfigManager.data.freecamSpeed = val;
            speedLabel.setText(String.format(Locale.ROOT, "Speed: %.1f", val));
            ConfigManager.save();
        });
        listView.addItem(speedSlider);

        ToggleSwitch showHandsToggle = new ToggleSwitch(0, 0, 260,
                "Show Hands",
                "Shows player hands in freecam",
                ConfigManager.data.freecamShowHands, value -> {
            ConfigManager.data.freecamShowHands = value;
            ConfigManager.save();
        });
        listView.addItem(showHandsToggle);

        freecamCard.addChild(listView);
        freecamCard.updateLayout();
        return freecamCard;
    }

    private ResizableCard createPerspectiveCard(int x, int y) {
        perspectiveCard = screen.createResizableCard("perspective", x, y, 300, 300, "Perspective");
        int contentX = perspectiveCard.getContentX();
        int contentY = perspectiveCard.getContentY();

        ListView listView = new ListView(contentX, contentY, 260, 250);

        ToggleSwitch enableToggle = new ToggleSwitch(0, 0, 260,
                "Enabled",
                "Orbits camera around player",
                ConfigManager.data.perspectiveEnabled, value -> {
            if (value) {
                Perspective.getInstance().activate();
            } else {
                Perspective.getInstance().deactivate();
            }
            ConfigManager.data.perspectiveEnabled = Perspective.getInstance().isActive();
            ConfigManager.save();
        });
        listView.addItem(enableToggle);

        KeybindButton keybindButton = new KeybindButton(0, 0, 260,
                "Keybind",
                ConfigManager.data.perspectiveKeyCode,
                keyCode -> {
                    ConfigManager.data.perspectiveKeyCode = keyCode;
                    ConfigManager.save();
                });
        listView.addItem(keybindButton);

        Dropdown activationModeDropdown = new Dropdown(0, 0, 260,
                "Activation Mode",
                List.of("Toggle On/Off", "Hold Key"),
                mode -> {
                    ConfigManager.data.perspectiveHoldMode = mode.equals("Hold Key");
                    ConfigManager.save();
                });
        activationModeDropdown.setSelectedIndex(ConfigManager.data.perspectiveHoldMode ? 1 : 0);
        listView.addItem(activationModeDropdown);

        Label distLabel = new Label(0, 0,
                String.format(Locale.ROOT, "Distance: %.1f", ConfigManager.data.perspectiveDistance),
                Label.Style.BODY);
        listView.addItem(distLabel);

        Slider distSlider = new Slider(0, 0, 260, 1.0f, 20.0f,
                ConfigManager.data.perspectiveDistance, val -> {
            ConfigManager.data.perspectiveDistance = val;
            distLabel.setText(String.format(Locale.ROOT, "Distance: %.1f", val));
            ConfigManager.save();
        });
        listView.addItem(distSlider);

        Label sensitivityLabel = new Label(0, 0,
                String.format(Locale.ROOT, "Sensitivity: %.1f", ConfigManager.data.perspectiveSensitivity),
                Label.Style.BODY);
        listView.addItem(sensitivityLabel);

        Slider sensitivitySlider = new Slider(0, 0, 260, 0.1f, 5.0f,
                ConfigManager.data.perspectiveSensitivity, val -> {
            ConfigManager.data.perspectiveSensitivity = val;
            sensitivityLabel.setText(String.format(Locale.ROOT, "Sensitivity: %.1f", val));
            ConfigManager.save();
        });
        listView.addItem(sensitivitySlider);

        ToggleSwitch scrollToggle = new ToggleSwitch(0, 0, 260,
                "Scroll to Change Distance",
                "Allows using the mouse wheel to change orbit distance",
                ConfigManager.data.perspectiveScrollEnabled, value -> {
            ConfigManager.data.perspectiveScrollEnabled = value;
            ConfigManager.save();
        });
        listView.addItem(scrollToggle);

        perspectiveCard.addChild(listView);
        perspectiveCard.updateLayout();
        return perspectiveCard;
    }
    private static String delayLabel(int ticks) {
        String base = "Action Delay: " + ticks + (ticks == 1 ? " tick" : " ticks");
        return ticks <= 1 ? base + " (can be broken)" : base;
    }

    private static String startDelayLabel(int ticks) {
        double seconds = ticks * 0.05;
        String base = String.format(java.util.Locale.US, "Auto Start Delay: %d ticks (%.3f seconds)", ticks, seconds);
        if (ticks <= 2) {
            base += " §c(not safe)";
        }
        return base;
    }

}
