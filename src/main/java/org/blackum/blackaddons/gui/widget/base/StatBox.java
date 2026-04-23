package org.blackum.blackaddons.gui.widget.base;

import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.render.Theme;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class StatBox extends Widget {
    private String label;
    private String value;

    public StatBox(int x, int y, int width, String label, String value) {
        super(x, y, width, 50);
        this.label = label;
        this.value = value;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        RenderHelper.renderRoundedRect(graphics, x, y, width, height, Theme.BORDER_RADIUS, Theme.BACKGROUND_SECONDARY);

        Minecraft mc = Minecraft.getInstance();
        graphics.drawCenteredString(mc.font, label, x + width / 2, y + 10, Theme.ACCENT);
        graphics.drawCenteredString(mc.font, ChatFormatting.WHITE + value, x + width / 2, y + 25, 0xFFFFFFFF);
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
