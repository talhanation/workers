package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.ShepherdEntity;
import com.talhanation.workers.inventory.WorkerInventoryContainer;
import com.talhanation.workers.network.MessageSheepCount;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public class ShepherdInventoryScreen extends WorkerInventoryScreen{

    private final ShepherdEntity shepherd;
    private int count;

    private static final int fontColor = 4210752;

    public ShepherdInventoryScreen(WorkerInventoryContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, new TextComponent(""));
        this.shepherd = (ShepherdEntity) container.getWorker();
    }

    @Override
    protected void init(){
        super.init();
        //Count
        addRenderableWidget(new Button(leftPos + 10, topPos + 60, 8, 12, new TextComponent("<"), button -> {
                this.count = shepherd.getMaxSheepCount();
                if (this.count != 0) {
                    this.count--;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageSheepCount(this.count, shepherd.getUUID()));
                }
        }));

        addRenderableWidget(new Button(leftPos + 10 + 30, topPos + 60, 8, 12, new TextComponent(">"), button -> {
                this.count = shepherd.getMaxSheepCount();
                if (this.count != 32) {
                    this.count++;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageSheepCount(this.count, shepherd.getUUID()));
                }
        }));
    }


    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        int k = 79;//right left
        int l = 19;//hight

        String count = String.valueOf(shepherd.getMaxSheepCount());
        font.draw(matrixStack,MAX_SHEEPS.getString() + ":", k - 60, l + 35, fontColor);
        font.draw(matrixStack, count, k - 55, l + 45, fontColor);
    }

    private final TranslatableComponent MAX_SHEEPS = new TranslatableComponent("gui.workers.shepherd.max_sheeps");


}
