package org.blackum.blackaddons.gui.widget.input;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.render.Theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class AutocompleteTextField extends TextField {
    private final Supplier<List<String>> suggestionProvider;
    private List<String> currentSuggestions = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private boolean showSuggestions = false;
    private final int MAX_VISIBLE_SUGGESTIONS = 7;
    private int lastMouseX = -1;
    private int lastMouseY = -1;
    private java.util.function.Consumer<String> onSelect;

    public AutocompleteTextField(int x, int y, int width, int height, String placeholder,
            Supplier<List<String>> suggestionProvider) {
        super(x, y, width, height, placeholder);
        this.suggestionProvider = suggestionProvider;
    }

    public void setOnSelect(java.util.function.Consumer<String> onSelect) {
        this.onSelect = onSelect;
    }

    private void confirmSelection(String value) {
        showSuggestions = false;
        if (onSelect != null) onSelect.accept(value);
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        updateSuggestions(text);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        boolean result = super.charTyped(character, modifiers);
        if (result) {
            updateSuggestions(getText());
        }
        return result;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showSuggestions && isFocused()) {
            if (keyCode == 264) { // Down
                selectedIndex = Math.min(selectedIndex + 1, currentSuggestions.size() - 1);
                ensureVisible(selectedIndex);
                return true;
            } else if (keyCode == 265) { // Up
                selectedIndex = Math.max(selectedIndex - 1, 0);
                ensureVisible(selectedIndex);
                return true;
            } else if (keyCode == 257 || keyCode == 335) { // Enter
                if (selectedIndex >= 0 && selectedIndex < currentSuggestions.size()) {
                    String selected = currentSuggestions.get(selectedIndex);
                    setText(selected);
                    confirmSelection(selected);
                    return true;
                }
            } else if (keyCode == 258) { // Tab
                if (!currentSuggestions.isEmpty()) {
                    String selected = currentSuggestions.get(0);
                    setText(selected);
                    confirmSelection(selected);
                    return true;
                }
            } else if (keyCode == 256) { // Escape
                showSuggestions = false;
                return true;
            }
        }
        boolean result = super.keyPressed(keyCode, scanCode, modifiers);
        if (result && (keyCode == 259 || keyCode == 261 || (modifiers & 2) != 0)) { // Backspace, Delete, or Ctrl+V
            updateSuggestions(getText());
        }
        return result;
    }

    private void ensureVisible(int index) {
        if (index < scrollOffset) {
            scrollOffset = index;
        } else if (index >= scrollOffset + MAX_VISIBLE_SUGGESTIONS) {
            scrollOffset = index - MAX_VISIBLE_SUGGESTIONS + 1;
        }
    }

    public void refreshSuggestions() {
        updateSuggestions(getText());
    }

    private void updateSuggestions(String query) {
        if (!isFocused()) {
            showSuggestions = false;
            return;
        }

        List<String> all = suggestionProvider.get();
        currentSuggestions.clear();
        scrollOffset = 0;

        if (query.isEmpty()) {
            for (String s : all) {
                currentSuggestions.add(s);
            }
        } else {
            String q = query.toLowerCase();
            List<String> startsWith = new ArrayList<>();
            List<String> contains = new ArrayList<>();

            for (String s : all) {
                if (s.equalsIgnoreCase(query)) continue;
                String lowerS = s.toLowerCase();
                if (lowerS.startsWith(q)) {
                    startsWith.add(s);
                } else if (lowerS.contains(q)) {
                    contains.add(s);
                }
            }

            currentSuggestions.addAll(startsWith);
            currentSuggestions.addAll(contains);
        }

        selectedIndex = currentSuggestions.isEmpty() ? -1 : 0;
        showSuggestions = !currentSuggestions.isEmpty();
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            showSuggestions = false;
        } else {
            updateSuggestions(getText());
        }
    }

    @Override
    public boolean hasActiveOverlay() {
        return showSuggestions && !currentSuggestions.isEmpty();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showSuggestions) {
            int suggestionHeight = 12;
            int maxItems = Math.min(currentSuggestions.size(), MAX_VISIBLE_SUGGESTIONS);
            int totalHeight = maxItems * suggestionHeight + 4;
            if (mouseX >= x && mouseX <= x + width && mouseY >= y + height && mouseY <= y + height + totalHeight) {
                int index = (int) ((mouseY - (y + height + 2)) / suggestionHeight);
                int actualIndex = scrollOffset + index;
                if (actualIndex >= 0 && actualIndex < currentSuggestions.size()) {
                    String selected = currentSuggestions.get(actualIndex);
                    setText(selected);
                    confirmSelection(selected);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (showSuggestions && !currentSuggestions.isEmpty()) {
            int suggestionHeight = 12;
            int maxItems = Math.min(currentSuggestions.size(), MAX_VISIBLE_SUGGESTIONS);
            int totalHeight = maxItems * suggestionHeight + 4;
            if (mouseX >= x && mouseX <= x + width && mouseY >= y + height && mouseY <= y + height + totalHeight) {
                if (scrollY > 0) {
                    scrollOffset = Math.max(0, scrollOffset - 1);
                } else if (scrollY < 0) {
                    scrollOffset = Math.min(Math.max(0, currentSuggestions.size() - maxItems), scrollOffset + 1);
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, int rawMouseX, int rawMouseY,
            float partialTick) {
        if (!showSuggestions || !visible || currentSuggestions.isEmpty())
            return;

        int suggestionHeight = 12;
        int maxItems = Math.min(currentSuggestions.size(), MAX_VISIBLE_SUGGESTIONS);
        int totalHeight = maxItems * suggestionHeight + 4;
        int overlayY = y + height;

        RenderHelper.renderRoundedRect(graphics, x, overlayY, width, totalHeight, Theme.BORDER_RADIUS_SMALL,
                Theme.GLASS_FILL);
        RenderHelper.renderRoundedOutline(graphics, x, overlayY, width, totalHeight, Theme.BORDER_RADIUS_SMALL,
                Theme.GLASS_BORDER);

        for (int i = 0; i < maxItems; i++) {
            int actualIndex = scrollOffset + i;
            if (actualIndex >= currentSuggestions.size()) break;

            int itemY = overlayY + 2 + (i * suggestionHeight);
            String suggestion = currentSuggestions.get(actualIndex);

            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= itemY
                    && mouseY < itemY + suggestionHeight;

            if (hovered && (mouseX != lastMouseX || mouseY != lastMouseY)) {
                selectedIndex = actualIndex;
            }

            if (actualIndex == selectedIndex) {
                graphics.fill(x + 1, itemY, x + width - 1, itemY + suggestionHeight,
                        Theme.withAlpha(Theme.ACCENT, 0.3f));
            }

            graphics.drawString(Minecraft.getInstance().font, suggestion, x + 4, itemY + (suggestionHeight - 8) / 2,
                    Theme.TEXT_PRIMARY);
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }
}
