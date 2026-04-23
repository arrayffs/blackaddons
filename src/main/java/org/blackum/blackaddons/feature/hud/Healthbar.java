package org.blackum.blackaddons.feature.hud;

import static org.blackum.blackaddons.common.util.mc.MinecraftInstance.mc;

import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.gui.hud.HudElement;
import org.blackum.blackaddons.gui.hud.HudRegistry;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

public class Healthbar implements HudElement {
    public static void register() {
        HudRegistry.register(new Healthbar());
    }

    int background_overheal_slider_fill = 0xFFfca503;
    int background_overheal_slider_outline = 0xFF96670e;

    int background_slider_fill = 0xFF6e0505;
    int background_slider_outline = 0xFF910606;

    int background_fill = 0xFF202021;
    int background_outline = 0xFF2E2E30;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker tracker) {
        if (!enabled()) return;
        if (mc.player == null) return;

        if (x() == -1 || y() == -1) {
            setPos(graphics.guiWidth() / 2 - 95, graphics.guiHeight() - 45);
        }

        graphics.fill(x(), y(), x() + width(), y() + height(), background_fill);
        AnimUtils.Outline(graphics, x(), y(), x() + width(), y() + height(), background_outline);

        float total = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        float slider_width = Math.min(total / mc.player.getMaxHealth(), 1f) * width();

        int color = background_slider_fill;
        int outline_color = background_slider_outline;

        if (total > mc.player.getMaxHealth()) {
            color = background_overheal_slider_fill;
            outline_color = background_overheal_slider_outline;
        }

        graphics.fill(x(), y(), x() + (int)slider_width, y() + height(), color);
        AnimUtils.Outline(graphics, x(), y(), x() + (int)slider_width, y() + height(),outline_color);

        graphics.drawCenteredString(mc.font, "" + total, x() + width() / 2, y() + height() / 2 - mc.font.lineHeight / 2, 0xFFFFFFFF);
    }


    public static class FeatureConfig {
        public boolean enabled = true;
        public int gui_x = -1;
        public int gui_y = -1;

        int gui_width = 90;
        int gui_height = 10;

        public FeatureConfig() {
            reset();
        }

        void reset() {
            enabled = true;
        }
    }

    //<editor-fold desc="Element Config">
    @Override
    public String id() { return "healthbar"; }

    @Override
    public String displayName() { return "Health Bar"; }

    @Override
    public boolean enabled() { return ConfigManager.data.hudOptions.healthbar.enabled; }

    @Override
    public int x() { return ConfigManager.data.hudOptions.healthbar.gui_x; }

    @Override
    public int y() { return ConfigManager.data.hudOptions.healthbar.gui_y; }

    @Override
    public void setPos(int x, int y) {
        ConfigManager.data.hudOptions.healthbar.gui_x = x;
        ConfigManager.data.hudOptions.healthbar.gui_y = y;
    }

    @Override
    public int width() { return ConfigManager.data.hudOptions.healthbar.gui_width; }

    @Override
    public int height() { return  ConfigManager.data.hudOptions.healthbar.gui_height; }

    @Override
    public boolean resizable() {
        return true;
    }

    @Override
    public void setSize(int w, int h) {
        ConfigManager.data.hudOptions.healthbar.gui_width = w;
        ConfigManager.data.hudOptions.healthbar.gui_height = h;
    }
    //</editor-fold>
}
