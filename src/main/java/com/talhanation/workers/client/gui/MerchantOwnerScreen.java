package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.inventory.MerchantInventoryContainer;
import com.talhanation.workers.network.MessageHire;
import com.talhanation.workers.network.MessageMerchantAddWayPoint;
import com.talhanation.workers.network.MessageMerchantRemoveWayPoint;
import com.talhanation.workers.network.MessageTravel;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

import java.util.List;

public class MerchantOwnerScreen extends ScreenBase<MerchantInventoryContainer> {

    private static final ResourceLocation GUI_TEXTURE_3 = new ResourceLocation(Main.MOD_ID,
            "textures/gui/merchant_owner_gui.png");

    private final MerchantEntity merchant;
    private final Inventory playerInventory;

    public MerchantOwnerScreen(MerchantInventoryContainer container, Inventory playerInventory, Component title) {
        super(GUI_TEXTURE_3, container, playerInventory, Component.literal(""));
        this.merchant = (MerchantEntity) container.getWorker();
        this.playerInventory = playerInventory;

        imageWidth = 176;
        imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        int zeroLeftPos = leftPos + 180;
        int zeroTopPos = topPos + 10;

        int mirror = 240 - 60;

        addRenderableWidget(new Button(zeroLeftPos - mirror + 170, zeroTopPos + 85, 41, 20, Component.literal("Travel"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageTravel(merchant.getUUID()));

        }));

        addRenderableWidget(new Button(zeroLeftPos - mirror + 170, zeroTopPos + 106, 20, 20, Component.literal("+"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantAddWayPoint(merchant.getUUID()));

        }));

        addRenderableWidget(new Button(zeroLeftPos - mirror + 191, zeroTopPos + 106, 20, 20, Component.literal("-"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMerchantRemoveWayPoint(merchant.getUUID()));

        }));
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        font.draw(matrixStack, merchant.getDisplayName().getVisualOrderText(), 8, 6, FONT_COLOR);
        font.draw(matrixStack, playerInventory.getDisplayName().getVisualOrderText(), 8, imageHeight - 152 + 59, FONT_COLOR);

        List<BlockPos> list = merchant.WAYPOINTS;
        for(int i = 0; i < list.size(); i++){
            BlockPos pos = list.get(i);

            font.draw(matrixStack,i + ": " + "x: " +  pos.getX() + "y: " +  pos.getY() + "z: " +  pos.getZ(), 30, 6 + i * 20, FONT_COLOR);
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
