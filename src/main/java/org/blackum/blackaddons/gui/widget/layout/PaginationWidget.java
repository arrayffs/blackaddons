package org.blackum.blackaddons.gui.widget.layout;

import java.util.function.Consumer;

import org.blackum.blackaddons.gui.widget.base.Button;
import org.blackum.blackaddons.gui.widget.base.Widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class PaginationWidget extends Widget {
    private final Button prev;
    private final Button next;
    private int currentPage;
    private int totalPages;
    private final Consumer<Integer> onPageChange;

    public PaginationWidget(int x, int y, int width, int currentPage, int totalPages, Consumer<Integer> onPageChange) {
        super(x, y, width, 25);
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.onPageChange = onPageChange;

        this.prev = new Button(0, 0, 80, 20, "< Prev", () -> changePage(-1));
        this.next = new Button(0, 0, 80, 20, "Next >", () -> changePage(1));
        updateButtonStates();
    }

    private void changePage(int delta) {
        int newPage = currentPage + delta;
        if (newPage >= 1 && newPage <= totalPages) {
            onPageChange.accept(newPage);
        }
    }

    public void update(int currentPage, int totalPages) {
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        updateButtonStates();
    }

    private void updateButtonStates() {
        prev.setEnabled(currentPage > 1);
        next.setEnabled(currentPage < totalPages && totalPages > 0);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        int mid = x + width / 2;
        prev.setX(mid - 120);
        prev.setY(y + 2);
        next.setX(mid + 40);
        next.setY(y + 2);

        prev.render(graphics, mouseX, mouseY, partialTick);
        next.render(graphics, mouseX, mouseY, partialTick);

        String pageStr = currentPage + " / " + (totalPages > 0 ? totalPages : "?");
        graphics.drawCenteredString(Minecraft.getInstance().font, pageStr, mid, y + 8, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled)
            return false;
        return prev.mouseClicked(mouseX, mouseY, button) || next.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return prev.mouseReleased(mouseX, mouseY, button) || next.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);
        prev.updateHoverState(mouseX, mouseY);
        next.updateHoverState(mouseX, mouseY);
    }

    @Override
    public void tick() {
        prev.tick();
        next.tick();
    }
}
