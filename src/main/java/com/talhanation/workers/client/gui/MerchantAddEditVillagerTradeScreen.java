package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.BlackShowingTextField;
import com.talhanation.recruits.client.gui.widgets.RecruitsCheckBox;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.inventory.MerchantAddEditVillagerTradeContainer;
import com.talhanation.workers.network.MessageUpdateMerchantTrade;
import com.talhanation.workers.world.WorkersMerchantTrade;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class MerchantAddEditVillagerTradeScreen extends ScreenBase<MerchantAddEditVillagerTradeContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(WorkersMain.MOD_ID, "textures/gui/merchant_add_edit_villager_trade_screen.png");
    private static final MutableComponent BUTTON_CANCEL  = Component.translatable("gui.workers.button.cancel");
    private static final MutableComponent BUTTON_SAVE    = Component.translatable("gui.workers.button.save");
    private static final MutableComponent BUTTON_RESET   = Component.translatable("gui.workers.button.reset");
    private static final MutableComponent TEXT_ENABLED   = Component.translatable("gui.workers.checkbox.enabled");
    private static final int fontColor = 4210752;

    private final MerchantEntity merchantEntity;
    private final Player player;
    private final MerchantAddEditVillagerTradeContainer tradeContainer;
    public WorkersMerchantTrade trade;

    private ExtendedButton saveButton;
    private ExtendedButton cancelButton;
    private ExtendedButton plusMaxTradesButton;
    private ExtendedButton minusMaxTradesButton;
    private RecruitsCheckBox enabledCheckBox;

    private int currentTrades;
    private int maxTrades;
    private boolean enabled;

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
        this.currentTrades = this.trade.currentTrades;
        this.maxTrades     = this.trade.maxTrades;
        this.enabled       = this.trade.enabled;

        this.setWidgets();
    }

    public void setWidgets() {
        this.clearWidgets();

        int x  = leftPos + 7;
        int y  = topPos + 40;

        addRenderableWidget(new BlackShowingTextField(x, y + 15, 120, 20,
                Component.literal("currentTrades:     " + formatValue(currentTrades))));
        addRenderableWidget(new BlackShowingTextField(x, y + 40, 120, 20,
                Component.literal("maxTrades:          " + formatValue(maxTrades))));

        int y2 = y + 65;

        saveButton = new ExtendedButton(x, y2, 80, 20, BUTTON_SAVE,
                button -> {
                    this.trade.isVillagerTrade        = true;
                    this.trade.currencyItem           = new ItemStack(Items.EMERALD);
                    this.trade.allowDamagedCurrency   = false;
                    this.trade.tradeItem              = tradeContainer.getTradeItem();
                    this.trade.currentTrades          = currentTrades;
                    this.trade.maxTrades              = maxTrades;
                    this.trade.enabled                = enabled;

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

        cancelButton = new ExtendedButton(x + 80, y2, 80, 20, BUTTON_CANCEL,
                button -> merchantEntity.openTradeGUI(player));
        addRenderableWidget(cancelButton);

        resetButton(x, y);

        plusMaxTradesButton = new ExtendedButton(x + 120, y + 40, 20, 20, Component.literal("+"),
                button -> {
                    if (hasShiftDown()) maxTrades += 5;
                    else maxTrades++;
                    maxTrades = Mth.clamp(maxTrades, -1, 100);
                    this.setWidgets();
                });
        addRenderableWidget(plusMaxTradesButton);

        minusMaxTradesButton = new ExtendedButton(x + 140, y + 40, 20, 20, Component.literal("-"),
                button -> {
                    if (hasShiftDown()) maxTrades -= 5;
                    else maxTrades--;
                    maxTrades = Mth.clamp(maxTrades, -1, 100);
                    this.setWidgets();
                });
        addRenderableWidget(minusMaxTradesButton);

        this.enabledCheckBox = new RecruitsCheckBox(x + 170, y + 40, 100, 20, TEXT_ENABLED,
                this.enabled,
                (bool) -> this.enabled = bool);
        addRenderableWidget(enabledCheckBox);
    }

    private void resetButton(int x, int y) {
        ExtendedButton resetButton = new ExtendedButton(x + 120, y + 15, 40, 20, BUTTON_RESET,
                button -> {
                    this.currentTrades = 0;
                    this.setWidgets();
                });
        addRenderableWidget(resetButton);
    }

    public String formatValue(int x) {
        if (x == -1) return "\u221E"; // do not replace with "∞"
        return String.valueOf(x);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 5, fontColor, false);
        guiGraphics.drawString(font, player.getInventory().getDisplayName().getVisualOrderText(), 8, this.imageHeight - 96 + 2, fontColor, false);

        guiGraphics.drawString(font, Component.literal("Trade Item: "), 20, 30, fontColor, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
}
