package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.inventory.MerchantInventoryContainer;
import com.talhanation.workers.network.*;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public class MerchantOwnerScreen extends ScreenBase<MerchantInventoryContainer> {

    private static final ResourceLocation GUI_TEXTURE_3 = new ResourceLocation(Main.MOD_ID,
            "textures/gui/merchant_owner_gui.png");
    public static int FONT_COLOR_WHITE= 14342874;
    public static List<Integer> currentTrades;
    public static List<Integer> limits;
    private final MerchantEntity merchant;
    private final Inventory playerInventory;
    private final Player player;

    public MerchantOwnerScreen(MerchantInventoryContainer container, Inventory playerInventory, Component title) {
        super(GUI_TEXTURE_3, container, playerInventory, Component.literal(""));
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


        addRenderableWidget(new Button(zeroLeftPos - mirror + 180, zeroTopPos + 85, 41, 20, Component.literal("Travel"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenWaypointsGuiMerchant(this.player, this.merchant.getUUID()));
        }));

        if(this.player.isCreative() && this.player.createCommandSourceStack().hasPermission(4)){
            createCreativeButton(zeroLeftPos - mirror + 180, zeroTopPos + 148);
        }
        createTradeLimitButtons(zeroLeftPos - mirror + 131, zeroTopPos + 45, 0);
        createTradeLimitButtons(zeroLeftPos - mirror + 131, zeroTopPos + 45, 1);
        createTradeLimitButtons(zeroLeftPos - mirror + 131, zeroTopPos + 45, 2);
        createTradeLimitButtons(zeroLeftPos - mirror + 131, zeroTopPos + 45, 3);
    }

    public void setUpdatableButtons(){
        int zeroLeftPos = leftPos + 180;
        int zeroTopPos = topPos - 30;
        int mirror = 240 - 60;

        createHorseButton(zeroLeftPos - mirror + 180, zeroTopPos + 127);
    }

    private void createHorseButton(int x, int y) {
        String dis_mount;
        if(merchant.getVehicle() != null) dis_mount = "Dismount";
        else dis_mount = "Mount Horse";

        addRenderableWidget(new Button(x, y, 41, 20, Component.literal(dis_mount),
            button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantHorse(merchant.getUUID()));
                this.setUpdatableButtons();
        }));

    }
    private void createCreativeButton(int x, int y) {
        addRenderableWidget(new Button(x, y, 41, 20, Component.literal("Creative"),
            button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantSetCreative(merchant.getUUID(), !merchant.isCreative()));
                setUpdatableButtons();
        }));
    }

    private void createTradeLimitButtons(int x, int y, int index){
        addRenderableWidget(new Button(x, y + 18 * index, 12, 12, Component.literal("+"), button -> {
            int limit = limits.get(index);
             limit++;
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTradeLimitButton(index, limit, merchant.getUUID()));
        }));

        addRenderableWidget(new Button(13 + x, y + 18 * index, 12, 12, Component.literal("-"), button -> {
            int limit = limits.get(index);
            if(limit > -1){
                limit--;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTradeLimitButton(index, limit, merchant.getUUID()));
            }
        }));

        addRenderableWidget(new Button(26 + x, y + 18 * index, 12, 12, Component.literal("0"),
            button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantResetCurrentTradeCounts(merchant.getUUID()));
        }));
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        font.draw(matrixStack, merchant.getDisplayName().getVisualOrderText(), 8, 6, FONT_COLOR);
        font.draw(matrixStack, playerInventory.getDisplayName().getVisualOrderText(), 8, imageHeight - 152 + 59, FONT_COLOR);






        if(limits != null && currentTrades != null){
            for (int i = 0; i < limits.size(); i++) {
                int limit = limits.get(i);
                if(limit != -1){
                    font.draw(matrixStack, currentTrades.get(i) +"/" + limits.get(i), 100,  22 + i * 18, FONT_COLOR);
                }
                else
                    font.draw(matrixStack, currentTrades.get(i) +"/" +"∞", 100,  22 + i * 18, FONT_COLOR);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
    }
}
