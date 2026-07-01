package com.talhanation.workers.entities.ai.navigation;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

public class WorkerOpenDoorGoal extends Goal {
    protected final AbstractWorkerEntity worker;
    protected BlockPos doorPos = BlockPos.ZERO;
    protected boolean hasDoor;
    private boolean passed;
    private float doorOpenDirX;
    private float doorOpenDirZ;

    private final boolean closeDoor;
    private int forgetTime;

    public WorkerOpenDoorGoal(AbstractWorkerEntity worker, boolean closeDoor) {
        this.worker = worker;
        this.closeDoor = closeDoor;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    protected boolean isOpen() {
        if (!this.hasDoor) {
            return false;
        } else {
            BlockState blockstate = this.worker.getCommandSenderWorld().getBlockState(this.doorPos);
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
            BlockState blockstate = this.worker.getCommandSenderWorld().getBlockState(this.doorPos);
            if (blockstate.getBlock() instanceof FenceGateBlock) {
                useGate(blockstate, this.worker.getCommandSenderWorld(), doorPos, this.worker);
            }
            else if (blockstate.getBlock() instanceof DoorBlock doorBlock) {
                doorBlock.setOpen(this.worker, this.worker.getCommandSenderWorld(), blockstate, this.doorPos, open);
            }

            for(Direction direction: Direction.values()){
                if(direction.equals(Direction.DOWN)) continue;

                BlockPos blockPos = this.doorPos.relative(direction);
                BlockState state = this.worker.getCommandSenderWorld().getBlockState(blockPos);
                if (state.getBlock() instanceof FenceGateBlock) {
                    useGate(blockstate, this.worker.getCommandSenderWorld(), blockPos, this.worker);
                }
                else if (state.getBlock() instanceof DoorBlock doorBlock) {
                    doorBlock.setOpen(this.worker, this.worker.getCommandSenderWorld(), blockstate, this.doorPos, open);
                }
            }
        }
    }

    @Override
    public boolean canUse() {
        if (!GoalUtils.hasGroundPathNavigation(this.worker)) {
            return false;
        } else if (!this.worker.horizontalCollision) {
            return false;
        } else {
            WorkersGroundPathNavigation groundpathnavigation = this.worker.getNavigation() instanceof WorkersGroundPathNavigation wNav ? wNav : null;
            if (groundpathnavigation == null) return false;
            Path path = groundpathnavigation.getPath();
            if (path != null && !path.isDone() && groundpathnavigation.canOpenDoors()) {
                for(int i = 0; i < Math.min(path.getNextNodeIndex() + 2, path.getNodeCount()); ++i) {
                    Node node = path.getNode(i);
                    this.doorPos = new BlockPos(node.x, node.y, node.z);
                    if (!(this.worker.distanceToSqr((double)this.doorPos.getX(), this.worker.getY(), (double)this.doorPos.getZ()) > 5D)) {
                        this.hasDoor = DoorBlock.isWoodenDoor(this.worker.getCommandSenderWorld(), this.doorPos) || (this.worker.getCommandSenderWorld().getBlockState(this.doorPos).getBlock() instanceof FenceGateBlock);
                        if (this.hasDoor) {
                            return true;
                        }
                    }
                }

                this.doorPos = this.worker.blockPosition().above();
                this.hasDoor = DoorBlock.isWoodenDoor(this.worker.getCommandSenderWorld(), this.doorPos) || (this.worker.getCommandSenderWorld().getBlockState(this.doorPos).getBlock() instanceof FenceGateBlock);
                return this.hasDoor;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.closeDoor && this.forgetTime > 0 && !this.passed;
    }

    @Override
    public void start() {
        this.forgetTime = 25;
        this.passed = false;
        this.doorOpenDirX = (float)((double)this.doorPos.getX() + 0.5D - this.worker.getX());
        this.doorOpenDirZ = (float)((double)this.doorPos.getZ() + 0.5D - this.worker.getZ());
        this.setOpen(true);
    }

    @Override
    public void stop() {
        this.setOpen(false);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        --this.forgetTime;
        float f = (float)((double)this.doorPos.getX() + 0.5D - this.worker.getX());
        float f1 = (float)((double)this.doorPos.getZ() + 0.5D - this.worker.getZ());
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
        level.gameEvent(entity, flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, blockPos);
    }
}
