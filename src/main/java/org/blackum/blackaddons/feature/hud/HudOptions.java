package org.blackum.blackaddons.feature.hud;

public class HudOptions {
    public Hotbar.FeatureConfig hotbar = new Hotbar.FeatureConfig();
    public Healthbar.FeatureConfig healthbar = new Healthbar.FeatureConfig();

    public HudOptions() {
        Hotbar.register();
        Healthbar.register();
    }
}
