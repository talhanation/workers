package com.talhanation.workers.entities.ai.navigation.door;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import java.util.ArrayList;
import java.util.List;


public abstract class WorkersDoorInteractGoal extends Goal {
    protected Mob mob;
    protected BlockPos doorPos = BlockPos.ZERO;
    protected boolean hasDoor;
    private boolean passed;
    private float doorOpenDirX;
    private float doorOpenDirZ;

    public WorkersDoorInteractGoal(Mob p_25193_) {
        this.mob = p_25193_;
        if (!GoalUtils.hasGroundPathNavigation(p_25193_)) {
            throw new IllegalArgumentException("Unsupported mob type for DoorInteractGoal");
        }
    }

    protected boolean isOpen() {
        if (!this.hasDoor) {
            return false;
        } else {
            BlockState blockstate = this.mob.level.getBlockState(this.doorPos);
            if (!(blockstate.getBlock() instanceof DoorBlock)) {
                this.hasDoor = false;
                return false;
            } else {
                return blockstate.getValue(DoorBlock.OPEN);
            }
        }
    }

    protected void setOpen(boolean open) {
        if (this.hasDoor) {
            BlockState blockstate = this.mob.level.getBlockState(this.doorPos);

            if (blockstate.getBlock() instanceof DoorBlock) {
                ((DoorBlock) blockstate.getBlock()).setOpen(this.mob, this.mob.level, blockstate, this.doorPos, open);
            } else if (blockstate.getBlock() instanceof FenceGateBlock) {
                useGate(blockstate, this.mob.level, doorPos, this.mob);
            }

            for(Direction direction: Direction.values()){
                if(direction.equals(Direction.DOWN)) continue;

                BlockPos blockPos = this.doorPos.relative(direction);
                BlockState state = this.mob.level.getBlockState(blockPos);

                if (state.getBlock() instanceof DoorBlock) {
                    ((DoorBlock) blockstate.getBlock()).setOpen(this.mob, this.mob.level, blockstate, this.doorPos, open);
                } else if (state.getBlock() instanceof FenceGateBlock) {
                    useGate(blockstate, this.mob.level, blockPos, this.mob);
                }
            }
        }
    }

    public boolean canUse() {
        if (!GoalUtils.hasGroundPathNavigation(this.mob)) {
            return false;
        } else if (!this.mob.horizontalCollision) {
            return false;
        } else {
            GroundPathNavigation groundpathnavigation = (GroundPathNavigation)this.mob.getNavigation();
            Path path = groundpathnavigation.getPath();
            if (path != null && !path.isDone() && groundpathnavigation.canOpenDoors()) {
                for(int i = 0; i < Math.min(path.getNextNodeIndex() + 2, path.getNodeCount()); ++i) {
                    Node node = path.getNode(i);
                    this.doorPos = new BlockPos(node.x, node.y, node.z);
                    if (!(this.mob.distanceToSqr((double)this.doorPos.getX(), this.mob.getY(), (double)this.doorPos.getZ()) > 2.25D)) {
                        this.hasDoor = DoorBlock.isWoodenDoor(this.mob.level, this.doorPos) || (this.mob.getCommandSenderWorld().getBlockState(this.doorPos).getBlock() instanceof FenceGateBlock);
                        if (this.hasDoor) {
                            return true;
                        }
                    }
                }

                this.doorPos = this.mob.blockPosition().above();
                this.hasDoor = DoorBlock.isWoodenDoor(this.mob.level, this.doorPos) || (this.mob.getCommandSenderWorld().getBlockState(this.doorPos).getBlock() instanceof FenceGateBlock);
                return this.hasDoor;
            } else {
                return false;
            }
        }
    }

    public boolean canContinueToUse() {
        return !this.passed;
    }

    public void start() {
        this.passed = false;
        this.doorOpenDirX = (float)((double)this.doorPos.getX() + 0.5D - this.mob.getX());
        this.doorOpenDirZ = (float)((double)this.doorPos.getZ() + 0.5D - this.mob.getZ());
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        float f = (float)((double)this.doorPos.getX() + 0.5D - this.mob.getX());
        float f1 = (float)((double)this.doorPos.getZ() + 0.5D - this.mob.getZ());
        float f2 = this.doorOpenDirX * f + this.doorOpenDirZ * f1;
        if (f2 < 0.0F) {
            this.passed = true;
        }

    }

    public void useGate(BlockState blockState, Level level, BlockPos blockPos, Entity entity) {
        if (blockState.getValue(FenceGateBlock.OPEN)) {
            blockState = blockState.setValue(FenceGateBlock.OPEN, Boolean.FALSE);
            level.setBlock(blockPos, blockState, 10);
        } else {
            Direction direction = entity.getDirection();
            if (blockState.getValue(FenceGateBlock.FACING) == direction.getOpposite()) {
                blockState = blockState.setValue(FenceGateBlock.FACING, direction);
            }

            blockState = blockState.setValue(FenceGateBlock.OPEN, Boolean.TRUE);
            level.setBlock(blockPos, blockState, 10);
        }

        boolean flag = blockState.getValue(FenceGateBlock.OPEN);
        //level.levelEvent(entity, flag ? 1008 : 1014, blockPos, 0);
        level.gameEvent(entity, flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, blockPos);
    }

}
