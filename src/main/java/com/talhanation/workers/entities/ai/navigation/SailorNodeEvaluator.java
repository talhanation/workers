package com.talhanation.workers.entities.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.SwimNodeEvaluator;

import javax.annotation.Nullable;

public class SailorNodeEvaluator extends SwimNodeEvaluator {

    public SailorNodeEvaluator() {
        super(false);
    }

    @Nullable
    @Override
    protected Node getNode(int x, int y, int z) {
        Node node = null;
        BlockPathTypes blockpathtypes = this.getCachedBlockType(x, y, z);
        if (blockpathtypes == BlockPathTypes.WATER) {
            float f = this.mob.getPathfindingMalus(blockpathtypes);
            if (f >= 0.0F) {
                node = this.getSailorNode(x, y, z);
                if (node != null) {
                    node.type = blockpathtypes;
                    node.costMalus = Math.max(node.costMalus, f);
                    if (this.level.getFluidState(new BlockPos(x, y, z)).isEmpty()) {
                        node.costMalus += 8.0F;
                    }
                }
            }
        }

        return node;
    }

    @Nullable
    protected Node getSailorNode(int x, int y, int z) {

        if(isValidWaterPos(x,y,z)){
            return this.nodes.computeIfAbsent(Node.createHash(x, y, z), (node) -> {
                return new Node(x, y, z);
            });
        }
        return null;
    }


    public boolean isValidWaterPos(int x, int y, int z){
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = this.mob.getLevel().getBlockState(pos);

        if(state.is(Blocks.WATER)){
            BlockState stateNorth = this.mob.getLevel().getBlockState(pos.north());
            BlockState stateEast = this.mob.getLevel().getBlockState(pos.east());
            BlockState stateSouth = this.mob.getLevel().getBlockState(pos.south());
            BlockState stateWest = this.mob.getLevel().getBlockState(pos.west());

            /*
            BlockState stateNorthEast = this.mob.getLevel().getBlockState(pos.north().east());
            BlockState stateNorthWest = this.mob.getLevel().getBlockState(pos.north().west());
            BlockState stateSouthEast = this.mob.getLevel().getBlockState(pos.south().east());
            BlockState stateSouthWest = this.mob.getLevel().getBlockState(pos.south().west());

             */

            return stateNorth.is(Blocks.WATER) && stateEast.is(Blocks.WATER) && stateSouth.is(Blocks.WATER) && stateWest.is(Blocks.WATER);

            //stateNorthEast.is(Blocks.WATER) && stateNorthWest.is(Blocks.WATER) && stateSouthEast.is(Blocks.WATER) && stateSouthWest.is(Blocks.WATER);
        }
        return false;
    }
}
