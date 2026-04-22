package org.blackum.blackaddons.feature.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.gui.hud.HudElement;
import org.blackum.blackaddons.gui.hud.HudRegistry;

import static org.blackum.blackaddons.common.util.mc.MinecraftInstance.mc;

public class Hotbar implements HudElement {
    int square_w = 20;

    int width = square_w * 9;
    int height = square_w;

    int background_fill = 0xFF202021;
    int background_outline = 0xFF2E2E30;
    int selected_background_outline = 0xFF4F4A59;
    float selected_slot = 0.f;

    public static void register() {
        HudRegistry.register(new Hotbar());
    }


    @Override
    public void render(GuiGraphics graphics, DeltaTracker tracker) {
        if (!enabled()) return;
        if (mc.player == null) return;

        if (x() == -1 || y() == -1) {
            setPos(graphics.guiWidth() / 2 - (int)(square_w * 4.5f), graphics.guiHeight() - square_w);
        }

        var inventory = mc.player.getInventory();

        graphics.fill(x(), y(), x() + width, y() + height, background_fill);
        AnimUtils.Outline(graphics, x(), y(), x() + width, y() + height, background_outline);

        for (int i = 0; i < 9; ++i) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            graphics.renderItem(stack, x() + i * square_w + 2, y() + 2);

            int stack_count = stack.getCount();
            graphics.drawCenteredString(mc.font, "" + stack_count, x() + (i + 1) * square_w - 4, y() + square_w - mc.font.lineHeight, 0xFFFFFFFF);
        }

        selected_slot = AnimUtils.Lerp(selected_slot, (float) inventory.getSelectedSlot(), tracker.getGameTimeDeltaPartialTick(true) / 20.f * 4.f);
        AnimUtils.Outline(
                graphics,
                x() + (int)(selected_slot * square_w),
                y(),
                x() + (int)((selected_slot + 1) * square_w),
                y() + square_w,
                selected_background_outline
        );
    }


    public static class FeatureConfig {
        public boolean enabled = true;
        public int gui_x = -1;
        public int gui_y = -1;

        public FeatureConfig() {
            reset();
        }

        void reset() {
            enabled = true;
        }
    }

    //<editor-fold desc="Element Config">
    @Override
    public String id() { return "hotbar"; }

    @Override
    public String displayName() { return "Hotbar"; }

    @Override
    public boolean enabled() { return ConfigManager.data.hudOptions.hotbar.enabled; }

    @Override
    public int x() { return ConfigManager.data.hudOptions.hotbar.gui_x; }

    @Override
    public int y() { return ConfigManager.data.hudOptions.hotbar.gui_y; }

    @Override
    public void setPos(int x, int y) {
        ConfigManager.data.hudOptions.hotbar.gui_x = x;
        ConfigManager.data.hudOptions.hotbar.gui_y = y;
    }

    @Override
    public int width() { return width; }

    @Override
    public int height() { return height; }
    //</editor-fold>
}
