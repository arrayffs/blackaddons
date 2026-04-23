package org.blackum.blackaddons.gui.widget.input;

import java.util.function.Consumer;

import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.render.Theme;

import net.minecraft.client.gui.GuiGraphics;

public class SmallColorPicker extends ColorPicker {
    public static final int S_WIDTH = 100;
    public static final int S_HEIGHT = 100;

    public SmallColorPicker(int x, int y, int initialColor, Consumer<Integer> onColorChange) {
        super(x, y, initialColor, onColorChange);
        this.width = S_WIDTH;
        this.height = S_HEIGHT;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public void setWidth(int width) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        RenderHelper.renderSurface(graphics, x, y, width, height, Theme.BORDER_RADIUS_SMALL, false);

        int padding = 5;
        int innerX = x + padding;
        int sbWidth = width - (padding * 2);
        int sbHeight = (int) (height * 0.6f);
        int currentY = y + padding;

        renderSBArea(graphics, innerX, currentY, sbWidth, sbHeight);
        currentY += sbHeight + padding;

        int sliderHeight = Math.max(4, (int) (height * 0.06f));
        int spacing = Math.max(4, (int) (height * 0.1f));

        renderHueSlider(graphics, innerX, currentY, sbWidth, sliderHeight);
        currentY += spacing;

        renderAlphaSlider(graphics, innerX, currentY, sbWidth, sliderHeight);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;

        int padding = 5;
        int innerX = x + padding;
        int sbWidth = width - (padding * 2);
        int sbHeight = (int) (height * 0.6f);
        int sbY = y + padding;

        if (mouseX >= innerX && mouseX <= innerX + sbWidth && mouseY >= sbY && mouseY <= sbY + sbHeight) {
            draggingSB = true;
            updateSB(mouseX, mouseY, innerX, sbY, sbWidth, sbHeight);
            return true;
        }

        int sliderHeight = Math.max(4, (int) (height * 0.06f));
        int spacing = Math.max(4, (int) (height * 0.1f));

        int hueY = sbY + sbHeight + padding;
        if (mouseX >= innerX && mouseX <= innerX + sbWidth && mouseY >= hueY && mouseY <= hueY + sliderHeight) {
            draggingHue = true;
            updateHue(mouseX, innerX, sbWidth);
            return true;
        }

        int alphaY = hueY + spacing;
        if (mouseX >= innerX && mouseX <= innerX + sbWidth && mouseY >= alphaY && mouseY <= alphaY + sliderHeight) {
            draggingAlpha = true;
            updateAlpha(mouseX, innerX, sbWidth);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int padding = 5;
        int innerX = x + padding;
        int sbWidth = width - (padding * 2);
        int sbHeight = (int) (height * 0.6f);
        int sbY = y + padding;

        if (draggingSB) {
            updateSB(mouseX, mouseY, innerX, sbY, sbWidth, sbHeight);
            return true;
        }
        if (draggingHue) {
            updateHue(mouseX, innerX, sbWidth);
            return true;
        }
        if (draggingAlpha) {
            updateAlpha(mouseX, innerX, sbWidth);
            return true;
        }
        return false;
    }

    private void updateHue(double mouseX, int innerX, int sbWidth) {
        hull = (float) ((mouseX - innerX) / sbWidth);
        hull = Math.max(0f, Math.min(1f, hull));
        notifyChange();
    }

    private void updateSB(double mouseX, double mouseY, int innerX, int sbY, int width, int height) {
        saturation = (float) ((mouseX - innerX) / width);
        brightness = 1f - (float) ((mouseY - sbY) / height);
        saturation = Math.max(0f, Math.min(1f, saturation));
        brightness = Math.max(0f, Math.min(1f, brightness));
        notifyChange();
    }

    private void updateAlpha(double mouseX, int innerX, int sbWidth) {
        alpha = (float) ((mouseX - innerX) / sbWidth);
        alpha = Math.max(0f, Math.min(1f, alpha));
        notifyChange();
    }
}
