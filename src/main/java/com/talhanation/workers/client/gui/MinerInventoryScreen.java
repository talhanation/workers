package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.talhanation.workers.Main;
import com.talhanation.workers.WorkerInventoryContainer;
import com.talhanation.workers.entities.MinerEntity;
import com.talhanation.workers.network.MessageMineDepth;
import com.talhanation.workers.network.MessageMineType;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class MinerInventoryScreen extends WorkerInventoryScreen{

    private MinerEntity miner;
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
        addButton(new Button(leftPos + 77, topPos + 55, 8, 12, new StringTextComponent("<"), button -> {
            this.mineType = miner.getMineType();
            if (this.mineType != 0) {
                this.mineType--;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMineType(this.mineType, miner.getUUID()));
            }
        }));

        addButton(new Button(leftPos + 77 + 85, topPos + 55, 8, 12, new StringTextComponent(">"), button -> {
            this.mineType = miner.getMineType();
            if (this.mineType != 3) {
                this.mineType++;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMineType(this.mineType, miner.getUUID()));
            }
        }));

        //MINEDEPTH
        addButton(new Button(leftPos + 33, topPos + 55, 8, 12, new StringTextComponent("<"), button -> {
            if (miner.getMineType() != 3){
                this.mineDepth = miner.getMineType();
                if (this.mineDepth != 0) {
                    this.mineDepth--;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMineDepth(this.mineDepth, miner.getUUID()));
                }
            }
        }));

        addButton(new Button(leftPos + 77 + 44, topPos + 55, 8, 12, new StringTextComponent(">"), button -> {
            if (miner.getMineType() != 3) {
                this.mineDepth = miner.getMineDepth();
                if (this.mineDepth != 16) {
                    this.mineDepth++;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMineDepth(this.mineDepth, miner.getUUID()));
                }
            }
        }));
    }


    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        int k = 79;//rechst links
        int l = 19;//höhe

        String depth;

        if (miner.getMineType() != 3){
            depth = String.valueOf(miner.getMineDepth());
        }
        else depth = "";
        font.draw(matrixStack, depth, k + 15, l + 20 + 0, fontColor);

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
                type = "8x8 Pit";
                break;
        }
        font.draw(matrixStack, type, k + 15, l + 40 + 0, fontColor);
    }


}
