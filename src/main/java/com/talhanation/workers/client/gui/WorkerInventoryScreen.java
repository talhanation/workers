package com.talhanation.workers.client.gui;

import com.talhanation.workers.Main;
import com.talhanation.workers.inventory.WorkerInventoryContainer;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

public class WorkerInventoryScreen extends ScreenBase<WorkerInventoryContainer> {

    private static final ResourceLocation GUI_TEXTURE_3 = new ResourceLocation(Main.MOD_ID,
            "textures/gui/worker_gui.png");
    protected static final int fontColor = 4210752;
    private final AbstractWorkerEntity worker;
    private final Inventory playerInventory;

    public WorkerInventoryScreen(WorkerInventoryContainer container, Inventory playerInventory, Component title) {
        super(GUI_TEXTURE_3, container, playerInventory, Component.literal(""));
        Main.LOGGER.info("WorkerInventoryScreen loaded");
        this.worker = container.getWorker();
        this.playerInventory = playerInventory;

        imageWidth = 176;
        imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        /*
        // HOME POS
        addRenderableWidget(new Button(leftPos + 60, topPos + 60, 12, 12, Component.literal("Home"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(
                    new MessageHomePos(playerInventory.player.getUUID(), worker.getUUID(), worker.getWorkerOnPos()));
            Main.LOGGER.debug("Screen: " + worker.getWorkerOnPos().toShortString());
        }));
         */
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(font, worker.getDisplayName().getVisualOrderText(), 8, 6, fontColor, false);
        guiGraphics.drawString(font, playerInventory.getDisplayName().getVisualOrderText(), 8, imageHeight - 152 + 3, fontColor, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTicks, mouseX, mouseY);
    }
}
