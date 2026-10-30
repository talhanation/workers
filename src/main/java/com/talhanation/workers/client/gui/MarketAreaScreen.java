package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.workarea.MarketArea;
import com.talhanation.workers.network.MessageUpdateMarketArea;
import com.talhanation.workers.world.VillagerPriceTable;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class MarketAreaScreen extends WorkAreaScreen {

    private static final MutableComponent TEXT_OPEN   = Component.translatable("gui.workers.checkbox.marketOpen");
    private static final MutableComponent TEXT_MERCHANT = Component.translatable("entity.workers.merchant");
    private static final MutableComponent TEXT_SPACE           = Component.translatable("gui.workers.text.space");
    private static final MutableComponent TEXT_EVICT_MERCHANT  = Component.translatable("gui.workers.text.evictMerchant");
    private static final MutableComponent TEXT_USE_STORAGE     = Component.translatable("gui.workers.checkbox.useStorage");
    private static final MutableComponent TOOLTIP_USE_STORAGE  = Component.translatable("gui.workers.checkbox.tooltip.useStorageMarket");
    private static final MutableComponent TEXT_RESERVE         = Component.translatable("gui.workers.text.currencyReserve");
    private static final MutableComponent TEXT_ACCEPT_FOREIGN  = Component.translatable("gui.workers.checkbox.acceptForeign");
    private static final MutableComponent TOOLTIP_ACCEPT_FOREIGN = Component.translatable("gui.workers.checkbox.tooltip.acceptForeign");
    private static final MutableComponent TEXT_TRAVELLING      = Component.translatable("gui.workers.text.merchantTravelling");
    public final MarketArea marketArea;
    private EditBox nameEditBox;
    private RecruitsCheckBox openCheckBox;
    private String marketName;
    private boolean isOpen;
    private boolean useStorage;
    private int currencyReserve;
    private boolean acceptForeign;
    private String merchantName;
    private int freeSlots;
    private int totalSlots;
    public MarketAreaScreen(MarketArea marketArea, Player player) {
        super(Component.literal(marketArea.getMarketName()), marketArea, player);
        this.marketArea = marketArea;
        this.marketName = marketArea.getMarketName();
        this.isOpen = marketArea.isOpen();
        this.useStorage = marketArea.getUseStorage();
        this.currencyReserve = marketArea.getCurrencyReserve();
        this.acceptForeign = marketArea.getAcceptForeignMerchants();
    }

    @Override
    protected void init() {
        super.init();
        this.merchantName = marketArea.getMerchantName();
        if (marketArea.isMerchantTravelling()) this.merchantName += " (" + TEXT_TRAVELLING.getString() + ")";
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

        RecruitsCheckBox useStorageCheckBox = new RecruitsCheckBox(bx, by + 75, w, h, TEXT_USE_STORAGE, useStorage,
                bool -> {
                    this.useStorage = bool;
                    sendMessage();
                });
        useStorageCheckBox.setTooltip(Tooltip.create(TOOLTIP_USE_STORAGE));
        addRenderableWidget(useStorageCheckBox);

        // currency that always stays in the market when buying from villagers
        BlackShowingTextField reserveField = new BlackShowingTextField(bx, by + 100, w - 2 * h, h,
                Component.literal(TEXT_RESERVE.getString() + " " + currencyReserve));
        reserveField.setTooltip(Tooltip.create(Component.translatable("gui.workers.tooltip.currencyReserve", VillagerPriceTable.getCurrencyStack().getHoverName())));
        addRenderableWidget(reserveField);

        addRenderableWidget(new ExtendedButton(bx + w - 2 * h, by + 100, h, h, Component.literal("-"),
                button -> {
                    currencyReserve = Mth.clamp(currencyReserve - (hasShiftDown() ? 16 : 1), 0, MarketArea.MAX_CURRENCY_RESERVE);
                    sendMessage();
                    setButtons();
                }));
        addRenderableWidget(new ExtendedButton(bx + w - h, by + 100, h, h, Component.literal("+"),
                button -> {
                    currencyReserve = Mth.clamp(currencyReserve + (hasShiftDown() ? 16 : 1), 0, MarketArea.MAX_CURRENCY_RESERVE);
                    sendMessage();
                    setButtons();
                }));

        // travelling merchants of other players may trade here
        RecruitsCheckBox acceptForeignCheckBox = new RecruitsCheckBox(bx, by + 125, w, h, TEXT_ACCEPT_FOREIGN, acceptForeign,
                bool -> {
                    this.acceptForeign = bool;
                    sendMessage();
                });
        acceptForeignCheckBox.setTooltip(Tooltip.create(TOOLTIP_ACCEPT_FOREIGN));
        addRenderableWidget(acceptForeignCheckBox);
    }

    public void sendMessage() {
        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateMarketArea(marketArea.getUUID(), isOpen, marketName, useStorage, currencyReserve, acceptForeign));
    }

    @Override
    public void onClose() {
        super.onClose();
        sendMessage();
    }
}
