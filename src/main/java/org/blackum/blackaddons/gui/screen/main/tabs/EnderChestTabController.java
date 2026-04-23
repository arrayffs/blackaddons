package org.blackum.blackaddons.gui.screen.main.tabs;


import java.util.ArrayList;
import java.util.List;

import org.blackum.blackaddons.common.constants.Constants;
import org.blackum.blackaddons.common.model.SkyblockItem;
import org.blackum.blackaddons.feature.item.ItemDeserializer;
import org.blackum.blackaddons.gui.screen.feature.ProfileViewerScreen;
import org.blackum.blackaddons.gui.widget.base.Button;
import org.blackum.blackaddons.gui.widget.base.ItemGridWidget;
import org.blackum.blackaddons.gui.widget.base.Label;
import org.blackum.blackaddons.gui.widget.base.Widget;
import org.blackum.blackaddons.gui.widget.layout.ListView;
import org.blackum.blackaddons.gui.widget.layout.TabPanel;

import com.google.gson.JsonObject;

public class EnderChestTabController extends ProfileTabController {
    private ListView listView;
    private final List<Integer> pageOffsets = new ArrayList<>();

    public EnderChestTabController(ProfileViewerScreen screen, JsonObject profileData) {
        super(screen, profileData);
    }

    @Override
    public void init(TabPanel.Tab tab) {
        int startX = tab.getParent().getContentX();
        int startY = tab.getParent().getContentY();
        int width = tab.getParent().getContentWidth();
        int height = tab.getParent().getContentHeight();

        JsonObject inventory = profileData.getAsJsonObject("inventory");
        if (inventory == null || !inventory.has("ender_chest_contents")) {
            tab.addWidget(
                    new Label(startX + width / 2 - 50, startY + height / 2, "No Ender Chest data", Label.Style.TITLE));
            return;
        }

        JsonObject ecObj = inventory.getAsJsonObject("ender_chest_contents");
        List<SkyblockItem> allItems;
        if (ecObj.has("data")) {
            allItems = ItemDeserializer.deserializeList(ecObj.get("data").getAsString());
        } else if (ecObj.has("skycrypt_items") && ecObj.get("skycrypt_items").isJsonArray()) {
            allItems = ItemDeserializer.deserializeSkyCryptItems(ecObj.getAsJsonArray("skycrypt_items"));
        } else {
            tab.addWidget(
                    new Label(startX + width / 2 - 50, startY + height / 2, "No Ender Chest data", Label.Style.TITLE));
            return;
        }

        if (allItems.isEmpty()) {
            tab.addWidget(
                    new Label(startX + width / 2 - 50, startY + height / 2, "Ender Chest is empty", Label.Style.TITLE));
            return;
        }

        int btnWidth = 30;
        int btnHeight = 20;
        int btnGap = 5;
        int maxPages = (int) Math.ceil(allItems.size() / (double) Constants.ENDER_CHEST_PAGE_SLOTS);
        int totalBtnWidth = maxPages * btnWidth + (maxPages - 1) * btnGap;
        int btnX = startX + (width - totalBtnWidth) / 2;

        for (int i = 0; i < maxPages; i++) {
            final int pageIdx = i;
            Button btn = new Button(btnX, startY, btnWidth, btnHeight, String.valueOf(i + 1), () -> {
                if (listView != null && pageIdx < pageOffsets.size()) {
                    listView.scrollTo(pageOffsets.get(pageIdx));
                }
            });
            tab.addWidget(btn);
            btnX += btnWidth + btnGap;
        }

        listView = new ListView(startX, startY + btnHeight + 10, width, height - btnHeight - 10);
        tab.addWidget(listView);

        pageOffsets.clear();
        int currentOffset = 0;

        for (int i = 0; i < maxPages; i++) {
            int start = i * Constants.ENDER_CHEST_PAGE_SLOTS;
            int end = Math.min(start + Constants.ENDER_CHEST_PAGE_SLOTS, allItems.size());
            List<SkyblockItem> pageItems = new ArrayList<>(allItems.subList(start, end));
            while (pageItems.size() < Constants.ENDER_CHEST_PAGE_SLOTS) {
                pageItems.add(new SkyblockItem(net.minecraft.world.item.ItemStack.EMPTY, "EMPTY", "COMMON"));
            }

            pageOffsets.add(currentOffset);

            Label pageLabel = new Label(0, 0, "Page " + (i + 1), Label.Style.TITLE);
            listView.addItem(pageLabel);
            currentOffset += pageLabel.getHeight() + listView.getItemSpacing();

            ItemGridWidget grid = new ItemGridWidget(0, 0, 9, pageItems);
            listView.addItem(grid);
            currentOffset += grid.getHeight() + listView.getItemSpacing();

            listView.addItem(new Widget(0, 0, 0, 20) {
                @Override
                public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                        float partialTick) {
                }
            });
            currentOffset += 20 + listView.getItemSpacing();
        }
    }
}
