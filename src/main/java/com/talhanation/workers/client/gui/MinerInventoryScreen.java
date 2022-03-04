package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.talhanation.workers.Main;
import com.talhanation.workers.inventory.WorkerInventoryContainer;
import com.talhanation.workers.entities.MinerEntity;
import com.talhanation.workers.network.MessageMineDepth;
import com.talhanation.workers.network.MessageMineType;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class MinerInventoryScreen extends WorkerInventoryScreen{

    private final MinerEntity miner;
    private int mineType;
    private int mineDepth;

    private static final int fontColor = 4210752;

    public MinerInventoryScreen(WorkerInventoryContainer container, PlayerInventory playerInventory, ITextComponent title) {
        super(container, playerInventory, title);
        this.miner = (MinerEntity) container.getWorker();
    }

    @Override
    protected void init(){
        super.init();
        //MINETYPE
        addButton(new Button(leftPos + 90, topPos + 60, 8, 12, new StringTextComponent("<"), button -> {
            this.mineType = miner.getMineType();
            if (this.mineType != 0) {
                this.mineType--;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMineType(this.mineType, miner.getUUID()));
            }
        }));

        addButton(new Button(leftPos + 90 + 70, topPos + 60, 8, 12, new StringTextComponent(">"), button -> {
            this.mineType = miner.getMineType();
            if (this.mineType != 4) {
                this.mineType++;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMineType(this.mineType, miner.getUUID()));
            }
        }));

        //MINEDEPTH
        addButton(new Button(leftPos + 10, topPos + 60, 8, 12, new StringTextComponent("<"), button -> {
            if (miner.getMineType() != 3){
                this.mineDepth = miner.getMineDepth();
                if (this.mineDepth != 0) {
                    this.mineDepth--;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMineDepth(this.mineDepth, miner.getUUID()));
                }
            }
        }));

        addButton(new Button(leftPos + 10 + 30, topPos + 60, 8, 12, new StringTextComponent(">"), button -> {
            if (miner.getMineType() != 3) {
                this.mineDepth = miner.getMineDepth();
                if (this.mineDepth != miner.getMaxMineDepth()) {
                    this.mineDepth++;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMineDepth(this.mineDepth, miner.getUUID()));
                }
            }
        }));
    }


    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        int k = 79;//right left
        int l = 19;//hight

        String depth;

        if (miner.getMineType() == 3 || miner.getMineType() == 4){
            depth = "-";
        }
        else depth = String.valueOf(miner.getMineDepth());
        font.draw(matrixStack, "Depth:", k - 70, l + 35, fontColor);
        font.draw(matrixStack, depth, k - 55, l + 45, fontColor);

        String type;
        switch (miner.getMineType()){
            default:
            case 0:
                if (miner.getFollow())
                    type = "Following";
                else type = "Wandering";
                break;
            case 1:
                type = "1x1 Tunnel";
                break;
            case 2:
                type = "3x3 Tunnel";
                break;
            case 3:
                type = "8x8x8 Pit";
                break;
            case 4:
                type = "8x8x1 Flat";
                break;
        }
        font.draw(matrixStack, "Mine-Mode:", k + 25, l + 35, fontColor);
        font.draw(matrixStack, type, k + 25, l + 45, fontColor);
    }


}
