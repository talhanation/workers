package com.talhanation.workers.entities.workarea;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Stack;

public class BuildArea extends AbstractWorkAreaEntity {

    public Stack<BlockPos> stackToBreak = new Stack<>();
    
    public BuildArea(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if(isDone()) this.remove(RemovalReason.DISCARDED);
    }

    @Override
    public Item getRenderItem() {
        return Items.IRON_SHOVEL;
    }

    @Override
    public Screen getScreen(Player player) {
        return null;
    }

    public void scanBreakArea(){
        stackToBreak.clear();
        Level level = this.getCommandSenderWorld();

        Fluid centerPosFluid = level.getFluidState(this.getOnPos()).getType();
        if(!(centerPosFluid == Fluids.WATER)|| (centerPosFluid == Fluids.FLOWING_WATER)) {
            this.stackToBreak.push(this.getOnPos());
        }

        for (int i = -getSize(); i <= getSize(); i++) {
            for (int k = -getHeight(); k <= getHeight(); k++) {
                for (int j = -getSize(); j <= getSize(); j++) {
                    BlockPos pos = getOnPos().offset(i, k, j);
                    BlockState state = level.getBlockState(pos);

                    if(!state.isAir()){
                        this.stackToBreak.push(pos);
                    }
                }
            }
        }
    }
}
