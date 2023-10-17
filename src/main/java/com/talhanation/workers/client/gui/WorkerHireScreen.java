package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.inventory.WorkerHireContainer;
import com.talhanation.workers.network.MessageHire;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.text.DecimalFormat;

@OnlyIn(Dist.CLIENT)
public class WorkerHireScreen extends ScreenBase<WorkerHireContainer> {
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,
            "textures/gui/hire_gui.png");

    private static final MutableComponent TEXT_HIRE = Component.translatable("gui.workers.hire_gui.text.hire");

    private static final int fontColor = 4210752;

    private final AbstractWorkerEntity worker;
    private final Player player;

    public WorkerHireScreen(WorkerHireContainer recruitContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, recruitContainer, playerInventory, Component.literal(""));
        this.worker = recruitContainer.getWorkerEntity();
        this.player = playerInventory.player;
        imageWidth = 176;
        imageHeight = 218;
        Main.LOGGER.info("WorkerHireScreen loaded");
    }

    @Override
    protected void init() {
        super.init();
        int zeroLeftPos = leftPos + 180;
        int zeroTopPos = topPos + 10;

        int mirror = 240 - 60;
        addRenderableWidget(new ExtendedButton(zeroLeftPos - mirror + 40, zeroTopPos + 85, 100, 20, TEXT_HIRE, button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageHire(player.getUUID(), worker.getUUID()));
            this.onClose();
        }));

    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        int health = Mth.ceil(worker.getHealth());
        int maxHealth = Mth.ceil(worker.getMaxHealth());
        int hunger = Mth.ceil(worker.getHunger());

        double speed = worker.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) / 0.3;
        DecimalFormat decimalformat = new DecimalFormat("##.##");
        int costs = worker.workerCosts();

        int k = 89;// rechst links
        int l = 19;// höhe

        // Titles
        guiGraphics.drawString(font, worker.getDisplayName().getVisualOrderText(), 8, 5, fontColor, false);
        guiGraphics.drawString(font, player.getInventory().getDisplayName().getVisualOrderText(), 8, this.imageHeight - 96 + 2, fontColor, false);

        // Info
        guiGraphics.drawString(font, "Hp:", k, l, fontColor, false);
        guiGraphics.drawString(font, "" + health, k + 40, l, fontColor, false);

        guiGraphics.drawString(font, "MaxHp:", k, l + 10, fontColor, false);
        guiGraphics.drawString(font, "" + maxHealth, k + 40, l + 10, fontColor, false);

        guiGraphics.drawString(font, "Speed:", k, l + 20, fontColor, false);
        guiGraphics.drawString(font, "" + decimalformat.format(speed), k + 40, l + 20, fontColor, false);

        guiGraphics.drawString(font, "Hunger:", k, l + 30, fontColor, false);
        guiGraphics.drawString(font, "" + hunger, k + 40, l + 30, fontColor, false);

        guiGraphics.drawString(font, "Costs:", k, l + 40, fontColor, false);
        guiGraphics.drawString(font, "" + costs, k + 40, l + 40, fontColor, false);

    }

    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTicks, mouseX, mouseY);

        RenderSystem.clearColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, i + 40, j + 72, 20, (float) (i + 50) - mouseX,
                (float) (j + 75 - 50) - mouseY, this.worker);
    }
}
