package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.talhanation.workers.Main;
import com.talhanation.workers.WorkerInventoryContainer;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class WorkerInventoryScreen extends ScreenBase<WorkerInventoryContainer> {

    private static final ResourceLocation GUI_TEXTURE_3 = new ResourceLocation(Main.MOD_ID,"textures/gui/worker_gui.png");

    private final AbstractWorkerEntity worker;
    private final PlayerInventory playerInventory;

    public WorkerInventoryScreen(WorkerInventoryContainer container, PlayerInventory playerInventory, ITextComponent title) {
        super(GUI_TEXTURE_3, container, playerInventory, title);
        this.worker = container.getWorker();
        this.playerInventory = playerInventory;

        imageWidth = 176;
        imageHeight = 222;
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        font.draw(matrixStack, worker.getDisplayName().getVisualOrderText(), 8, 6, FONT_COLOR);
        font.draw(matrixStack, playerInventory.getDisplayName().getVisualOrderText(), 8, imageHeight - 152 + 3, FONT_COLOR);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
