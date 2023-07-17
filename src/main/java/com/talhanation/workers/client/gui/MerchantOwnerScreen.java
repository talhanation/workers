package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.inventory.MerchantInventoryContainer;
import com.talhanation.workers.network.*;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class MerchantOwnerScreen extends ScreenBase<MerchantInventoryContainer> {

    private static final ResourceLocation GUI_TEXTURE_3 = new ResourceLocation(Main.MOD_ID,
            "textures/gui/merchant_owner_gui.png");
    public static int FONT_COLOR_WHITE= 14342874;
    private final MerchantEntity merchant;
    private final Inventory playerInventory;
    private final Player player;
    public static List<BlockPos> waypoints;

    private int returnTime;

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
        this.returnTime = merchant.getReturningTime();
        this.setButtons();
    }

    public void setButtons(){
        int zeroLeftPos = leftPos + 180;
        int zeroTopPos = topPos - 30;
        int mirror = 240 - 60;

        addRenderableWidget(new Button(zeroLeftPos - mirror + 180, zeroTopPos + 85, 41, 20, Component.literal("Travel"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantTravel(merchant.getUUID(), !merchant.getTraveling()));
        }));

        addRenderableWidget(new Button(zeroLeftPos - mirror + 180, zeroTopPos + 106, 20, 20, Component.literal("+"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantAddWayPoint(merchant.getUUID()));
        }));

        addRenderableWidget(new Button(zeroLeftPos - mirror + 201, zeroTopPos + 106, 20, 20, Component.literal("-"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantRemoveWayPoint(merchant.getUUID()));
        }));

        if(this.player.isCreative() && this.player.createCommandSourceStack().hasPermission(4)){
            createCreativeButton(zeroLeftPos - mirror + 180, zeroTopPos + 127);
        }

        // ReturnTime
        addRenderableWidget(new Button(zeroLeftPos - mirror + 20, zeroTopPos - 60, 8, 12, Component.literal("+"), button -> {
            this.returnTime = merchant.getReturningTime();
            this.returnTime++;
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantReturnTime(this.returnTime, merchant.getUUID()));

        }));

        addRenderableWidget(new Button(zeroLeftPos - mirror, zeroTopPos - 60, 8, 12, Component.literal("-"), button -> {
            this.returnTime = merchant.getReturningTime();
            if (this.returnTime > 0) {
                this.returnTime--;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantReturnTime(this.returnTime, merchant.getUUID()));
            }
        }));
    }
    private void createCreativeButton(int x, int y) {
        addRenderableWidget(new Button(x, y, 41, 20, Component.literal("Creative"),
            button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantSetCreative(merchant.getUUID(), !merchant.isCreative()));
        }));
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        returnTime = merchant.getReturningTime();
        font.draw(matrixStack, merchant.getDisplayName().getVisualOrderText(), 8, 6, FONT_COLOR);
        font.draw(matrixStack, playerInventory.getDisplayName().getVisualOrderText(), 8, imageHeight - 152 + 59, FONT_COLOR);
        int currentWayPoint =  merchant.getCurrentWayPointIndex();
        int currentReturningTime = merchant.getCurrentReturningTime();

        if(waypoints != null) {
            font.draw(matrixStack, "WP: " + currentWayPoint + "/" + waypoints.size(), 70, 6, FONT_COLOR);
            for (int i = 0; i < waypoints.size(); i++) {
                BlockPos pos = waypoints.get(i);
                font.draw(matrixStack, i + ": " + " x: " + pos.getX() + " y: " + pos.getY() + " z: " + pos.getZ(), 170, i * 13, FONT_COLOR_WHITE);
            }
        }
        font.draw(matrixStack, "Days: " + currentReturningTime +"/" + returnTime, 120, 6, FONT_COLOR);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
    }
}
