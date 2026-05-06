package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.workarea.MarketArea;
import com.talhanation.workers.network.MessageUpdateMarketArea;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public class MarketAreaScreen extends WorkAreaScreen {

    private static final MutableComponent TEXT_OPEN   = Component.translatable("gui.workers.checkbox.marketOpen");
    private static final MutableComponent TEXT_MERCHANT = Component.translatable("entity.workers.merchant");
    private static final MutableComponent TEXT_SPACE = Component.translatable("gui.workers.text.space");
    public final MarketArea marketArea;
    private EditBox nameEditBox;
    private RecruitsCheckBox openCheckBox;
    private String marketName;
    private boolean isOpen;
    private String merchantName;
    private int freeSlots;
    private int totalSlots;
    public MarketAreaScreen(MarketArea marketArea, Player player) {
        super(Component.literal(marketArea.getMarketName()), marketArea, player);
        this.marketArea = marketArea;
        this.marketName = marketArea.getMarketName();
        this.isOpen = marketArea.isOpen();
    }

    @Override
    protected void init() {
        super.init();
        this.merchantName = marketArea.getMerchantName();
        freeSlots = marketArea.getFreeSlots();
        totalSlots = marketArea.getTotalSlots();
        setButtons();
    }

    @Override
    public void setButtons() {
        super.setButtons();

        int w = 120;
        int h = 20;
        int bx = x - w / 2;
        int by = y + h / 2 + 42;

        nameEditBox = new EditBox(font, bx, by - h, w, h, Component.literal(""));
        nameEditBox.setValue(marketName);
        nameEditBox.setTextColor(-1);
        nameEditBox.setTextColorUneditable(-1);
        nameEditBox.setBordered(true);
        nameEditBox.setMaxLength(32);
        nameEditBox.setResponder(s -> this.marketName = s);
        addRenderableWidget(nameEditBox);

        addRenderableWidget(new BlackShowingTextField(bx, by + 30, w, h, Component.literal( TEXT_MERCHANT.getString() + ":")));
        addRenderableWidget(new BlackShowingTextField(bx, by + 50, w, h, Component.literal(TEXT_SPACE.getString() + ": ")));

        addRenderableWidget(new BlackShowingTextField(bx + w/2, by + 30, w/2, h, Component.literal("" + this.merchantName)));
        addRenderableWidget(new BlackShowingTextField(bx + w/2, by + 50, w/2, h, Component.literal("" + this.freeSlots + " / " + this.totalSlots)));

        openCheckBox = new RecruitsCheckBox(bx, by + 7, w, h, TEXT_OPEN, isOpen,
                bool -> {
                    this.isOpen = bool;
                    sendMessage();
                });
        addRenderableWidget(openCheckBox);
    }

    public void sendMessage() {
        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateMarketArea(marketArea.getUUID(), isOpen, marketName));
    }

    @Override
    public void onClose() {
        super.onClose();
        sendMessage();
    }
}
