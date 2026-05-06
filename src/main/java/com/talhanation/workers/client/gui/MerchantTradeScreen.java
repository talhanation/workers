package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.inventory.MerchantTradeContainer;
import com.talhanation.workers.network.MessageDoTradeWithMerchant;
import com.talhanation.workers.network.MessageMoveMerchantTrade;
import com.talhanation.workers.network.MessageUpdateMerchant;
import com.talhanation.workers.network.MessageUpdateMerchantTrade;
import com.talhanation.workers.world.WorkersMerchantTrade;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class MerchantTradeScreen extends ScreenBase<MerchantTradeContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(WorkersMain.MOD_ID,"textures/gui/merchant.png" );
    private static final ResourceLocation ARROW_IMAGE = new ResourceLocation(WorkersMain.MOD_ID, "textures/gui/arrow.png");
    private static final MutableComponent BUTTON_ADD = Component.translatable("gui.workers.button.add");
    private static final MutableComponent BUTTON_EDIT = Component.translatable("gui.workers.button.edit");
    private static final MutableComponent BUTTON_REMOVE = Component.translatable("gui.workers.button.remove");
    private static final MutableComponent BUTTON_COPY = Component.translatable("gui.workers.button.copy");
    private static final MutableComponent BUTTON_TRADE = Component.translatable("gui.workers.button.trade");
    private static final MutableComponent TEXT_CREATIVE = Component.translatable("gui.workers.text.creative");
    private static final MutableComponent TEXT_DAILY_REFRESH = Component.translatable("gui.workers.text.dailyRefresh");
    private static final int fontColor = 4210752;

    private static final int LIST_X       = 5;
    private static final int LIST_Y       = 18;
    private static final int LIST_W       = 85;
    private static final int LIST_H       = 170;
    private static final int LIST_ITEM_H  = 28;  // was 40, compacted like CourierScreen

    private final MerchantEntity merchantEntity;
    private final Player player;
    private final boolean isOwner;
    public WorkersMerchantTrade selection;
    public MerchantTradeContainer tradeContainer;
    private ExtendedButton tradeButton;
    private ExtendedButton addEditTradeButton;
    private ExtendedButton removeTradeButton;
    private RecruitsCheckBox creativeCheckbox;
    private RecruitsCheckBox dailyRefreshCheckbox;
    private ExtendedButton copyTradeButton;
    private ExtendedButton moveUpButton;
    private ExtendedButton moveDownButton;
    private boolean isCreative;
    private boolean isDailyRefresh;
    private ItemStack hoveredTooltipStack = ItemStack.EMPTY;
    private int hoveredTooltipX = 0;
    private int hoveredTooltipY = 0;
    private TradeList tradeList;

    public MerchantTradeScreen(MerchantTradeContainer tradeContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, tradeContainer, playerInventory, Component.literal("Trades"));
        this.tradeContainer = tradeContainer;
        this.merchantEntity = tradeContainer.getMerchantEntity();
        this.player = playerInventory.player;
        this.isOwner = player.getUUID().equals(merchantEntity.getOwnerUUID());
        imageWidth = 256;
        imageHeight = 197;
    }

    @Override
    protected void init() {
        super.init();
        this.isCreative = merchantEntity.isCreative();
        this.isDailyRefresh = merchantEntity.isDailyRefresh();
        this.setWidgets();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        //this.loadTrades();
    }

    public void setWidgets(){
        this.clearWidgets();

        int listLeft   = leftPos + LIST_X;
        int listTop    = topPos  + LIST_Y;
        int listBottom = listTop + LIST_H;

        this.tradeList = new TradeList(Minecraft.getInstance(), LIST_W, LIST_H, listTop, listBottom, LIST_ITEM_H, LIST_W);
        this.tradeList.setLeftPos(listLeft);
        this.tradeList.setRenderBackground(false);
        this.tradeList.setRenderTopAndBottom(false);
        this.tradeList.setRenderSelection(false);

        this.loadTrades();
        this.addRenderableWidget(this.tradeList);

        if((merchantEntity.isCreative() && player.isCreative()) || isOwner){
            tradeButton = new ExtendedButton(leftPos + 88, topPos + 58, 60, 18, BUTTON_TRADE,
                    button -> {
                        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageDoTradeWithMerchant(merchantEntity.getUUID(), selection.uuid));
                        this.selection.currentTrades++;
                        this.updateButtonState();
                    });
            addRenderableWidget(tradeButton);

            addEditTradeButton = new ExtendedButton(leftPos + 186, topPos + 58, 60, 18, Component.empty(),
                    button -> {
                        WorkersMerchantTrade trade = selection == null ? new WorkersMerchantTrade() : selection;
                        merchantEntity.openAddEditTradeGUI(player, trade);
                        tradeList.addEntry(this.tradeList.new TradeEntry(trade));
                        this.selection = null;
                        tradeList.setSelected(null);
                        updateButtonState();
                    });
            addRenderableWidget(addEditTradeButton);

            copyTradeButton = new ExtendedButton(leftPos + 88, topPos + 77, 60, 18, BUTTON_COPY,
                    button -> {
                        WorkersMerchantTrade trade = selection == null ? new WorkersMerchantTrade() : selection.copy();
                        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateMerchantTrade(this.merchantEntity.getUUID(), trade, false));
                        tradeList.addEntry(this.tradeList.new TradeEntry(trade));
                        this.selection = null;
                        tradeList.setSelected(null);
                        updateButtonState();
                    });
            addRenderableWidget(copyTradeButton);

            removeTradeButton = new ExtendedButton(leftPos + 186, topPos + 77, 60, 18, BUTTON_REMOVE,
                    button -> {
                        tradeList.children().removeIf(tradeEntry -> tradeEntry.trade.uuid.equals(selection.uuid));
                        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateMerchantTrade(merchantEntity.getUUID(), selection, true));
                        this.selection = null;
                        tradeList.setSelected(null);
                        updateButtonState();
                    });
            addRenderableWidget(removeTradeButton);

            moveUpButton = new ExtendedButton(leftPos + 158, topPos + 58, 18, 18, Component.literal("\u21E7"),//do not replace
                    button -> {
                        if (selection == null) return;
                        UUID selectedUuid = selection.uuid;
                        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageMoveMerchantTrade(merchantEntity.getUUID(), selectedUuid, true));
                        List<WorkersMerchantTrade> list = new java.util.ArrayList<>(merchantEntity.getTrades());
                        for (int i = 1; i < list.size(); i++) {
                            if (list.get(i).uuid.equals(selectedUuid)) {
                                WorkersMerchantTrade tmp = list.get(i - 1);
                                list.set(i - 1, list.get(i));
                                list.set(i, tmp);
                                break;
                            }
                        }
                        merchantEntity.setTrades(list);
                        loadTrades();
                        restoreSelection(selectedUuid);
                    });
            addRenderableWidget(moveUpButton);

            moveDownButton = new ExtendedButton(leftPos + 158, topPos + 77, 18, 18, Component.literal("\u21E9"),//do not replace
                    button -> {
                        if (selection == null) return;
                        UUID selectedUuid = selection.uuid;
                        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageMoveMerchantTrade(merchantEntity.getUUID(), selectedUuid, false));
                        List<WorkersMerchantTrade> list = new java.util.ArrayList<>(merchantEntity.getTrades());
                        for (int i = 0; i < list.size() - 1; i++) {
                            if (list.get(i).uuid.equals(selectedUuid)) {
                                WorkersMerchantTrade tmp = list.get(i + 1);
                                list.set(i + 1, list.get(i));
                                list.set(i, tmp);
                                break;
                            }
                        }
                        merchantEntity.setTrades(list);
                        loadTrades();
                        restoreSelection(selectedUuid);
                    });
            addRenderableWidget(moveDownButton);

            if(player.isCreative()) {
                this.creativeCheckbox = new RecruitsCheckBox(leftPos + 256, topPos + 172, 100, 20, TEXT_CREATIVE,
                        this.isCreative,
                        (bool) -> {
                            this.isCreative = bool;
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateMerchant(merchantEntity.getUUID(), isCreative, true, isDailyRefresh));
                            setWidgets(); // rebuild so daily-refresh checkbox shows/hides
                        }
                );
                addRenderableWidget(creativeCheckbox);

                if (this.isCreative) {
                    this.dailyRefreshCheckbox = new RecruitsCheckBox(leftPos + 256, topPos + 192, 100, 20, TEXT_DAILY_REFRESH,
                            this.isDailyRefresh,
                            (bool) -> {
                                this.isDailyRefresh = bool;
                                WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateMerchant(merchantEntity.getUUID(), isCreative, true, isDailyRefresh));
                            }
                    );
                    addRenderableWidget(dailyRefreshCheckbox);
                }
            }
        }
        else{
            if(player.isCreative()) {
                this.creativeCheckbox = new RecruitsCheckBox(leftPos + 256, topPos + 172, 100, 20, TEXT_CREATIVE,
                        this.isCreative,
                        (bool) -> {
                            this.isCreative = bool;
                            WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateMerchant(merchantEntity.getUUID(), isCreative, true, isDailyRefresh));
                            setWidgets(); // rebuild so daily-refresh checkbox shows/hides
                        }
                );
                addRenderableWidget(creativeCheckbox);

                if (this.isCreative) {
                    this.dailyRefreshCheckbox = new RecruitsCheckBox(leftPos + 256, topPos + 192, 100, 20, TEXT_DAILY_REFRESH,
                            this.isDailyRefresh,
                            (bool) -> {
                                this.isDailyRefresh = bool;
                                WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateMerchant(merchantEntity.getUUID(), isCreative, true, isDailyRefresh));
                            }
                    );
                    addRenderableWidget(dailyRefreshCheckbox);
                }
            }

            tradeButton = new ExtendedButton(leftPos + 97, topPos + 66, 140, 20, BUTTON_TRADE,
                    button -> {
                        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageDoTradeWithMerchant(merchantEntity.getUUID(), selection.uuid));
                        this.selection.currentTrades++;
                        this.updateButtonState();
                    });
            addRenderableWidget(tradeButton);
        }
        this.updateButtonState();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean clicked = super.mouseClicked(mouseX, mouseY, button);

        boolean overAddEdit = this.addEditTradeButton != null && this.addEditTradeButton.isHovered();
        boolean overTrade = this.tradeButton != null && this.tradeButton.isHovered();
        boolean overTradeList = this.tradeList != null && this.tradeList.isMouseOver(mouseX, mouseY);
        boolean overCopy = this.copyTradeButton != null && this.copyTradeButton.isMouseOver(mouseX, mouseY);
        boolean overUp = this.moveUpButton != null && this.moveUpButton.isMouseOver(mouseX, mouseY);
        boolean overDown = this.moveDownButton != null && this.moveDownButton.isMouseOver(mouseX, mouseY);

        if (!overAddEdit && !overTrade && !overTradeList && !overCopy && !overUp && !overDown) {
            this.selection = null;
            if (this.tradeList != null)
                this.tradeList.setSelected(null);
            this.updateButtonState();
        }

        return clicked;
    }

    private void restoreSelection(UUID tradeUuid) {
        for (TradeList.TradeEntry entry : tradeList.children()) {
            if (entry.trade.uuid.equals(tradeUuid)) {
                this.tradeList.setSelected(entry);
                this.selection = entry.trade;
                return;
            }
        }
        this.selection = null;
        tradeList.setSelected(null);
        updateButtonState();
    }

    public void loadTrades(){
        this.tradeList.clearEntries();
        List<WorkersMerchantTrade> trades = merchantEntity.getTrades();
        for(WorkersMerchantTrade merchantTrade : trades){
            if(!merchantTrade.enabled && !isOwner) continue;
            if(merchantTrade.isVillagerTrade && !isOwner) continue;
            tradeList.addEntry(this.tradeList.new TradeEntry(merchantTrade));
        }
    }

    public void onSelected(TradeList.TradeEntry entry){
        this.selection = entry.trade;
        this.updateButtonState();
    }

    public void updateButtonState(){
        if(addEditTradeButton != null){
            this.addEditTradeButton.setMessage(selection != null ? BUTTON_EDIT : BUTTON_ADD);
        }
        if(removeTradeButton != null){
            removeTradeButton.active = selection != null;
        }
        if(copyTradeButton != null){
            copyTradeButton.active = selection != null;
        }
        if(moveUpButton != null){
            moveUpButton.active = selection != null;
        }
        if(moveDownButton != null){
            moveDownButton.active = selection != null;
        }

        this.tradeButton.active = false;

        if(selection != null){
            boolean playerFreeSlot = player.getInventory().getFreeSlot() != -1;
            boolean withinLimit = selection.maxTrades == -1 || selection.currentTrades < selection.maxTrades;
            this.tradeButton.active = playerFreeSlot && withinLimit && selection.enabled && !selection.isVillagerTrade;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, float partialTicks) {
        this.hoveredTooltipStack = ItemStack.EMPTY;
        super.render(guiGraphics, x, y, partialTicks);
        if (!this.hoveredTooltipStack.isEmpty()) {
            this.renderItemStackTooltip(guiGraphics, this.hoveredTooltipStack, this.hoveredTooltipX, this.hoveredTooltipY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 5, fontColor, false);

        // Show "MerchantName - MarketName" if merchant is in a market, else just merchant name
        String marketName = merchantEntity.getCurrentMarketName();
        Component nameLabel = marketName.isEmpty()
                ? merchantEntity.getDisplayName()
                : Component.literal(merchantEntity.getDisplayName().getString() + " - " + marketName);
        guiGraphics.drawString(font, nameLabel, 92, 5, fontColor, false);

        guiGraphics.drawString(font, player.getInventory().getDisplayName().getVisualOrderText(), 92, this.imageHeight - 96 + 2, fontColor, false);
    }

    protected void renderItemStackTooltip(GuiGraphics guiGraphics, ItemStack itemstack, int mouseX, int mouseY) {
        guiGraphics.renderTooltip(this.font, this.getTooltipFromContainerItem(itemstack), itemstack.getTooltipImage(), itemstack, mouseX, mouseY);
    }

    // ── TradeList ─────────────────────────────────────────────────────────────

    private class TradeList extends ObjectSelectionList<TradeList.TradeEntry> {

        final int itemWidth;

        TradeList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight, int itemWidth) {
            super(mc, width, height, top, bottom, itemHeight);
            this.itemWidth = itemWidth;
        }

        @Override protected int addEntry(TradeList.TradeEntry entry) { return super.addEntry(entry); }
        @Override protected void clearEntries() { super.clearEntries(); }

        /** Scrollbar sits flush against the right edge of the list area. */
        @Override
        protected int getScrollbarPosition() {
            // leftPos + LIST_X + LIST_W - 6  →  right edge of list minus scrollbar width
            return MerchantTradeScreen.this.leftPos + LIST_X + LIST_W - 14;
        }

        @Override
        public int getRowLeft() {
            return MerchantTradeScreen.this.leftPos + LIST_X + 2;
        }

        @Override
        public int getRowWidth() {
            // Full list width minus scrollbar (6px) minus small gap (4px)
            return LIST_W - 18;
        }

        public void setSelected(@Nullable TradeList.TradeEntry entry) {
            super.setSelected(entry);
            if(entry == null) return;
            MerchantTradeScreen.this.onSelected(entry);
        }

        // ── TradeEntry ────────────────────────────────────────────────────────

        public class TradeEntry extends ObjectSelectionList.Entry<TradeList.TradeEntry> {

            private final WorkersMerchantTrade trade;

            TradeEntry(WorkersMerchantTrade trade) {
                this.trade = trade;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTicks) {
                int rowLeft  = TradeList.this.getRowLeft();
                int rowWidth = TradeList.this.getRowWidth();
                boolean selected = (TradeList.this.getSelected() == this);
                boolean out      = trade.maxTrades != -1 && trade.currentTrades >= trade.maxTrades;
                boolean disabled = !trade.enabled;
                int textureY     = getButtonTextureY(hovered, selected, out || disabled);

                float alpha = disabled ? 0.45f : 1.0f;
                RenderSystem.enableBlend();
                RenderSystem.enableDepthTest();
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
                guiGraphics.blitNineSliced(AbstractButton.WIDGETS_LOCATION, rowLeft, top, rowWidth, entryHeight, 20, 4, 200, 20, 0, textureY);

                // Vertically centre items in the compacted entry height (28px)
                final int itemY = top + (entryHeight - 16) / 2;

                if(!trade.isVillagerTrade) {
                    final int item1X = rowLeft + 2;
                    final int arrowX = rowLeft + rowWidth / 2 - 9;
                    final int item2X = rowLeft + rowWidth - 18;

                    guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);

                    guiGraphics.blit(ARROW_IMAGE, arrowX, itemY - 3, 0, 0, 21, 21, 21, 21);

                    guiGraphics.renderFakeItem(trade.currencyItem, item1X, itemY);
                    guiGraphics.renderItemDecorations(font, trade.currencyItem, item1X, itemY);

                    guiGraphics.renderFakeItem(trade.tradeItem, item2X, itemY);
                    guiGraphics.renderItemDecorations(font, trade.tradeItem, item2X, itemY);

                    // Tooltips
                    if (!trade.currencyItem.isEmpty() && mouseX >= item1X && mouseX < item1X + 16 && mouseY >= top && mouseY < top + entryHeight) {
                        MerchantTradeScreen.this.hoveredTooltipStack = trade.currencyItem;
                        MerchantTradeScreen.this.hoveredTooltipX = mouseX;
                        MerchantTradeScreen.this.hoveredTooltipY = mouseY;
                    } else if (!trade.tradeItem.isEmpty() && mouseX >= item2X && mouseX < item2X + 16 && mouseY >= top && mouseY < top + entryHeight) {
                        MerchantTradeScreen.this.hoveredTooltipStack = trade.tradeItem;
                        MerchantTradeScreen.this.hoveredTooltipX = mouseX;
                        MerchantTradeScreen.this.hoveredTooltipY = mouseY;
                    }
                }
                else {
                    // Villager trade: emerald-green tint + single centred item
                    guiGraphics.fill(rowLeft, top, rowLeft + rowWidth, top + entryHeight, 0x5500AA44);

                    final int itemX = rowLeft + rowWidth / 2 - 8;
                    guiGraphics.renderFakeItem(trade.tradeItem, itemX, itemY);
                    guiGraphics.renderItemDecorations(font, trade.tradeItem, itemX, itemY);

                    if (!trade.tradeItem.isEmpty() && mouseX >= itemX && mouseX < itemX + 16 && mouseY >= top && mouseY < top + entryHeight) {
                        MerchantTradeScreen.this.hoveredTooltipStack = trade.tradeItem;
                        MerchantTradeScreen.this.hoveredTooltipX = mouseX;
                        MerchantTradeScreen.this.hoveredTooltipY = mouseY;
                    }
                }

                guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                TradeList.this.setSelected(this);
                MerchantTradeScreen.this.selection = this.trade;
                MerchantTradeScreen.this.updateButtonState();
                return true;
            }

            @Override
            public Component getNarration() { return Component.empty(); }

            private int getButtonTextureY(boolean hovered, boolean selected, boolean out) {
                final int BUTTON_Y_OUT     = 46;
                final int BUTTON_Y_NORMAL  = 66;
                final int BUTTON_Y_HOVER   = 86;
                final int BUTTON_Y_PRESSED = 86;

                if (out)      return BUTTON_Y_OUT;
                if (selected) return BUTTON_Y_PRESSED;
                if (hovered)  return BUTTON_Y_HOVER;
                return BUTTON_Y_NORMAL;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (tradeList != null && tradeList.isMouseOver(mouseX, mouseY))
            return tradeList.mouseScrolled(mouseX, mouseY, delta);
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (tradeList != null)
            tradeList.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void onClose() {
        super.onClose();
        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateMerchant(merchantEntity.getUUID(), isCreative, false, isDailyRefresh));
    }
}