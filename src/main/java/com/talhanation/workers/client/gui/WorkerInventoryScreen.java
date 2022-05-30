package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.workers.Main;
import com.talhanation.workers.inventory.WorkerInventoryContainer;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.network.MessageCampPos;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public class WorkerInventoryScreen extends ScreenBase<WorkerInventoryContainer> {

    private static final ResourceLocation GUI_TEXTURE_3 = new ResourceLocation(Main.MOD_ID,"textures/gui/worker_gui.png");

    private final AbstractWorkerEntity worker;
    private final Inventory playerInventory;

    public WorkerInventoryScreen(WorkerInventoryContainer container, Inventory playerInventory, Component title) {
        super(GUI_TEXTURE_3, container, playerInventory, new TextComponent(""));
        this.worker = container.getWorker();
        this.playerInventory = playerInventory;

        imageWidth = 176;
        imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        //HOME POS
        addRenderableWidget(new Button(leftPos + 60, topPos + 60, 12, 12, new TextComponent("Home"), button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageCampPos(worker.getUUID(), worker.getWorkerOnPos()));
        }));
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        font.draw(matrixStack, worker.getDisplayName().getVisualOrderText(), 8, 6, FONT_COLOR);
        font.draw(matrixStack, playerInventory.getDisplayName().getVisualOrderText(), 8, imageHeight - 152 + 3, FONT_COLOR);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
    }
}
