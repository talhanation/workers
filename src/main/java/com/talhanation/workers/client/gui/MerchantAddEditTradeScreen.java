package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.inventory.MerchantAddEditTradeContainer;
import com.talhanation.workers.network.MessageUpdateLumberArea;
import com.talhanation.workers.network.MessageUpdateMerchantTrade;
import com.talhanation.workers.world.WorkersMerchantTrade;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class MerchantAddEditTradeScreen extends ScreenBase<MerchantAddEditTradeContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(WorkersMain.MOD_ID,"textures/gui/merchant_add_edit_trade_screen.png" );
    private static final MutableComponent BUTTON_CANCEL = Component.translatable("gui.workers.button.cancel");
    private static final MutableComponent BUTTON_SAVE = Component.translatable("gui.workers.button.save");
    private static final MutableComponent BUTTON_EDIT = Component.translatable("gui.workers.button.edit");
    private static final MutableComponent BUTTON_RESET = Component.translatable("gui.workers.button.reset");
    private static final MutableComponent TEXT_ALLOW_DAMAGED_ITEMS = Component.translatable("gui.workers.checkbox.allowDamagedCurrency");
    private static final int fontColor = 4210752;
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
    private int currentTrades;
    private int maxTrades;
    private boolean allowDamagedCurrency;
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
                    this.trade.currencyItem = tradeContainer.getCurrencyItem();
                    this.trade.tradeItem = tradeContainer.getTradeItem();
                    this.trade.currentTrades = currentTrades;
                    this.trade.maxTrades = maxTrades;
                    this.trade.allowDamagedCurrency = allowDamagedCurrency;
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
    }

    public String formatValue(int x){
        if(x == -1) return "\u221E"; // do not replace with "∞"

        return String.valueOf(x);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 5, fontColor, false);
        guiGraphics.drawString(font, player.getInventory().getDisplayName().getVisualOrderText(), 8, this.imageHeight - 96 + 2, fontColor, false);
    }
}
