package org.blackum.blackaddons.gui.screen.overlay;


import org.blackum.blackaddons.gui.screen.main.BaseScreen;
import org.blackum.blackaddons.gui.screen.main.BlackAddonsGUI;
import org.blackum.blackaddons.gui.screen.feature.ModOrganizer;
import org.blackum.blackaddons.gui.screen.feature.WaypointEditScreen;
import org.blackum.blackaddons.gui.screen.feature.WaypointGroupEditScreen;
import org.blackum.blackaddons.gui.screen.feature.WaypointActionEditScreen;
import org.blackum.blackaddons.gui.screen.feature.ChatActionEditScreen;
import org.blackum.blackaddons.gui.screen.feature.IrcScreen;
import org.blackum.blackaddons.gui.screen.feature.ImagePreviewScreen;
import org.blackum.blackaddons.gui.screen.feature.ProfileViewerScreen;
import org.blackum.blackaddons.gui.screen.feature.PartyFinderScreen;
import org.blackum.blackaddons.gui.screen.feature.PartyCreationScreen;
import org.blackum.blackaddons.gui.screen.feature.SoloLeaderboardScreen;
import org.blackum.blackaddons.gui.screen.debug.DemoScreen;
import org.blackum.blackaddons.gui.screen.debug.TestMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.gui.hud.HudElement;
import org.blackum.blackaddons.gui.hud.HudRegistry;

public class OverlayEditScreen extends Screen {
    private static final int HANDLE_SIZE = 8;
    private static final int OUTLINE_IDLE = 0x80FFFFFF;
    private static final int OUTLINE_SELECTED = 0xFF00FF00;
    private static final int HANDLE_COLOR = 0xFFFFFFFF;
    private static final int HINT_COLOR = 0xFFAAAAAA;

    private final Screen parent;
    private final String preselectId;
    private HudElement selected;
    private boolean dragging;
    private boolean resizing;
    private double dragOffsetX;
    private double dragOffsetY;

    public OverlayEditScreen(Screen parent) {
        this(parent, null);
    }

    public OverlayEditScreen(Screen parent, String preselectId) {
        super(Component.literal("HUD Editor"));
        this.parent = parent;
        this.preselectId = preselectId;
    }

    @Override
    public void init() {
        super.init();
        if (preselectId != null && selected == null) {
            HudElement e = HudRegistry.get(preselectId);
            if (e != null && e.enabled()) selected = e;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0x80000000);

        g.drawCenteredString(font, "Drag to move | Drag corner to resize | Scroll on selected to scale | Right-click to reset | Esc to save",
                this.width / 2, 8, HINT_COLOR);
        if (selected != null) {
            g.drawCenteredString(font, "Selected: " + selected.displayName()
                    + "  @" + selected.x() + "," + selected.y()
                    + "  " + selected.width() + "x" + selected.height(),
                    this.width / 2, 22, 0xFFFFFFFF);
        }

        for (HudElement e : HudRegistry.all()) {
            if (!e.enabled()) continue;
            int ex = e.x();
            int ey = e.y();
            int ew = e.width();
            int eh = e.height();
            int color = e == selected ? OUTLINE_SELECTED : OUTLINE_IDLE;
            drawOutline(g, ex, ey, ew, eh, color);

            int labelY = ey - font.lineHeight - 1;
            if (labelY < 0) labelY = ey + 1;
            g.drawString(font, e.displayName(), ex + 2, labelY, color | 0xFF000000);

            if (e.resizable() && e == selected) {
                int hx = ex + ew - HANDLE_SIZE;
                int hy = ey + eh - HANDLE_SIZE;
                g.fill(hx, hy, ex + ew, ey + eh, HANDLE_COLOR);
            }
        }
    }

    private static void drawOutline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private double mx() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.xpos() * ((double) this.width / mc.getWindow().getScreenWidth());
    }

    private double my() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.ypos() * ((double) this.height / mc.getWindow().getScreenHeight());
    }

    private HudElement hitTest(double x, double y) {
        HudElement hit = null;
        for (HudElement e : HudRegistry.all()) {
            if (!e.enabled()) continue;
            int ex = e.x();
            int ey = e.y();
            int ew = e.width();
            int eh = e.height();
            if (x >= ex && x <= ex + ew && y >= ey && y <= ey + eh) {
                hit = e;
            }
        }
        return hit;
    }

    private boolean isOverHandle(HudElement e, double x, double y) {
        if (e == null || !e.resizable()) return false;
        int hx = e.x() + e.width() - HANDLE_SIZE;
        int hy = e.y() + e.height() - HANDLE_SIZE;
        return x >= hx && x <= e.x() + e.width() && y >= hy && y <= e.y() + e.height();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean pressed) {
        double x = mx();
        double y = my();
        int button = event.button();

        if (button == 1) {
            HudElement hit = hitTest(x, y);
            if (hit != null) {
                hit.reset();
                if (selected == hit) selected = hit;
                return true;
            }
        }

        if (button == 0) {
            if (isOverHandle(selected, x, y)) {
                resizing = true;
                return true;
            }
            HudElement hit = hitTest(x, y);
            if (hit != null) {
                selected = hit;
                dragging = true;
                dragOffsetX = x - hit.x();
                dragOffsetY = y - hit.y();
                return true;
            }
            selected = null;
        }

        return super.mouseClicked(event, pressed);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        resizing = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (selected == null) return super.mouseDragged(event, dragX, dragY);
        double x = mx();
        double y = my();

        if (dragging) {
            int nx = (int) Math.max(0, Math.min(this.width - selected.width(), x - dragOffsetX));
            int ny = (int) Math.max(0, Math.min(this.height - selected.height(), y - dragOffsetY));
            selected.setPos(nx, ny);
            return true;
        }
        if (resizing) {
            int nw = (int) Math.max(16, x - selected.x());
            int nh = (int) Math.max(16, y - selected.y());
            selected.setSize(nw, nh);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (selected != null && selected.resizable()) {
            int step = (int) Math.round(verticalAmount * 8);
            int nw = Math.max(1, selected.width() + step);
            int nh = Math.max(1, selected.height() + step);
            selected.setSize(nw, nh);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        Minecraft.getInstance().setScreen(parent);
    }
}
