package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.client.WorkersClientManager;
import com.talhanation.workers.client.gui.widgets.ItemSearchWidget;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.inventory.MerchantAddEditVillagerTradeContainer;
import com.talhanation.workers.network.MessageSetGhostItem;
import com.talhanation.workers.network.MessageUpdateMerchantTrade;
import com.talhanation.workers.world.VillagerPriceTable;
import com.talhanation.workers.world.WorkersMerchantTrade;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import javax.annotation.Nullable;

public class MerchantAddEditVillagerTradeScreen extends ScreenBase<MerchantAddEditVillagerTradeContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(WorkersMain.MOD_ID, "textures/gui/merchant_add_edit_villager_trade_screen.png");
    private static final MutableComponent BUTTON_CANCEL  = Component.translatable("gui.workers.button.cancel");
    private static final MutableComponent BUTTON_SAVE    = Component.translatable("gui.workers.button.save");
    private static final MutableComponent TEXT_ENABLED   = Component.translatable("gui.workers.checkbox.enabled");
    private static final MutableComponent BUTTON_MODE_SELL = Component.translatable("gui.workers.button.villagerSell");
    private static final MutableComponent BUTTON_MODE_BUY  = Component.translatable("gui.workers.button.villagerBuy");
    private static final MutableComponent TOOLTIP_MODE     = Component.translatable("gui.workers.tooltip.villagerTradeMode");
    private static final MutableComponent TEXT_SELL_ITEM   = Component.translatable("gui.workers.text.villagerSellItem");
    private static final MutableComponent TEXT_BUY_ITEM    = Component.translatable("gui.workers.text.villagerBuyItem");
    private static final MutableComponent TEXT_KEEP        = Component.translatable("gui.workers.text.storageKeep");
    private static final MutableComponent TEXT_BUY_UNTIL   = Component.translatable("gui.workers.text.storageBuyUntil");
    private static final MutableComponent TOOLTIP_KEEP     = Component.translatable("gui.workers.tooltip.villagerKeep");
    private static final MutableComponent TOOLTIP_BUY_UNTIL = Component.translatable("gui.workers.tooltip.storageBuyUntil");
    private static final MutableComponent TEXT_NO_VILLAGER_BUYS  = Component.translatable("gui.workers.text.noVillagerBuys");
    private static final MutableComponent TEXT_NO_VILLAGER_SELLS = Component.translatable("gui.workers.text.noVillagerSells");
    private static final MutableComponent TOOLTIP_SEARCH_PICK = Component.translatable("gui.workers.search.tooltip.pick");
    private static final int fontColor = 4210752;
    private static final int infoColor = 0xFFFFFF;
    private static final int noRateColor = 0xFF5555;
    private static final int MAX_AMOUNT = 9999;
    // box with the vanilla price, right of the texture above the mode button
    private static final int RATE_X = 177;
    private static final int RATE_Y = 14;
    private static final int RATE_WIDTH = 140;
    private static final int RATE_HEIGHT = 38;

    private final MerchantEntity merchantEntity;
    private final Player player;
    private final MerchantAddEditVillagerTradeContainer tradeContainer;
    public WorkersMerchantTrade trade;

    private ExtendedButton saveButton;
    @Nullable private ItemSearchWidget search;

    private boolean enabled;
    private WorkersMerchantTrade.VillagerTradeMode mode;
    private int amount;

    public MerchantAddEditVillagerTradeScreen(MerchantAddEditVillagerTradeContainer tradeContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, tradeContainer, playerInventory, Component.literal("Add or Edit Villager Trade"));
        this.tradeContainer  = tradeContainer;
        this.merchantEntity  = tradeContainer.getMerchantEntity();
        this.player          = playerInventory.player;
        this.trade           = tradeContainer.getTrade();
        imageWidth  = 176;
        imageHeight = 223;
    }

    @Override
    protected void init() {
        super.init();
        this.enabled = this.trade.enabled;
        this.mode    = this.trade.villagerMode;
        this.amount  = this.trade.storageAmount;

        // item search left of the texture, keeps its text when the screen is resized
        int searchX = leftPos - 6 - ItemSearchWidget.WIDTH;
        if (search == null) search = new ItemSearchWidget(font, searchX, topPos + 5, item -> item != VillagerPriceTable.getCurrency());
        else search.setPosition(searchX, topPos + 5);

        this.setWidgets();
    }

    public void setWidgets() {
        this.clearWidgets();

        int x  = leftPos + 7;
        int y  = topPos + 40;
        boolean buy = mode == WorkersMerchantTrade.VillagerTradeMode.BUY;

        // sell: amount that is kept in market and storages, buy: target amount in market and storages
        BlackShowingTextField amountField = new BlackShowingTextField(x, y + 15, 120, 20,
                Component.literal((buy ? TEXT_BUY_UNTIL : TEXT_KEEP).getString() + " " + amount));
        amountField.setTooltip(Tooltip.create(buy ? TOOLTIP_BUY_UNTIL : TOOLTIP_KEEP));
        addRenderableWidget(amountField);

        addRenderableWidget(new ExtendedButton(x + 120, y + 15, 20, 20, Component.literal("-"),
                button -> {
                    amount = Mth.clamp(amount - (hasShiftDown() ? 16 : 1), 0, MAX_AMOUNT);
                    this.setWidgets();
                }));
        addRenderableWidget(new ExtendedButton(x + 140, y + 15, 20, 20, Component.literal("+"),
                button -> {
                    amount = Mth.clamp(amount + (hasShiftDown() ? 16 : 1), 0, MAX_AMOUNT);
                    this.setWidgets();
                }));

        saveButton = new ExtendedButton(x, y + 40, 80, 20, BUTTON_SAVE,
                button -> {
                    this.trade.isVillagerTrade        = true;
                    this.trade.currencyItem           = VillagerPriceTable.getCurrencyStack();
                    this.trade.allowDamagedCurrency   = false;
                    this.trade.tradeItem              = tradeContainer.getTradeItem().copyWithCount(1);
                    this.trade.maxTrades              = -1;
                    this.trade.enabled                = enabled;
                    this.trade.villagerMode           = mode;
                    this.trade.useStorage             = true;
                    this.trade.storageAmount          = amount;

                    WorkersMain.SIMPLE_CHANNEL.sendToServer(
                            new MessageUpdateMerchantTrade(this.merchantEntity.getUUID(), this.trade, false));

                    new java.util.Timer().schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            Minecraft.getInstance().execute(() -> merchantEntity.openTradeGUI(player));
                        }
                    }, 250);

                    saveButton.active = false;
                });
        addRenderableWidget(saveButton);
        updateSaveButton();

        addRenderableWidget(new ExtendedButton(x + 80, y + 40, 80, 20, BUTTON_CANCEL,
                button -> merchantEntity.openTradeGUI(player)));

        // sell to villagers / buy from villagers
        ExtendedButton modeButton = new ExtendedButton(x + 170, y + 15, 100, 20, buy ? BUTTON_MODE_BUY : BUTTON_MODE_SELL,
                button -> {
                    this.mode = buy ? WorkersMerchantTrade.VillagerTradeMode.SELL : WorkersMerchantTrade.VillagerTradeMode.BUY;
                    // an untouched default follows the mode: sell keeps nothing, buy buys one stack
                    if (buy && amount == WorkersMerchantTrade.DEFAULT_BUY_AMOUNT) amount = WorkersMerchantTrade.DEFAULT_KEEP_AMOUNT;
                    else if (!buy && amount == WorkersMerchantTrade.DEFAULT_KEEP_AMOUNT) amount = WorkersMerchantTrade.DEFAULT_BUY_AMOUNT;
                    this.setWidgets();
                });
        modeButton.setTooltip(Tooltip.create(TOOLTIP_MODE));
        addRenderableWidget(modeButton);

        addRenderableWidget(new RecruitsCheckBox(x + 170, y + 40, 100, 20, TEXT_ENABLED,
                this.enabled,
                (bool) -> this.enabled = bool));

        if (search != null) addRenderableWidget(search.getBox());
    }

    /** The ghost slot of the trade item: the only slot that is not the player inventory. */
    private int getGhostSlotIndex() {
        for (Slot slot : tradeContainer.slots) {
            if (slot.container != player.getInventory()) return slot.index;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ItemStack picked = search != null ? search.getItemAt(mouseX, mouseY) : null;
        if (picked != null) {
            int index = getGhostSlotIndex();
            if (index >= 0) WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageSetGhostItem(index, picked.getItem(), 1));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (search != null && search.mouseScrolled(mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (search != null && search.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** No item, or the villager currency itself, can not be saved. */
    private void updateSaveButton() {
        if (saveButton == null) return;
        ItemStack item = tradeContainer.getTradeItem();
        saveButton.active = !item.isEmpty() && !item.is(VillagerPriceTable.getCurrency());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateSaveButton();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 5, fontColor, false);
        guiGraphics.drawString(font, player.getInventory().getDisplayName().getVisualOrderText(), 8, this.imageHeight - 96 + 2, fontColor, false);

        guiGraphics.drawString(font, mode == WorkersMerchantTrade.VillagerTradeMode.BUY ? TEXT_BUY_ITEM : TEXT_SELL_ITEM, 20, 30, fontColor, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        renderVillagerRate(guiGraphics, mouseX, mouseY);
        if (search != null) {
            search.render(guiGraphics, mouseX, mouseY);
            search.renderTooltip(guiGraphics, mouseX, mouseY, TOOLTIP_SEARCH_PICK);
        }
    }

    /**
     * Vanilla price of the ghost item as icons, e.g. "Mason pays:" [20 Clay] -> [1 Emerald].
     * The real price depends on the villager, random trades show a range.
     */
    private void renderVillagerRate(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ItemStack item = tradeContainer.getTradeItem().copyWithCount(1);
        if (item.isEmpty()) return;

        int x = leftPos + RATE_X;
        int y = topPos + RATE_Y;
        guiGraphics.fill(x, y, x + RATE_WIDTH, y + RATE_HEIGHT, 0xAA000000);

        boolean buy = mode == WorkersMerchantTrade.VillagerTradeMode.BUY;
        VillagerPriceTable.Entry entry = WorkersClientManager.villagerPrices.get(item.getItem());
        VillagerPriceTable.Rate rate = VillagerPriceTable.getRate(entry, buy);
        if (rate == null) {
            int lineY = y + 6;
            for (FormattedCharSequence line : font.split(buy ? TEXT_NO_VILLAGER_SELLS : TEXT_NO_VILLAGER_BUYS, RATE_WIDTH - 8)) {
                guiGraphics.drawString(font, line, x + 4, lineY, noRateColor, false);
                lineY += font.lineHeight + 1;
            }
            return;
        }

        Component trader = Component.translatable(buy ? "gui.workers.text.villagerAsks" : "gui.workers.text.villagerPays",
                Component.translatable(buy ? entry.sellTrader : entry.buyTrader));
        guiGraphics.drawString(font, Language.getInstance().getVisualOrder(font.substrByWidth(trader, RATE_WIDTH - 8)), x + 4, y + 4, infoColor, false);

        // what the merchant gives -> what he gets
        ItemStack currency = VillagerPriceTable.getCurrencyStack();
        ItemStack given    = buy ? currency : item;
        ItemStack received = buy ? item : currency;
        String givenText    = buy ? rate.price() : rate.items();
        String receivedText = buy ? rate.items() : rate.price();

        int iconY = y + 17;
        int givenX = x + 8;
        int receivedX = x + 56;
        renderIcon(guiGraphics, given, givenX, iconY, givenText);
        guiGraphics.drawString(font, "\u2192", x + 34, iconY + 4, infoColor, false);
        renderIcon(guiGraphics, received, receivedX, iconY, receivedText);

        ItemStack extra = buy && entry.sellExtra != null ? entry.sellExtra : ItemStack.EMPTY;
        int extraX = x + 92;
        if (!extra.isEmpty()) {
            guiGraphics.drawString(font, "+", x + 80, iconY + 4, infoColor, false);
            renderIcon(guiGraphics, extra.copyWithCount(1), extraX, iconY, String.valueOf(extra.getCount()));
        }

        // item tooltips
        ItemStack hovered = ItemStack.EMPTY;
        if (isOver(mouseX, mouseY, givenX, iconY)) hovered = given;
        else if (isOver(mouseX, mouseY, receivedX, iconY)) hovered = received;
        else if (!extra.isEmpty() && isOver(mouseX, mouseY, extraX, iconY)) hovered = extra;
        if (!hovered.isEmpty()) guiGraphics.renderTooltip(font, hovered, mouseX, mouseY);
    }

    private void renderIcon(GuiGraphics guiGraphics, ItemStack stack, int x, int y, @Nullable String count) {
        guiGraphics.renderFakeItem(stack, x, y);
        guiGraphics.renderItemDecorations(font, stack, x, y, "1".equals(count) ? null : count);
    }

    private static boolean isOver(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }
}
