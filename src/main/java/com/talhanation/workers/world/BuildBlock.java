package com.talhanation.workers.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BuildBlock {
    BlockState state;
    BlockPos pos;

    public BuildBlock(BlockPos pos, BlockState state) {
        this.pos = pos;
        this.state = state;
    }

    public void setState(BlockState state) {
        this.state = state;
    }

    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getState() {
        return state;
    }
}
