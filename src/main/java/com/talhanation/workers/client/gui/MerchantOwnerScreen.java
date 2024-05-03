package com.talhanation.workers.client.gui;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.inventory.MerchantOwnerContainer;
import com.talhanation.workers.network.*;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.util.List;

public class MerchantOwnerScreen extends ScreenBase<MerchantOwnerContainer> {

    private static final ResourceLocation GUI_TEXTURE_3 = new ResourceLocation(Main.MOD_ID,
            "textures/gui/merchant_owner_gui.png");
    private static final int fontColor = 4210752;
    public static List<Integer> currentTrades;
    public static List<Integer> limits;
    private final MerchantEntity merchant;
    private final Inventory playerInventory;
    private final Player player;

    private final MutableComponent TEXT_TRAVEL = new TranslatableComponent("gui.workers.merchant.travel");
    private final MutableComponent TEXT_INVENTORY = new TranslatableComponent("gui.workers.merchant.inventory");
    private final MutableComponent TEXT_SHOW_TRADE = new TranslatableComponent("gui.workers.merchant.show_trade");
    public MerchantOwnerScreen(MerchantOwnerContainer container, Inventory playerInventory, Component title) {
        super(GUI_TEXTURE_3, container, playerInventory, new TextComponent(""));
        this.merchant = (MerchantEntity) container.getWorker();
        this.playerInventory = playerInventory;
        this.player = playerInventory.player;
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        this.setUpdatableButtons();
        int zeroLeftPos = leftPos + 180;
        int zeroTopPos = topPos - 30;
        int mirror = 240 - 60;


        addRenderableWidget(new Button(zeroLeftPos - mirror + 180, zeroTopPos + 35, 41, 20, TEXT_TRAVEL, button -> {
            this.merchant.openWaypointsGUI(player);
        }));

        addRenderableWidget(new Button(zeroLeftPos - mirror + 180, zeroTopPos + 200, 41, 20, TEXT_SHOW_TRADE, button -> {
            this.merchant.openTradeGUI(player);
        }));

        addRenderableWidget(new Button(zeroLeftPos - mirror + 180, zeroTopPos + 225, 41, 20, TEXT_INVENTORY, button -> {
            this.merchant.openGUI(player);

        }));

        if(this.player.isCreative() && this.player.createCommandSourceStack().hasPermission(4)){
            createCreativeButton(zeroLeftPos - mirror - 45, zeroTopPos + 35);
        }
        createTradeLimitButtons(zeroLeftPos - mirror + 130, zeroTopPos + 48, 0);
        createTradeLimitButtons(zeroLeftPos - mirror + 130, zeroTopPos + 48, 1);
        createTradeLimitButtons(zeroLeftPos - mirror + 130, zeroTopPos + 48, 2);
        createTradeLimitButtons(zeroLeftPos - mirror + 130, zeroTopPos + 48, 3);
        createTradeLimitButtons(zeroLeftPos - mirror + 130, zeroTopPos + 48, 4);
        createTradeLimitButtons(zeroLeftPos - mirror + 130, zeroTopPos + 48, 5);
    }

    public void setUpdatableButtons(){
        int zeroLeftPos = leftPos + 180;
        int zeroTopPos = topPos - 30;
        int mirror = 240 - 60;

        createHorseButton(zeroLeftPos - mirror + 180, zeroTopPos + 60);
    }

    private void createHorseButton(int x, int y) {
        String dis_mount;
        if(merchant.getVehicle() != null) dis_mount = "Dismount";
        else dis_mount = "Mount";

        addRenderableWidget(new ExtendedButton(x, y, 41, 20, Component.literal(dis_mount),
            button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantHorse(merchant.getUUID()));
                this.setUpdatableButtons();
        }));

    }
    private void createCreativeButton(int x, int y) {
        addRenderableWidget(new ExtendedButton(x, y, 41, 20, Component.literal("Creative"),
            button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantSetCreative(merchant.getUUID(), !merchant.isCreative()));
                this.setUpdatableButtons();
        }));

    }

    private void createTradeLimitButtons(int x, int y, int index){
        addRenderableWidget(new ExtendedButton(x, y + 18 * index, 12, 12, Component.literal("+"), button -> {
            int limit = limits.get(index);

             if(player.isShiftKeyDown()) limit = limit + 5;
             else limit++;

            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTradeLimitButton(index, limit, merchant.getUUID()));
        }));

        addRenderableWidget(new ExtendedButton(13 + x, y + 18 * index, 12, 12, Component.literal("-"), button -> {
            int limit = limits.get(index);

            if(player.isShiftKeyDown()) limit = limit - 5;
            else limit--;

            if(limit < -1) limit = -1;

            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTradeLimitButton(index, limit, merchant.getUUID()));
        }));
        addRenderableWidget(new ExtendedButton(26 + x, y + 18 * index, 12, 12, Component.literal("0"),
            button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantResetCurrentTradeCounts(merchant.getUUID(), index));
        }));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(font, merchant.getDisplayName().getVisualOrderText(), 8, 6, fontColor, false);
        guiGraphics.drawString(font, playerInventory.getDisplayName().getVisualOrderText(), 8, imageHeight - 152 + 59, fontColor, false);

        if(limits != null && currentTrades != null){
            for (int i = 0; i < limits.size(); i++) {
                int limit = limits.get(i);
                if(limit != -1){
                    guiGraphics.drawString(font, currentTrades.get(i) +"/" + limits.get(i), 103,  21 + i * 18, fontColor, false);
                }
                else
                    guiGraphics.drawString(font, currentTrades.get(i) +"/" +"\u221E", 103,  21 + i * 18, fontColor, false);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTicks, mouseX, mouseY);
    }
}
