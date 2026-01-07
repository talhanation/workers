package com.talhanation.workers.client.gui;

import com.talhanation.workers.entities.workarea.FishingArea;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;


public class FishingAreaScreen extends WorkAreaScreen {
    private static final MutableComponent TEXT_SHEAR_LEAVES = Component.translatable("gui.workers.checkbox.shearLeaves");
    public final FishingArea fishingArea;
    public FishingAreaScreen(FishingArea fishingArea, Player player) {
        super(fishingArea.getCustomName(), fishingArea, player);
        this.fishingArea = fishingArea;
    }

    @Override
    protected void init() {
        setButtons();
    }

    @Override
    public void setButtons() {
        super.setButtons();
        int checkBoxWidth = 100;
        int checkBoxHeight = 20;

        int checkBoxX = x - checkBoxWidth / 2;
        int checkBoxY = y + checkBoxHeight / 2 - checkBoxHeight;

    }

}
