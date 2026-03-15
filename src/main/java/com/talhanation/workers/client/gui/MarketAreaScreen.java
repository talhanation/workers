package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.workarea.MarketArea;
import com.talhanation.workers.network.MessageUpdateMarketArea;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public class MarketAreaScreen extends WorkAreaScreen {

    private static final MutableComponent TEXT_OPEN   = Component.translatable("gui.workers.checkbox.marketOpen");
    private static final MutableComponent TEXT_MERCHANT = Component.translatable("entity.workers.merchant");
    private static final MutableComponent TEXT_SPACE = Component.translatable("gui.workers.text.space");
    private static final MutableComponent TEXT_NO_MERCHANT = Component.translatable("gui.workers.text.noMerchant");
    private static final MutableComponent TEXT_MERCHANT_PRESENT = Component.translatable("gui.workers.text.merchantPresent");
    public final MarketArea marketArea;
    private EditBox nameEditBox;
    private RecruitsCheckBox openCheckBox;
    private String marketName;
    private boolean isOpen;

    public MarketAreaScreen(MarketArea marketArea, Player player) {
        super(Component.literal(marketArea.getMarketName()), marketArea, player);
        this.marketArea = marketArea;
        this.marketName = marketArea.getMarketName();
        this.isOpen = marketArea.isOpen();
    }

    @Override
    protected void init() {
        super.init();
        setButtons();
    }

    @Override
    public void setButtons() {
        super.setButtons();

        int w = 120;
        int h = 20;
        int bx = x - w / 2;
        int by = y + h / 2 + 40;

        nameEditBox = new EditBox(font, bx, by - h, w, h, Component.literal(""));
        nameEditBox.setValue(marketName);
        nameEditBox.setTextColor(-1);
        nameEditBox.setTextColorUneditable(-1);
        nameEditBox.setBordered(true);
        nameEditBox.setMaxLength(32);
        nameEditBox.setResponder(s -> this.marketName = s);
        addRenderableWidget(nameEditBox);

        String merchantName = marketArea.isBeingWorkedOn()
                ? TEXT_MERCHANT_PRESENT.getString()
                : TEXT_NO_MERCHANT.getString();

        int slots = marketArea.getTotalSlots();

        addRenderableWidget(new BlackShowingTextField(bx, by + 21, w, h, Component.literal( TEXT_MERCHANT.getString() + ":")));
        addRenderableWidget(new BlackShowingTextField(bx, by + 41, w, h, Component.literal(TEXT_SPACE.getString() + ": ")));
        //addRenderableWidget(new BlackShowingTextField(bx, by + 61, w, h, Component.literal("Depth:")));
        addRenderableWidget(new BlackShowingTextField(bx + w/2, by + 21, w/2, h, Component.literal("" + merchantName)));
        addRenderableWidget(new BlackShowingTextField(bx + w/2, by + 41, w/2, h, Component.literal("" + slots)));
        //addRenderableWidget(new BlackShowingTextField(bx + w/2, by + 61, w/2, h, Component.literal("" + areaDepthSize)));



        openCheckBox = new RecruitsCheckBox(bx, by + 5, w, h,
                TEXT_OPEN,
                isOpen,
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
