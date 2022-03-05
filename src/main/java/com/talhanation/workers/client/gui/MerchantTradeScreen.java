package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.inventory.MerchantTradeContainer;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class MerchantTradeScreen extends ScreenBase<MerchantTradeContainer> {

    private static final ResourceLocation GUI_TEXTURE_3 = new ResourceLocation(Main.MOD_ID,"textures/gui/merchant_gui.png");

    private final MerchantEntity merchant;
    private final PlayerInventory playerInventory;

    public MerchantTradeScreen(MerchantTradeContainer container, PlayerInventory playerInventory, ITextComponent title) {
        super(GUI_TEXTURE_3, container, playerInventory, title);
        this.merchant = (MerchantEntity) container.getWorker();
        this.playerInventory = playerInventory;

        imageWidth = 176;
        imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        //CAMP POS

        addButton(new Button(leftPos + 110, topPos + 20 + 18 * 0, 48, 12, new StringTextComponent("Trade"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageTradeButton(merchant.getUUID(), playerInventory.player.getUUID(), 0));
        }));

        addButton(new Button(leftPos + 110, topPos + 20 + 18 * 1, 48, 12, new StringTextComponent("Trade"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageTradeButton(merchant.getUUID(), playerInventory.player.getUUID(), 1));
        }));

        addButton(new Button(leftPos + 110, topPos + 20 + 18 * 2, 48, 12, new StringTextComponent("Trade"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageTradeButton(merchant.getUUID(), playerInventory.player.getUUID(), 2));
        }));

        addButton(new Button(leftPos + 110, topPos + 20 + 18 * 3, 48, 12, new StringTextComponent("Trade"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageTradeButton(merchant.getUUID(), playerInventory.player.getUUID(), 3));
        }));
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        font.draw(matrixStack, merchant.getDisplayName().getVisualOrderText(), 8, 6, FONT_COLOR);
        font.draw(matrixStack, playerInventory.getDisplayName().getVisualOrderText(), 8, imageHeight - 152 + 25, FONT_COLOR);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected void renderBg(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
