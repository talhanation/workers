package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.workers.Main;
import com.talhanation.workers.inventory.CommandContainer;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CommandScreen extends ScreenBase<CommandContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/command_gui.png");
    private static final int fontColor = 16250871;
    private Player player;

    public CommandScreen(CommandContainer commandContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, commandContainer, playerInventory, new TextComponent(""));
        imageWidth = 201;
        imageHeight = 170;
        player = playerInventory.player;
    }

    @Override
    public boolean keyReleased(int x, int y, int z) {
        super.keyReleased(x,y,z);
        this.onClose();
        return true;
    }

    @Override
    protected void init() {
        super.init();

        int zeroLeftPos = leftPos + 150;
        int zeroTopPos = topPos + 10;
        int topPosGab = 7;
        int mirror = 240 - 60;

        //addRenderableWidget()



    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
    }

    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
    }
}
