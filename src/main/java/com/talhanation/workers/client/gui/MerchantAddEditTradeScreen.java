package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.client.gui.widgets.ItemSearchWidget;
import com.talhanation.workers.inventory.MerchantAddEditTradeContainer;
import com.talhanation.workers.network.MessageSetGhostItem;
import com.talhanation.workers.network.MessageUpdateMerchantTrade;
import com.talhanation.workers.world.VillagerPriceTable;
import com.talhanation.workers.world.WorkersMerchantTrade;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class MerchantAddEditTradeScreen extends ScreenBase<MerchantAddEditTradeContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(WorkersMain.MOD_ID,"textures/gui/merchant_add_edit_trade_screen.png" );
    private static final MutableComponent BUTTON_CANCEL = Component.translatable("gui.workers.button.cancel");
    private static final MutableComponent BUTTON_SAVE = Component.translatable("gui.workers.button.save");
    private static final MutableComponent BUTTON_EDIT = Component.translatable("gui.workers.button.edit");
    private static final MutableComponent BUTTON_RESET = Component.translatable("gui.workers.button.reset");
    private static final MutableComponent TEXT_ALLOW_DAMAGED_ITEMS = Component.translatable("gui.workers.checkbox.allowDamagedCurrency");
    private static final MutableComponent TEXT_ENABLED = Component.translatable("gui.workers.checkbox.enabled");
    private static final MutableComponent TEXT_VILLAGER_TRADE = Component.translatable("gui.workers.checkbox.villagerTrade");
    private static final MutableComponent TEXT_USE_STORAGE = Component.translatable("gui.workers.checkbox.useStorage");
    private static final MutableComponent TOOLTIP_USE_STORAGE = Component.translatable("gui.workers.checkbox.tooltip.tradeStorageSell");
    private static final MutableComponent TEXT_KEEP = Component.translatable("gui.workers.text.storageKeep");
    private static final MutableComponent TOOLTIP_KEEP = Component.translatable("gui.workers.tooltip.storageKeep");
    private static final MutableComponent TOOLTIP_SEARCH_PICK = Component.translatable("gui.workers.search.tooltip.pickTrade");
    private static final int fontColor = 4210752;
    private static final int MAX_STORAGE_AMOUNT = 9999;
    private final MerchantEntity merchantEntity;
    private final Player player;
    private ExtendedButton resetButton;
    public WorkersMerchantTrade trade;
    public MerchantAddEditTradeContainer tradeContainer;
    private ExtendedButton saveButton;
    private ExtendedButton cancelButton;
    private ExtendedButton plusMaxTradesButton;
    private ExtendedButton minusMaxTradesButton;
    private RecruitsCheckBox allowDamagedCurrencyCheckBox;
    private RecruitsCheckBox enabledCheckBox;
    private RecruitsCheckBox villagerTradeCheckBox;
    private int currentTrades;
    private int maxTrades;
    private boolean allowDamagedCurrency;
    private boolean enabled;
    private boolean isVillagerTrade;
    private boolean useStorage;
    private int storageAmount;
    @Nullable private ItemSearchWidget search;

    public MerchantAddEditTradeScreen(MerchantAddEditTradeContainer tradeContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, tradeContainer, playerInventory, Component.literal("Add or Edit Merchant Trade"));
        this.tradeContainer = tradeContainer;
        this.merchantEntity = tradeContainer.getMerchantEntity();
        this.player = playerInventory.player;
        this.trade = tradeContainer.getTrade();
        imageWidth = 176;
        imageHeight = 223;
    }

    @Override
    protected void init() {
        super.init();
        this.currentTrades = this.trade.currentTrades;
        this.maxTrades = this.trade.maxTrades;
        this.allowDamagedCurrency = this.trade.allowDamagedCurrency;
        this.enabled = this.trade.enabled;
        this.isVillagerTrade = this.trade.isVillagerTrade;
        this.useStorage = this.trade.useStorage;
        this.storageAmount = this.trade.storageAmount;

        // item search left of the texture, keeps its text when the screen is resized
        int searchX = leftPos - 6 - ItemSearchWidget.WIDTH;
        if (search == null) search = new ItemSearchWidget(font, searchX, topPos + 5, item -> true);
        else search.setPosition(searchX, topPos + 5);

        this.setWidgets();
    }

    public void setWidgets(){
        this.clearWidgets();

        int x = leftPos + 7;
        int y = topPos + 40;

        addRenderableWidget(new BlackShowingTextField(x, y + 15, 120, 20, Component.literal("currentTrades:     " + formatValue(currentTrades) )));
        addRenderableWidget(new BlackShowingTextField(x, y + 40, 120, 20, Component.literal("maxTrades:          " + formatValue(maxTrades))));


        int y2 = y + 65;
        saveButton = new ExtendedButton(x, y2, 80, 20, BUTTON_SAVE,
                button -> {
                    this.trade.tradeItem = tradeContainer.getTradeItem();
                    this.trade.currentTrades = currentTrades;
                    this.trade.maxTrades = maxTrades;
                    this.trade.enabled = enabled;
                    this.trade.isVillagerTrade = isVillagerTrade;
                    this.trade.useStorage = useStorage;
                    this.trade.storageAmount = storageAmount;

                    if(isVillagerTrade) {
                        // Currency is always emerald for villager trades – auto-set
                        this.trade.currencyItem = VillagerPriceTable.getCurrencyStack();
                        this.trade.allowDamagedCurrency = false;
                    }
                    else {
                        this.trade.currencyItem = tradeContainer.getCurrencyItem();
                        this.trade.allowDamagedCurrency = allowDamagedCurrency;
                    }

                    WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateMerchantTrade(this.merchantEntity.getUUID(), this.trade, false));

                    new java.util.Timer().schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            Minecraft.getInstance().execute(() -> merchantEntity.openTradeGUI(player));
                        }
                    }, 250);

                    saveButton.active = false;
                });
        addRenderableWidget(saveButton);

        cancelButton = new ExtendedButton(x + 80, y2, 80, 20, BUTTON_CANCEL,
                button -> {
                    merchantEntity.openTradeGUI(player);
                });
        addRenderableWidget(cancelButton);

        resetButton = new ExtendedButton(x + 120, y + 15, 40, 20, BUTTON_RESET,
                button -> {
                    this.currentTrades = 0;
                    this.setWidgets();
                });
        addRenderableWidget(resetButton);

        plusMaxTradesButton = new ExtendedButton(x + 120, y + 40, 20, 20, Component.literal("+"),
                button -> {
                    if(hasShiftDown()) maxTrades += 5;
                    else maxTrades++;
                    maxTrades = Mth.clamp(maxTrades, -1, 100);
                    this.setWidgets();
                });
        addRenderableWidget(plusMaxTradesButton);

        minusMaxTradesButton = new ExtendedButton(x + 140, y + 40, 20, 20, Component.literal("-"),
                button -> {
                    if(hasShiftDown()) maxTrades -= 5;
                    else maxTrades--;
                    maxTrades = Mth.clamp(maxTrades, -1, 100);
                    this.setWidgets();
                });
        addRenderableWidget(minusMaxTradesButton);

        this.allowDamagedCurrencyCheckBox = new RecruitsCheckBox(x + 170, y + 15, 100, 20, TEXT_ALLOW_DAMAGED_ITEMS,
                this.allowDamagedCurrency,
                (bool) -> {
                    this.allowDamagedCurrency = bool;
                }
        );
        addRenderableWidget(allowDamagedCurrencyCheckBox);

        this.enabledCheckBox = new RecruitsCheckBox(x + 170, y + 40, 100, 20, TEXT_ENABLED,
                this.enabled,
                (bool) -> {
                    this.enabled = bool;
                }
        );
        addRenderableWidget(enabledCheckBox);

        RecruitsCheckBox useStorageCheckBox = new RecruitsCheckBox(x + 170, y + 65, 100, 20, TEXT_USE_STORAGE,
                this.useStorage,
                (bool) -> {
                    this.useStorage = bool;
                    this.setWidgets();
                }
        );
        useStorageCheckBox.setTooltip(Tooltip.create(TOOLTIP_USE_STORAGE));
        addRenderableWidget(useStorageCheckBox);

        if (search != null) addRenderableWidget(search.getBox());

        if (!useStorage) return;

        // amount of the trade item that always stays in the storage
        BlackShowingTextField amountField = new BlackShowingTextField(x + 170, y + 90, 60, 20,
                Component.literal(TEXT_KEEP.getString() + " " + storageAmount));
        amountField.setTooltip(Tooltip.create(TOOLTIP_KEEP));
        addRenderableWidget(amountField);

        ExtendedButton minusAmountButton = new ExtendedButton(x + 230, y + 90, 20, 20, Component.literal("-"),
                button -> {
                    storageAmount = Mth.clamp(storageAmount - (hasShiftDown() ? 16 : 1), 0, MAX_STORAGE_AMOUNT);
                    this.setWidgets();
                });
        addRenderableWidget(minusAmountButton);

        ExtendedButton plusAmountButton = new ExtendedButton(x + 250, y + 90, 20, 20, Component.literal("+"),
                button -> {
                    storageAmount = Mth.clamp(storageAmount + (hasShiftDown() ? 16 : 1), 0, MAX_STORAGE_AMOUNT);
                    this.setWidgets();
                });
        addRenderableWidget(plusAmountButton);
    }

    /** Ghost slots in container order: currency first, then the trade item. */
    private List<Slot> getGhostSlots() {
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : tradeContainer.slots) {
            if (slot.container != player.getInventory()) slots.add(slot);
        }
        return slots;
    }

    /** Left click: trade item, right click: currency. The same item again adds one, with shift 16. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ItemStack picked = search != null ? search.getItemAt(mouseX, mouseY) : null;
        if (picked != null) {
            List<Slot> ghosts = getGhostSlots();
            boolean currency = button == 1;
            if (ghosts.size() >= 2 && !(currency && isVillagerTrade)) {
                Slot slot = ghosts.get(currency ? 0 : 1);
                ItemStack current = slot.getItem();
                int count = current.is(picked.getItem()) ? current.getCount() + (hasShiftDown() ? 16 : 1) : 1;
                WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageSetGhostItem(slot.index, picked.getItem(), count));
            }
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

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        if (search != null) {
            search.render(guiGraphics, mouseX, mouseY);
            search.renderTooltip(guiGraphics, mouseX, mouseY, TOOLTIP_SEARCH_PICK);
        }
    }

    public String formatValue(int x){
        if(x == -1) return "\u221E"; // do not replace with "∞"

        return String.valueOf(x);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 5, fontColor, false);
        guiGraphics.drawString(font, player.getInventory().getDisplayName().getVisualOrderText(), 8, this.imageHeight - 96 + 2, fontColor, false);

        if(isVillagerTrade) {
            guiGraphics.drawString(font, Component.literal("auto"), 42, 28, 0x55AA55, false);
        }
    }
}