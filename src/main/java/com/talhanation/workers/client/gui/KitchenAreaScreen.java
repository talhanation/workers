package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.workarea.KitchenArea;
import com.talhanation.workers.network.MessageUpdateKitchenArea;
import com.talhanation.workers.network.MessageUpdateWorkAreaName;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public class KitchenAreaScreen extends WorkAreaScreen {

    private static final MutableComponent TEXT_FEED_VILLAGERS = Component.translatable("gui.workers.checkbox.feedVillagers");
    private static final MutableComponent TOOLTIP_FEED_VILLAGERS = Component.translatable("gui.workers.checkbox.tooltip.feedVillagers");
    private static final MutableComponent TEXT_CHEF            = Component.translatable("entity.workers.cook");
    private static final MutableComponent TEXT_FURNACES        = Component.translatable("gui.workers.text.furnaces");
    private static final MutableComponent TEXT_CONTAINERS      = Component.translatable("gui.workers.text.space");
    private static final MutableComponent TEXT_NO_CHEF         = Component.translatable("gui.workers.text.noCook");
    private static final MutableComponent TEXT_CHEF_PRESENT    = Component.translatable("gui.workers.text.cookPresent");
    private static final MutableComponent TEXT_NAME_HINT       = Component.translatable("entity.workers.kitchen");
    private static final MutableComponent TEXT_USE_STORAGE     = Component.translatable("gui.workers.checkbox.useStorage");
    private static final MutableComponent TOOLTIP_USE_STORAGE  = Component.translatable("gui.workers.checkbox.tooltip.useStorageKitchen");

    public final KitchenArea kitchenArea;
    private RecruitsCheckBox feedVillagersCheckBox;
    private EditBox nameEditBox;
    private String areaName;
    private boolean feedVillagers;
    private boolean useStorage;
    private String cookName;
    private int freeSlots;
    private int totalSlots;

    public KitchenAreaScreen(KitchenArea kitchenArea, Player player) {
        super(Component.translatable("entity.workers.kitchen"), kitchenArea, player);
        this.kitchenArea    = kitchenArea;
        this.feedVillagers = kitchenArea.getFeedVillagers();
        this.useStorage    = kitchenArea.getUseStorage();
        this.areaName       = kitchenArea.getCustomName() != null ? kitchenArea.getCustomName().getString() : "";
    }

    @Override
    protected void init() {
        super.init();
        this.cookName   = kitchenArea.getCookName();
        this.freeSlots  = kitchenArea.getFreeSlots();
        this.totalSlots = kitchenArea.getTotalSlots();
        setButtons();
    }

    @Override
    public void setButtons() {
        super.setButtons();

        int w  = 120;
        int h  = 20;
        int bx = x - w / 2;
        int by = y + h / 2 + 42;

        nameEditBox = new EditBox(font, bx, by - h, w, h, Component.literal(""));
        nameEditBox.setValue(areaName);
        nameEditBox.setHint(TEXT_NAME_HINT);
        nameEditBox.setTextColor(-1);
        nameEditBox.setTextColorUneditable(-1);
        nameEditBox.setBordered(true);
        nameEditBox.setMaxLength(32);
        nameEditBox.setResponder(s -> this.areaName = s);
        addRenderableWidget(nameEditBox);

        addRenderableWidget(new BlackShowingTextField(bx, by + 10, w / 2, h,
                Component.literal(TEXT_CHEF.getString() + ":")));
        addRenderableWidget(new BlackShowingTextField(bx + w / 2, by + 10, w / 2, h,
                Component.literal("" + this.cookName)));

        addRenderableWidget(new BlackShowingTextField(bx, by + 30, w / 2, h,
                Component.literal(TEXT_FURNACES.getString() + ":")));
        addRenderableWidget(new BlackShowingTextField(bx + w / 2, by + 30, w / 2, h,
                Component.literal("" + kitchenArea.getFurnaceCount())));

        addRenderableWidget(new BlackShowingTextField(bx, by + 50, w / 2, h,
                Component.literal(TEXT_CONTAINERS.getString() + ":")));
        addRenderableWidget(new BlackShowingTextField(bx + w / 2, by + 50, w / 2, h,
                Component.literal("" + this.freeSlots + " / " + this.totalSlots)));

        feedVillagersCheckBox = new RecruitsCheckBox(bx, by + 75, w, h,
                TEXT_FEED_VILLAGERS,
                feedVillagers,
                bool -> {
                    this.feedVillagers = bool;
                    sendMessage();
                });
        feedVillagersCheckBox.setTooltip(Tooltip.create(TOOLTIP_FEED_VILLAGERS));
        addRenderableWidget(feedVillagersCheckBox);

        RecruitsCheckBox useStorageCheckBox = new RecruitsCheckBox(bx, by + 97, w, h,
                TEXT_USE_STORAGE,
                useStorage,
                bool -> {
                    this.useStorage = bool;
                    sendMessage();
                });
        useStorageCheckBox.setTooltip(Tooltip.create(TOOLTIP_USE_STORAGE));
        addRenderableWidget(useStorageCheckBox);
    }

    public void sendMessage() {
        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateKitchenArea(kitchenArea.getUUID(), feedVillagers, useStorage));
    }

    @Override
    public void onClose() {
        super.onClose();
        sendMessage();
        sendName();
    }

    private void sendName() {
        String current = workArea.getCustomName() != null ? workArea.getCustomName().getString() : "";
        if (areaName.equals(current)) return;
        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateWorkAreaName(workArea.getUUID(), areaName));
    }
}
