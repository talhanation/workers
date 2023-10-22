package com.talhanation.workers.client.gui;

import com.talhanation.workers.Main;
import com.talhanation.workers.Translatable;
import com.talhanation.workers.entities.AbstractAnimalFarmerEntity;
import com.talhanation.workers.entities.ChickenFarmerEntity;
import com.talhanation.workers.inventory.WorkerInventoryContainer;
import com.talhanation.workers.network.MessageAnimalCount;
import com.talhanation.workers.network.MessageChickenFarmerUseEggs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class AnimalFarmerInventoryScreen extends WorkerInventoryScreen {

    private final AbstractAnimalFarmerEntity animalFarmer;
    private int count;
    private boolean useEggs;

    public AnimalFarmerInventoryScreen(WorkerInventoryContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, Component.literal(""));
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
        addRenderableWidget(new ExtendedButton(leftPos + 10, topPos + 60, 8, 12, Component.literal("<"), button -> {
            this.count = animalFarmer.getMaxAnimalCount();
            if (this.count != 0) {
                this.count--;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAnimalCount(this.count, animalFarmer.getUUID()));
                this.setButtons();
            }
        }));

        addRenderableWidget(new ExtendedButton(leftPos + 10 + 30, topPos + 60, 8, 12, Component.literal(">"), button -> {
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
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        int k = 79;// right left
        int l = 19;// hight

        String count = String.valueOf(animalFarmer.getMaxAnimalCount());
        guiGraphics.drawString(font, MAX_ANIMALS.getString() + ":", k - 60, l + 35, fontColor, false);
        guiGraphics.drawString(font, count, k - 55, l + 45, fontColor, false);
    }

    private final MutableComponent MAX_ANIMALS = Component.translatable("gui.workers.shepherd.max_animals");

    private void setUseEggsButton(String string) {
        ExtendedButton button = addRenderableWidget(new ExtendedButton(leftPos + 190, topPos + 57, 40, 20, Component.literal(string), button1 -> {
            this.useEggs = !useEggs;

            Main.SIMPLE_CHANNEL.sendToServer(new MessageChickenFarmerUseEggs(animalFarmer.getUUID(), useEggs));
            this.setButtons();
        }));
        button.setTooltip(Tooltip.create(Translatable.TOOLTIP_FARMER_USE_EGGS));
    }

}
