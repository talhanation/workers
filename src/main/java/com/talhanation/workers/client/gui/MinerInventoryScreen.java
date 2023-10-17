package com.talhanation.workers.client.gui;

import com.talhanation.workers.Main;
import com.talhanation.workers.inventory.WorkerInventoryContainer;
import com.talhanation.workers.entities.MinerEntity;
import com.talhanation.workers.network.MessageMineDepth;
import com.talhanation.workers.network.MessageMineType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.client.gui.widget.ExtendedButton;

public class MinerInventoryScreen extends WorkerInventoryScreen {

    private final MinerEntity miner;
    private int mineType;
    private int mineDepth;

    private static final int fontColor = 4210752;

    public MinerInventoryScreen(WorkerInventoryContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, Component.literal(""));
        this.miner = (MinerEntity) container.getWorker();
    }

    @Override
    protected void init() {
        super.init();
        // MINETYPE
        addRenderableWidget(new ExtendedButton(leftPos + 90, topPos + 60, 12, 12, Component.literal("<"), button -> {
            this.mineType = miner.getMineType();
            if (this.mineType != 0) {
                this.mineType--;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMineType(this.mineType, miner.getUUID()));
            }
        }));

        addRenderableWidget(new ExtendedButton(leftPos + 90 + 70, topPos + 60, 12, 12, Component.literal(">"), button -> {
            this.mineType = miner.getMineType();
            if (this.mineType != 8) {
                this.mineType++;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMineType(this.mineType, miner.getUUID()));
            }
        }));

        // MINEDEPTH
        Button button1 = addRenderableWidget(new Button(leftPos + 10, topPos + 60, 8, 12, Component.literal("<"), button -> {
            if (miner.getMineType() < 3){
                this.mineDepth = miner.getMineDepth();
                if (this.mineDepth != 0) {
                    this.mineDepth--;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMineDepth(this.mineDepth, miner.getUUID()));
                }
            }
        }));
        //button1.isActive = miner.getMineType() < 3;

        Button button2 = addRenderableWidget(new Button(leftPos + 10 + 30, topPos + 60, 8, 12, Component.literal(">"), button -> {
            if (miner.getMineType() < 3){
                this.mineDepth = miner.getMineDepth();
                if (this.mineDepth != miner.getMaxMineDepth()) {
                    this.mineDepth++;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMineDepth(this.mineDepth, miner.getUUID()));
                }
            }
        }));

        //button2.isActive = miner.getMineType() < 3;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        int k = 79;// right left
        int l = 19;// hight

        String depth;

        if (miner.getMineType() == 3 || miner.getMineType() == 4 || miner.getMineType() == 5) {
            depth = "-";
        } else
            depth = String.valueOf(miner.getMineDepth());
        guiGraphics.drawString(font, DEPTH.getString() + ":", k - 70, l + 35, fontColor, false);
        guiGraphics.drawString(font, depth, k - 55, l + 45, fontColor, false);

        String type;
        switch (miner.getMineType()) {
            default:
            case 0:
                if (miner.getFollow())
                    type = FOLLOWING.getString();
                else
                    type = WANDERING.getString();
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
            case 5:
                type = ROOM8x8x3.getString();
                break;
        }
        guiGraphics.drawString(font, MINEMODE.getString() + ":", k + 25, l + 35, fontColor, false);
        guiGraphics.drawString(font, type, k + 25, l + 45, fontColor, false);
    }

    private final MutableComponent MINEMODE = Component.translatable("gui.workers.miner.mine_mode");
    private final MutableComponent TUNNEL = Component.translatable("gui.workers.miner.tunnel");
    private final MutableComponent TUNNEL3x3 = Component.translatable("gui.workers.miner.tunnel3x3");
    private final MutableComponent PIT8x8x8 = Component.translatable("gui.workers.miner.pit8x8x8");
    private final MutableComponent FLAT8x8x1 = Component.translatable("gui.workers.miner.flat8x8x1");
    private final MutableComponent ROOM8x8x3 = Component.translatable("gui.workers.miner.room8x8x3");
    private final MutableComponent DEPTH = Component.translatable("gui.workers.miner.depth");
    private final MutableComponent WANDERING = Component.translatable("gui.workers.following");
    private final MutableComponent FOLLOWING = Component.translatable("gui.workers.wandering");

}
