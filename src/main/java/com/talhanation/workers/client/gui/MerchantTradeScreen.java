package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.inventory.MerchantTradeContainer;
import com.talhanation.workers.network.MessageMerchantTradeButton;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class MerchantTradeScreen extends ScreenBase<MerchantTradeContainer> {

    private static final ResourceLocation GUI_TEXTURE_3 = new ResourceLocation(Main.MOD_ID,
            "textures/gui/merchant_gui.png");

    private final MerchantEntity merchant;
    private final Inventory playerInventory;

    private final MutableComponent TRADE_TEXT = new TranslatableComponent("gui.workers.merchant.trade");

    public MerchantTradeScreen(MerchantTradeContainer container, Inventory playerInventory, Component title) {
        super(GUI_TEXTURE_3, container, playerInventory, new TextComponent(""));
        this.merchant = (MerchantEntity) container.getWorker();
        this.playerInventory = playerInventory;

        imageWidth = 176;
        imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        // CAMP POS

        addRenderableWidget(new ExtendedButton(leftPos + 110, topPos + 20 + 18 * 0, 48, 12, TRADE_TEXT, button -> {
            Main.SIMPLE_CHANNEL
                    .sendToServer(new MessageMerchantTradeButton(merchant.getUUID(), playerInventory.player.getUUID(), 0));
        }));

        addRenderableWidget(new ExtendedButton(leftPos + 110, topPos + 20 + 18 * 1, 48, 12, TRADE_TEXT, button -> {
            Main.SIMPLE_CHANNEL
                    .sendToServer(new MessageMerchantTradeButton(merchant.getUUID(), playerInventory.player.getUUID(), 1));
        }));

        addRenderableWidget(new ExtendedButton(leftPos + 110, topPos + 20 + 18 * 2, 48, 12, TRADE_TEXT, button -> {
            Main.SIMPLE_CHANNEL
                    .sendToServer(new MessageMerchantTradeButton(merchant.getUUID(), playerInventory.player.getUUID(), 2));
        }));

        addRenderableWidget(new ExtendedButton(leftPos + 110, topPos + 20 + 18 * 3, 48, 12, TRADE_TEXT, button -> {
            Main.SIMPLE_CHANNEL
                    .sendToServer(new MessageMerchantTradeButton(merchant.getUUID(), playerInventory.player.getUUID(), 3));
        }));
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);

        font.draw(matrixStack, merchant.getDisplayName().getVisualOrderText(), 8, 6, FONT_COLOR);
        matrixStack.popPose();

        font.draw(matrixStack, playerInventory.getDisplayName().getVisualOrderText(), 8, imageHeight - 152 + 25,
                FONT_COLOR);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
    }
}
