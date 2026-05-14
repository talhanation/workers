package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.workarea.KitchenArea;
import com.talhanation.workers.network.MessageUpdateKitchenArea;
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

    public final KitchenArea kitchenArea;
    private RecruitsCheckBox feedVillagersCheckBox;
    private boolean feedVillagers;

    public KitchenAreaScreen(KitchenArea kitchenArea, Player player) {
        super(Component.translatable("entity.workers.kitchen"), kitchenArea, player);
        this.kitchenArea    = kitchenArea;
        this.feedVillagers = kitchenArea.getFeedVillagers();
    }

    @Override
    protected void init() {
        super.init();
        setButtons();
    }

    @Override
    public void setButtons() {
        super.setButtons();

        int w  = 120;
        int h  = 20;
        int bx = x - w / 2;
        int by = y + h / 2 + 42;

        String chefName = kitchenArea.isBeingWorkedOn()
                ? TEXT_CHEF_PRESENT.getString()
                : TEXT_NO_CHEF.getString();

        addRenderableWidget(new BlackShowingTextField(bx, by + 10, w, h,
                Component.literal(TEXT_CHEF.getString() + ":")));

        addRenderableWidget(new BlackShowingTextField(bx + w / 2, by + 10, w / 2, h,
                Component.literal(chefName)));

        addRenderableWidget(new BlackShowingTextField(bx, by + 30, w, h,
                Component.literal(TEXT_FURNACES.getString() + ":")));

        addRenderableWidget(new BlackShowingTextField(bx + w / 2, by + 30, w / 2, h,
                Component.literal("" + kitchenArea.getFurnaceCount())));

        addRenderableWidget(new BlackShowingTextField(bx, by + 50, w, h,
                Component.literal(TEXT_CONTAINERS.getString() + ":")));
        addRenderableWidget(new BlackShowingTextField(bx + w / 2, by + 50, w / 2, h,
                Component.literal("" + kitchenArea.getContainerCount())));

        feedVillagersCheckBox = new RecruitsCheckBox(bx, by + 75, w, h,
                TEXT_FEED_VILLAGERS,
                feedVillagers,
                bool -> {
                    this.feedVillagers = bool;
                    sendMessage();
                });
        feedVillagersCheckBox.setTooltip(Tooltip.create(TOOLTIP_FEED_VILLAGERS));
        addRenderableWidget(feedVillagersCheckBox);
    }

    public void sendMessage() {
        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateKitchenArea(kitchenArea.getUUID(), feedVillagers));
    }

    @Override
    public void onClose() {
        super.onClose();
        sendMessage();
    }
}
