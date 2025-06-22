package com.talhanation.workers.client.gui;

import com.talhanation.recruits.client.gui.widgets.ScrollDropDownMenu;
import com.talhanation.workers.Main;
import com.talhanation.workers.client.gui.widgets.ItemScrollDropDownMenu;
import com.talhanation.workers.entities.workarea.CropArea;
import com.talhanation.workers.network.MessageUpdateCropArea;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;

import java.util.ArrayList;
import java.util.List;

public class CropAreaScreen extends WorkAreaScreen {

    public final CropArea cropArea;
    private ScrollDropDownMenu<ItemStack> groupSelectionDropDownMenu;
    private ItemStack currentSeeds;
    private List<ItemStack> possibleSeeds;

    public CropAreaScreen(CropArea cropArea, Player player) {
        super(cropArea.getCustomName(), cropArea, player);
        this.cropArea = cropArea;
        this.currentSeeds = cropArea.getSeedStack();
    }
    boolean initial;
    @Override
    public void tick() {
        super.tick();
        if(possibleSeeds == null && !initial){
            this.possibleSeeds = getPossibleSeedsFromInventory();

            groupSelectionDropDownMenu = new ItemScrollDropDownMenu(currentSeeds, guiLeft + 100,guiTop + ySize - 100,  100, possibleSeeds, this::setCurrentSeeds);

            groupSelectionDropDownMenu.setBgFillSelected(FastColor.ARGB32.color(255, 139, 139, 139));
            //groupSelectionDropDownMenu.visible = Minecraft.getInstance().player.getUUID().equals(recruit.getOwnerUUID());
            addRenderableWidget(groupSelectionDropDownMenu);
            this.initial = true;
        }
    }

    public void setCurrentSeeds(ItemStack currentSeeds) {
        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateCropArea(this.cropArea.getUUID(), currentSeeds));
        this.currentSeeds = currentSeeds;
    }

    private List<ItemStack> getPossibleSeedsFromInventory() {
        List<Item> items = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack itemStack : player.getInventory().items) {
            if (itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof CropBlock) {
                if(!items.contains(itemStack.getItem())){
                    stacks.add(itemStack);
                    items.add(itemStack.getItem());
                }
            }
        }
        return stacks;
    }

    @Override
    public void mouseMoved(double x, double y) {
        if(groupSelectionDropDownMenu != null){
            groupSelectionDropDownMenu.onMouseMove(x,y);
        }
        super.mouseMoved(x, y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (groupSelectionDropDownMenu != null && groupSelectionDropDownMenu.isMouseOver(mouseX, mouseY)) {
            groupSelectionDropDownMenu.onMouseClick(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override
    public boolean mouseScrolled(double x, double y, double d) {
        if(groupSelectionDropDownMenu != null) groupSelectionDropDownMenu.mouseScrolled(x,y,d);
        return super.mouseScrolled(x, y, d);
    }
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (groupSelectionDropDownMenu != null) {
            groupSelectionDropDownMenu.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
        }
    }
}
