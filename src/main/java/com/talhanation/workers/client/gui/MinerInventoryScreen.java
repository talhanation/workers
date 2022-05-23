package com.talhanation.workers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.workers.Main;
import com.talhanation.workers.inventory.WorkerInventoryContainer;
import com.talhanation.workers.entities.MinerEntity;
import com.talhanation.workers.network.MessageMineDepth;
import com.talhanation.workers.network.MessageMineType;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public class MinerInventoryScreen extends WorkerInventoryScreen{

    private final MinerEntity miner;
    private int mineType;
    private int mineDepth;

    private static final int fontColor = 4210752;

    public MinerInventoryScreen(WorkerInventoryContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title);
        this.miner = (MinerEntity) container.getWorker();
    }

    @Override
    protected void init(){
        super.init();
        //MINETYPE
        addRenderableWidget(new Button(leftPos + 90, topPos + 60, 8, 12, new TextComponent("<"), button -> {
            this.mineType = miner.getMineType();
            if (this.mineType != 0) {
                this.mineType--;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMineType(this.mineType, miner.getUUID()));
            }
        }));

        addRenderableWidget(new Button(leftPos + 90 + 70, topPos + 60, 8, 12, new TextComponent(">"), button -> {
            this.mineType = miner.getMineType();
            if (this.mineType != 4) {
                this.mineType++;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMineType(this.mineType, miner.getUUID()));
            }
        }));

        //MINEDEPTH
        addRenderableWidget(new Button(leftPos + 10, topPos + 60, 8, 12, new TextComponent("<"), button -> {
            if (miner.getMineType() != 3){
                this.mineDepth = miner.getMineDepth();
                if (this.mineDepth != 0) {
                    this.mineDepth--;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMineDepth(this.mineDepth, miner.getUUID()));
                }
            }
        }));

        addRenderableWidget(new Button(leftPos + 10 + 30, topPos + 60, 8, 12, new TextComponent(">"), button -> {
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
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        int k = 79;//right left
        int l = 19;//hight

        String depth;

        if (miner.getMineType() == 3 || miner.getMineType() == 4){
            depth = "-";
        }
        else depth = String.valueOf(miner.getMineDepth());
        font.draw(matrixStack, DEPTH +":", k - 70, l + 35, fontColor);
        font.draw(matrixStack, depth, k - 55, l + 45, fontColor);

        String type;
        switch (miner.getMineType()){
            default:
            case 0:
                if (miner.getFollow())
                    type = FOLLOWING.getString();
                else type = WANDERING.getString();
                break;
            case 1:
                type = TUNNEL.getString();
                break;
            case 2:
                type = TUNNEL3x3.getString();
                break;
            case 3:
                type = PIT8x8x8.getString();
                break;
            case 4:
                type = FLAT8x8x1.getString();
                break;
        }
        font.draw(matrixStack, MINEMODE.getString() + ":", k + 25, l + 35, fontColor);
        font.draw(matrixStack, type, k + 25, l + 45, fontColor);
    }

    private final TranslatableComponent MINEMODE = new TranslatableComponent("gui.workers.miner.mine_mode");
    private final TranslatableComponent TUNNEL = new TranslatableComponent("gui.workers.miner.tunnel");
    private final TranslatableComponent TUNNEL3x3 = new TranslatableComponent("gui.workers.miner.tunnel3x3");
    private final TranslatableComponent PIT8x8x8 = new TranslatableComponent("gui.workers.miner.pit8x8x8");
    private final TranslatableComponent FLAT8x8x1 = new TranslatableComponent("gui.workers.miner.flat8x8x1");
    private final TranslatableComponent DEPTH = new TranslatableComponent("gui.workers.miner.depth");



    private final TranslatableComponent WANDERING = new TranslatableComponent("gui.workers.following");
    private final TranslatableComponent FOLLOWING = new TranslatableComponent("gui.workers.wandering");



}
