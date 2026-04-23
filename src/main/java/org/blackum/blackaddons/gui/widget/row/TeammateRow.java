package org.blackum.blackaddons.gui.widget.row;

import org.blackum.blackaddons.common.constants.Constants;
import org.blackum.blackaddons.common.model.Teammate;
import org.blackum.blackaddons.common.util.format.FormatUtils;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.widget.base.Button;
import org.blackum.blackaddons.gui.widget.base.Widget;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class TeammateRow extends Widget {
    private final Teammate tm;
    private Animation hoverAnimation;
    private final Button inviteBtn;

    public static final int COL_IGN = 90;
    public static final int COL_RUNS = 40;
    public static final int COL_CLASS = 80;
    public static final int COL_FLOOR = 60;

    public TeammateRow(int width, Teammate tm) {
        super(0, 0, width, 18);
        this.tm = tm;
        this.hoverAnimation = new Animation(0, 1, Theme.ANIM_HOVER, Easing::easeOut);
        this.inviteBtn = new Button(0, 0, 40, 12, "Invite", () -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.connection.sendCommand("party " + tm.ign);
            }
        });
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);
        inviteBtn.setX(this.x + this.width - 45);
        inviteBtn.setY(this.y + 3);
        inviteBtn.updateHoverState(mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        float hover = hoverAnimation.getValue();
        if (hover > 0) {
            int color = Theme.withAlpha(Theme.GLASS_HIGHLIGHT, hover * 0.2f);
            graphics.fill(x, y, x + width, y + height, color);
        }

        int cx = x + 2;
        int cy = y + 5;

        graphics.drawString(Minecraft.getInstance().font, tm.ign, cx, cy, Theme.ACCENT);
        cx += COL_IGN;
        graphics.drawString(Minecraft.getInstance().font, ChatFormatting.WHITE + String.valueOf(tm.count), cx, cy,
                0xFFFFFFFF);
        cx += COL_RUNS;

        int classColor = 0xFFFFFFFF;
        switch (tm.lastClass.toLowerCase()) {
            case "archer" -> classColor = 0xFFFFAA00;
            case "berserk" -> classColor = 0xFFFF5555;
            case "healer" -> classColor = 0xFFFF55FF;
            case "mage" -> classColor = 0xFF55FFFF;
            case "tank" -> classColor = 0xFF55FF55;
        }

        graphics.drawString(Minecraft.getInstance().font, tm.lastClass, cx, cy, classColor);
        int classWidth = Minecraft.getInstance().font.width(tm.lastClass);

        if (tm.lastClassLevel >= 0) {
            int lvlColor = 0xFFAAAAAA;
            if (tm.lastClassLevel == 50) {
                long time = System.currentTimeMillis() / 10;
                float hue = (time % 1000) / 1000f;
                lvlColor = java.awt.Color.HSBtoRGB(hue, 0.7f, 1f);
            } else if (tm.lastClassLevel >= 40) {
                lvlColor = 0xFFFF55FF;
            } else if (tm.lastClassLevel >= 30) {
                lvlColor = 0xFF55FFFF;
            } else if (tm.lastClassLevel >= 20) {
                lvlColor = 0xFF55FF55;
            } else if (tm.lastClassLevel >= 10) {
                lvlColor = 0xFFFFFFFF;
            }
            graphics.drawString(Minecraft.getInstance().font, " " + tm.lastClassLevel, cx + classWidth, cy, lvlColor);
        }
        cx += COL_CLASS;

        int floorColor = 0xFFFFFFFF;
        if (tm.lastFloor.equalsIgnoreCase("Entrance")) {
            floorColor = 0xFFBBBBBB;
        } else if (tm.lastFloor.startsWith("M")) {
            floorColor = 0xFFD35400;
        } else if (tm.lastFloor.startsWith("F")) {
            floorColor = 0xFF9B59B6;
        } else {
            switch (tm.lastFloor) {
                case "Hot" -> floorColor = 0xFFFFAA00;
                case "Burning" -> floorColor = 0xFFFFFF55;
                case "Fiery" -> floorColor = 0xFFFF5555;
                case "Infernal" -> floorColor = 0xFFAA0000;
            }
        }
        graphics.drawString(Minecraft.getInstance().font, tm.lastFloor, cx, cy, floorColor);
        cx += COL_FLOOR;

        String timeAgo = FormatUtils.formatRelativeTime(tm.lastTs);
        graphics.drawString(Minecraft.getInstance().font, ChatFormatting.GRAY + timeAgo, cx, cy, 0xFFFFFFFF);

        inviteBtn.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        inviteBtn.tick();
        if (hovered && hoverAnimation.getProgress() < 1
                && (!hoverAnimation.isRunning() || hoverAnimation.getValue() < 1)) {
            hoverAnimation = new Animation(hoverAnimation.getValue(), 1, Theme.ANIM_HOVER, Easing::easeOut);
            hoverAnimation.start();
        } else if (!hovered && hoverAnimation.getProgress() > 0
                && (!hoverAnimation.isRunning() || hoverAnimation.getValue() > 0)) {
            hoverAnimation = new Animation(hoverAnimation.getValue(), 0, Theme.ANIM_HOVER, Easing::easeOut);
            hoverAnimation.start();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled)
            return false;
        if (inviteBtn.mouseClicked(mouseX, mouseY, button))
            return true;

        if (isMouseOver(mouseX, mouseY) && button == 0) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.connection.sendCommand(Constants.BASE_COMMAND + " pv " + tm.ign);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return inviteBtn.mouseReleased(mouseX, mouseY, button);
    }
}
