package org.blackum.blackaddons.gui.screen.main.tabs;


import org.blackum.blackaddons.gui.screen.main.BlackAddonsGUI;
import org.blackum.blackaddons.gui.widget.layout.TabPanel;

public abstract class SimpleTabController {
    protected final BlackAddonsGUI screen;

    public SimpleTabController(BlackAddonsGUI screen) {
        this.screen = screen;
    }

    public abstract void init(TabPanel.Tab tab);

    public void tick() {
    }

    public void onSelected() {
    }
}
