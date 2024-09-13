package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.inventory.MerchantInventoryContainer;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MerchantInventoryScreen extends ScreenBase<MerchantInventoryContainer> {
    protected static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID,
            "textures/gui/worker_gui_4x.png");
    private final MerchantEntity merchant;
    private final Inventory playerInventory;
    private static final int fontColor = 4210752;

    public MerchantInventoryScreen(MerchantInventoryContainer container, Inventory playerInventory, Component title) {
        super(TEXTURE, container, playerInventory,  new TextComponent(""));
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
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        font.draw(matrixStack, merchant.getDisplayName().getVisualOrderText(), 8, 6, FONT_COLOR);
        font.draw(matrixStack, playerInventory.getDisplayName().getVisualOrderText(), 8, imageHeight - 152 + 60, FONT_COLOR);
    }
}
