package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.inventory.WorkerHireContainer;
import com.talhanation.workers.network.MessageHire;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.text.DecimalFormat;

@OnlyIn(Dist.CLIENT)
public class WorkerHireScreen extends ScreenBase<WorkerHireContainer> {
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,
            "textures/gui/hire_gui.png");

    private static final MutableComponent TEXT_HIRE = new TranslatableComponent("gui.workers.hire_gui.text.hire");

    private static final int fontColor = 4210752;

    private final AbstractWorkerEntity worker;
    private final Player player;
    private EditBox textField;

    public static ItemStack currency;
    public static int amount;

    public WorkerHireScreen(WorkerHireContainer recruitContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, recruitContainer, playerInventory, new TextComponent(""));
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

        if(currency != null) currency.setCount(amount);

        int mirror = 240 - 60;

        clearWidgets();
        setEditBox();

        addRenderableWidget(new Button(zeroLeftPos - mirror + 40, zeroTopPos + 85, 100, 20, TEXT_HIRE, button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageHire(player.getUUID(), worker.getUUID(), textField.getValue()));
            this.onClose();
        }));
    }
    private void setEditBox() {
        minecraft.keyboardHandler.setSendRepeatsToGui(true);
        Component name = worker.getName();

        textField = new EditBox(font, leftPos + 3, topPos + 3, 90, 12, name);
        textField.setValue(name.getString());
        textField.setTextColor(-1);
        textField.setTextColorUneditable(-1);
        textField.setBordered(true);
        textField.setFocus(true);
        textField.setMaxLength(16);

        addRenderableWidget(textField);
        setInitialFocus(textField);
    }

    protected void containerTick() {
        super.containerTick();
        if(textField != null) textField.tick();
    }

    public boolean mouseClicked(double p_100753_, double p_100754_, int p_100755_) {
        if (this.textField != null && this.textField.isFocused()) {
            this.textField.mouseClicked(p_100753_, p_100754_, p_100755_);
        }
        return super.mouseClicked(p_100753_, p_100754_, p_100755_);
    }
    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        int health = Mth.ceil(worker.getHealth());
        int maxHealth = Mth.ceil(worker.getMaxHealth());
        int hunger = Mth.ceil(worker.getHunger());

        double speed = worker.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) / 0.3;
        DecimalFormat decimalformat = new DecimalFormat("##.##");

        int k = 89;// rechst links
        int l = 19;// höhe

        // Titles
        //font.draw(matrixStack, worker.getDisplayName().getVisualOrderText(), 8, 5, fontColor);
        font.draw(matrixStack, player.getInventory().getDisplayName().getVisualOrderText(), 8, this.imageHeight - 96 + 2, fontColor);

        // Info
        font.draw(matrixStack, "Hp:", k, l, fontColor);
        font.draw(matrixStack, "" + health, k + 40, l, fontColor);

        font.draw(matrixStack, "MaxHp:", k, l + 10, fontColor);
        font.draw(matrixStack, "" + maxHealth, k + 40, l + 10, fontColor);

        font.draw(matrixStack, "Speed:", k, l + 20, fontColor);
        font.draw(matrixStack, "" + decimalformat.format(speed), k + 40, l + 20, fontColor);

        font.draw(matrixStack, "Hunger:", k, l + 30, fontColor);
        font.draw(matrixStack, "" + hunger, k + 40, l + 30, fontColor);

        if(currency != null){
            itemRenderer.renderGuiItem(currency, 120, this.imageHeight - 122);
            itemRenderer.renderGuiItemDecorations(font, currency, 120, this.imageHeight - 122);
        }
    }

    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);

        RenderSystem.clearColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        InventoryScreen.renderEntityInInventory(i + 40, j + 72, 20, (float) (i + 50) - mouseX,
                (float) (j + 75 - 50) - mouseY, this.worker);
    }
}
