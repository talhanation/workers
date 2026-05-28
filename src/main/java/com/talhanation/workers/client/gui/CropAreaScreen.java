package com.talhanation.workers.client.gui;

import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.client.gui.widgets.ItemScrollDropDownMenu;
import com.talhanation.workers.compat.FarmersDelight;
import com.talhanation.workers.entities.workarea.CropArea;
import com.talhanation.workers.network.MessageUpdateCropArea;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;

import java.util.ArrayList;
import java.util.List;

public class CropAreaScreen extends WorkAreaScreen {

    public final CropArea cropArea;
    private ItemScrollDropDownMenu seedItemSelectionDropDownMenu;
    private ItemStack currentSeeds;
    private List<ItemStack> possibleSeeds;

    public CropAreaScreen(CropArea cropArea, Player player) {
        super(cropArea.getCustomName(), cropArea, player);
        this.cropArea = cropArea;
        this.currentSeeds = cropArea.getSeedStack();
    }

    @Override
    protected void init() {
        super.init();
        this.possibleSeeds = getPossibleSeedsFromInventory();

        setButtons();
    }

    @Override
    public void setButtons() {
        super.setButtons();

        int dropDownWidth = 200;
        int dropDownHeight = 20;

        seedItemSelectionDropDownMenu = new ItemScrollDropDownMenu(currentSeeds,x - dropDownWidth / 2, 50 + y + dropDownHeight / 2 - dropDownHeight, dropDownWidth, dropDownHeight, possibleSeeds, this::setCurrentSeeds);
        seedItemSelectionDropDownMenu.setBgFillSelected(FastColor.ARGB32.color(255, 139, 139, 139));

        addRenderableWidget(seedItemSelectionDropDownMenu);
    }


    public void setCurrentSeeds(ItemStack currentSeeds) {
        WorkersMain.SIMPLE_CHANNEL.sendToServer(new MessageUpdateCropArea(this.cropArea.getUUID(), currentSeeds));
        this.currentSeeds = currentSeeds;
    }

    private List<ItemStack> getPossibleSeedsFromInventory() {
        List<Item> items = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack itemStack : player.getInventory().items) {
            if (itemStack.isEmpty() || items.contains(itemStack.getItem())) continue;

            if (WorkersMain.isFarmersDelightInstalled && FarmersDelight.isRiceItem(itemStack)) {
                stacks.add(itemStack.copy());
                items.add(itemStack.getItem());
                continue;
            }

            if (itemStack.getItem() instanceof BlockItem blockItem) {

                if(blockItem.getBlock() instanceof SaplingBlock) continue;

                if (blockItem.getBlock() instanceof CropBlock || blockItem.getBlock() instanceof StemBlock || blockItem.getBlock() instanceof BushBlock){
                    stacks.add(itemStack.copy());
                    items.add(itemStack.getItem());
                }
            }
        }

        return stacks;
    }

    @Override
    public void mouseMoved(double x, double y) {
        if(seedItemSelectionDropDownMenu != null){
            seedItemSelectionDropDownMenu.onMouseMove(x,y);
        }
        super.mouseMoved(x, y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (seedItemSelectionDropDownMenu != null && seedItemSelectionDropDownMenu.isMouseOver(mouseX, mouseY)) {
            seedItemSelectionDropDownMenu.onMouseClick(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override
    public boolean mouseScrolled(double x, double y, double d) {
        if(seedItemSelectionDropDownMenu != null) seedItemSelectionDropDownMenu.mouseScrolled(x,y,d);
        return super.mouseScrolled(x, y, d);
    }
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (seedItemSelectionDropDownMenu != null) {
            seedItemSelectionDropDownMenu.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
        }
    }
}