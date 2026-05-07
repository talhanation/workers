package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.world.WorkersMerchantTrade;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

@OnlyIn(Dist.CLIENT)
public class MerchantSelectTradeTypeScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_small.png");
    private static final MutableComponent BUTTON_PLAYER_TRADE  = Component.translatable("gui.workers.button.playerTrade");
    private static final MutableComponent BUTTON_VILLAGER_TRADE = Component.translatable("gui.workers.button.villagerTrade");
    private static final int fontColor = 4210752;
    private final MerchantEntity merchantEntity;
    private final Player player;

    public MerchantSelectTradeTypeScreen(MerchantEntity merchantEntity, Player player) {
        super(Component.literal("Selection"),246,84);
        this.merchantEntity = merchantEntity;
        this.player         = player;
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(new ExtendedButton(guiLeft + 10, guiTop + 30, 100, 20, BUTTON_PLAYER_TRADE,
                btn -> merchantEntity.openAddEditTradeGUI(player, new WorkersMerchantTrade())));

        addRenderableWidget(new ExtendedButton(guiLeft + 130, guiTop + 30, 100, 20, BUTTON_VILLAGER_TRADE,
                btn -> merchantEntity.openVillagerTradeGUI(player, new WorkersMerchantTrade())));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.renderBackground(guiGraphics, mouseX, mouseY, delta);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.drawString(font, title, guiLeft + 8, guiTop + 6, fontColor, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
