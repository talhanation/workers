package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.AbstractAnimalFarmerEntity;
import com.talhanation.workers.inventory.WorkerInventoryContainer;
import com.talhanation.workers.network.MessageAnimalCount;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public class AnimalFarmerInventoryScreen extends WorkerInventoryScreen{

    private final AbstractAnimalFarmerEntity animalFarmer;
    private int count;

    private static final int fontColor = 4210752;

    public AnimalFarmerInventoryScreen(WorkerInventoryContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, new TextComponent(""));
        this.animalFarmer = (AbstractAnimalFarmerEntity) container.getWorker();
    }

    @Override
    protected void init(){
        super.init();
        //Count
        addRenderableWidget(new Button(leftPos + 10, topPos + 60, 8, 12, new TextComponent("<"), button -> {
                this.count = animalFarmer.getMaxAnimalCount();
                if (this.count != 0) {
                    this.count--;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAnimalCount(this.count, animalFarmer.getUUID()));
                }
        }));

        addRenderableWidget(new Button(leftPos + 10 + 30, topPos + 60, 8, 12, new TextComponent(">"), button -> {
                this.count = animalFarmer.getMaxAnimalCount();
                if (this.count != 32) {
                    this.count++;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAnimalCount(this.count, animalFarmer.getUUID()));
                }
        }));
    }


    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        int k = 79;//right left
        int l = 19;//hight

        String count = String.valueOf(animalFarmer.getMaxAnimalCount());
        font.draw(matrixStack, MAX_ANIMALS.getString() + ":", k - 60, l + 35, fontColor);
        font.draw(matrixStack, count, k - 55, l + 45, fontColor);
    }

    private final TranslatableComponent MAX_ANIMALS = new TranslatableComponent("gui.workers.shepherd.max_animals");


}
