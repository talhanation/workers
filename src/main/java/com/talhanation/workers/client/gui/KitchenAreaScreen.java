package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.workarea.KitchenArea;
import com.talhanation.workers.network.MessageUpdateKitchenArea;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public class KitchenAreaScreen extends WorkAreaScreen {

    private static final MutableComponent TEXT_SELL_VILLAGERS  = Component.translatable("gui.workers.checkbox.kitchenSellVillagers");
    private static final MutableComponent TEXT_CHEF            = Component.translatable("entity.workers.cook");
    private static final MutableComponent TEXT_FURNACES        = Component.translatable("gui.workers.text.furnaces");
    private static final MutableComponent TEXT_CONTAINERS      = Component.translatable("gui.workers.text.containers");
    private static final MutableComponent TEXT_NO_CHEF         = Component.translatable("gui.workers.text.noChef");
    private static final MutableComponent TEXT_CHEF_PRESENT    = Component.translatable("gui.workers.text.chefPresent");

    public final KitchenArea kitchenArea;
    private RecruitsCheckBox sellToVillagersCheckBox;
    private boolean sellToVillagers;

    public KitchenAreaScreen(KitchenArea kitchenArea, Player player) {
        super(Component.translatable("gui.workers.kitchenArea"), kitchenArea, player);
        this.kitchenArea    = kitchenArea;
        this.sellToVillagers = kitchenArea.isSellToVillagers();
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

        sellToVillagersCheckBox = new RecruitsCheckBox(bx, by + 75, w, h,
                TEXT_SELL_VILLAGERS,
                sellToVillagers,
                bool -> {
                    this.sellToVillagers = bool;
                    sendMessage();
                });
        addRenderableWidget(sellToVillagersCheckBox);
    }

    public void sendMessage() {
        WorkersMain.SIMPLE_CHANNEL.sendToServer(
                new MessageUpdateKitchenArea(kitchenArea.getUUID(), sellToVillagers));
    }

    @Override
    public void onClose() {
        super.onClose();
        sendMessage();
    }
}
