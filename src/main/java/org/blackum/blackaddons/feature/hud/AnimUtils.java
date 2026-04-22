package org.blackum.blackaddons.feature.hud;

import net.minecraft.client.gui.GuiGraphics;

public class AnimUtils {
    public static float Lerp(float a, float b, float t) { return (a + (b - a) * t); }


    public static void Outline(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.hLine(x1, x2, y1, color);
        graphics.hLine(x1, x2, y2, color);

        graphics.vLine(x1, y1, y2, color);
        graphics.vLine(x2, y1, y2, color);
    }
}
