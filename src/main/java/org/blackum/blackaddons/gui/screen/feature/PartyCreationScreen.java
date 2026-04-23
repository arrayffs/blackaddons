package org.blackum.blackaddons.gui.screen.feature;


import java.util.List;

import org.blackum.blackaddons.feature.party.PartyFinderManager;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.screen.main.BaseScreen;
import org.blackum.blackaddons.gui.widget.base.Button;
import org.blackum.blackaddons.gui.widget.base.Label;
import org.blackum.blackaddons.gui.widget.input.Dropdown;

import com.google.gson.JsonObject;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PartyCreationScreen extends BaseScreen {

    private String selectedFloor = "M7";

    public PartyCreationScreen(Screen parent) {
        super(Component.literal("Create Dungeon Party"), parent);
    }

    @Override
    protected void initWidgets() {
        int innerW = Math.min(320, containerWidth - Theme.PADDING_LARGE * 2);
        int dropdownW = innerW;
        int buttonW = (innerW - Theme.PADDING) / 2;

        int titleH = 20;
        int labelH = 15;
        int rowGap = 8;
        int sectionGap = Theme.PADDING_LARGE;
        int blockH = titleH + rowGap + labelH + rowGap + Theme.BUTTON_HEIGHT + sectionGap + Theme.BUTTON_HEIGHT;

        int blockX = containerX + (containerWidth - innerW) / 2;
        int blockY = containerY + (containerHeight - blockH) / 2;

        widgets.add(new Label(blockX, blockY, "Create Party", Label.Style.TITLE));

        int labelY = blockY + titleH + rowGap;
        widgets.add(new Label(blockX, labelY, "Select Floor:", Label.Style.BODY));

        int dropdownY = labelY + labelH + rowGap;
        List<String> floors = List.of("M7", "M6", "M5", "M4", "M3", "M2", "M1", "F7", "F6", "F5", "F4", "F3", "F2",
                "F1", "Entrance");
        Dropdown floorDropdown = new Dropdown(blockX, dropdownY, dropdownW, Theme.BUTTON_HEIGHT, "Select Floor", floors,
                floor -> {
                    this.selectedFloor = floor;
                });
        floorDropdown.setSelectedOption(selectedFloor);

        int buttonsY = dropdownY + Theme.BUTTON_HEIGHT + sectionGap;
        Button createBtn = new Button(blockX, buttonsY, buttonW, Theme.BUTTON_HEIGHT, "Create", () -> {
            PartyFinderManager.getInstance().createParty(selectedFloor, new JsonObject());
            onClose();
        });
        widgets.add(createBtn);

        Button cancelBtn = new Button(blockX + buttonW + Theme.PADDING, buttonsY, buttonW, Theme.BUTTON_HEIGHT,
                "Cancel", this::onClose);
        widgets.add(cancelBtn);

        widgets.add(floorDropdown);
    }
}
