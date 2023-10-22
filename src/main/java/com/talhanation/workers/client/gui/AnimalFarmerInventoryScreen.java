package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.workers.Main;
import com.talhanation.workers.Translatable;
import com.talhanation.workers.entities.AbstractAnimalFarmerEntity;
import com.talhanation.workers.entities.ChickenFarmerEntity;
import com.talhanation.workers.inventory.WorkerInventoryContainer;
import com.talhanation.workers.network.MessageAnimalCount;
import com.talhanation.workers.network.MessageChickenFarmerUseEggs;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class AnimalFarmerInventoryScreen extends WorkerInventoryScreen {

    private final AbstractAnimalFarmerEntity animalFarmer;
    private int count;
    private boolean useEggs;

    private static final int fontColor = 4210752;

    public AnimalFarmerInventoryScreen(WorkerInventoryContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory,  new TextComponent(""));
        this.animalFarmer = (AbstractAnimalFarmerEntity) container.getWorker();
    }

    @Override
    protected void init() {
        super.init();

        this.setButtons();
    }

    private void setButtons(){
        this.clearWidgets();
        // Count
        addRenderableWidget(new Button(leftPos + 10, topPos + 60, 8, 12,  new TextComponent("<"), button -> {
            this.count = animalFarmer.getMaxAnimalCount();
            if (this.count != 0) {
                this.count--;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAnimalCount(this.count, animalFarmer.getUUID()));
                this.setButtons();
            }
        }));

        addRenderableWidget(new Button(leftPos + 10 + 30, topPos + 60, 8, 12,  new TextComponent(">"), button -> {
            this.count = animalFarmer.getMaxAnimalCount();
            if (this.count != 32) {
                this.count++;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAnimalCount(this.count, animalFarmer.getUUID()));
                this.setButtons();
            }
        }));

        if(animalFarmer instanceof ChickenFarmerEntity){
            String string = useEggs ? "True" : "False";
            setUseEggsButton(string);
        }
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        int k = 79;// right left
        int l = 19;// hight

        String count = String.valueOf(animalFarmer.getMaxAnimalCount());
        font.draw(matrixStack, MAX_ANIMALS.getString() + ":", k - 60, l + 35, fontColor);
        font.draw(matrixStack, count, k - 55, l + 45, fontColor);
    }

    private final MutableComponent MAX_ANIMALS = new TranslatableComponent("gui.workers.shepherd.max_animals");

    private void setUseEggsButton(String string) {
        addRenderableWidget(new Button(leftPos + 190, topPos + 57, 40, 20,  new TextComponent(string), button -> {
            this.useEggs = !useEggs;

            Main.SIMPLE_CHANNEL.sendToServer(new MessageChickenFarmerUseEggs(animalFarmer.getUUID(), useEggs));
            this.setButtons();
        },
            (button1, poseStack, i, i1) -> {
                this.renderTooltip(poseStack, Translatable.TOOLTIP_FARMER_USE_EGGS, i, i1);
            }));
    }

}
