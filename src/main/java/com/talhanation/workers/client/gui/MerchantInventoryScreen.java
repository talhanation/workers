package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.inventory.MerchantInventoryContainer;
import com.talhanation.workers.inventory.MerchantOwnerContainer;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MerchantInventoryScreen extends ScreenBase<MerchantInventoryContainer> {
    protected static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID,
            "textures/gui/worker_gui_4x.png");
    private final MerchantEntity merchant;
    private final Inventory playerInventory;
    private static final int fontColor = 4210752;

    public MerchantInventoryScreen(MerchantInventoryContainer container, Inventory playerInventory, Component title) {
        super(TEXTURE, container, playerInventory,  Component.literal(""));
        this.merchant = (MerchantEntity) container.getWorker();
        this.playerInventory = playerInventory;
        this.imageHeight = 184;
        this.imageWidth = 176;
    }

    @Override
    protected void init() {
        super.init();

    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(font, merchant.getDisplayName().getVisualOrderText(), 8, 6, FONT_COLOR, false);
        guiGraphics.drawString(font, playerInventory.getDisplayName().getVisualOrderText(), 8, imageHeight - 152 + 60, FONT_COLOR, false);
    }
}
