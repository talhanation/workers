package com.talhanation.workers.entities.ai.navigation;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;


public class WorkerOpenDoorGoal extends Goal {
    protected final AbstractWorkerEntity worker;
    protected BlockPos doorPos = BlockPos.ZERO;
    protected boolean hasDoor;
    private boolean passed;
    private boolean wasInside;
    private boolean cleared;
    private float doorOpenDirX;
    private float doorOpenDirZ;
    private final List<BlockPos> openedDoors = new ArrayList<>();

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
            if (!(blockstate.getBlock() instanceof DoorBlock) && !(blockstate.getBlock() instanceof FenceGateBlock)) {
                this.hasDoor = false;
                return false;
            } else {
                return blockstate.getValue(DoorBlock.OPEN);
            }
        }
    }

    // Only handle closed doors/gates. An open gate (left open by a player or another worker)
    // must not be touched, otherwise opening/closing would toggle it and leave it open.
    private static boolean isClosedDoor(Level level, BlockPos pos){
        BlockState state = level.getBlockState(pos);
        if(state.getBlock() instanceof FenceGateBlock) return !state.getValue(FenceGateBlock.OPEN);
        if(DoorBlock.isWoodenDoor(level, pos)) return !state.getValue(DoorBlock.OPEN);
        return false;
    }

    protected void setOpen(boolean open) {
        Level level = this.worker.getCommandSenderWorld();
        if(open){
            if(!this.hasDoor) return;
            this.openedDoors.clear();

            BlockState mainState = level.getBlockState(this.doorPos);
            this.openDoor(level, this.doorPos, mainState);

            //double doors and double gates
            for(Direction direction : Direction.Plane.HORIZONTAL){
                BlockPos blockPos = this.doorPos.relative(direction);
                BlockState state = level.getBlockState(blockPos);
                if(state.getBlock().getClass() != mainState.getBlock().getClass()) continue;
                if(!isClosedDoor(level, blockPos)) continue;

                this.openDoor(level, blockPos, state);
            }
        }
        else{
            for(BlockPos pos : this.openedDoors){
                this.closeDoor(level, pos);
            }
            this.openedDoors.clear();
        }
    }

    private void openDoor(Level level, BlockPos pos, BlockState state){
        if(state.getBlock() instanceof FenceGateBlock){
            if(state.getValue(FenceGateBlock.OPEN)) return;

            Direction direction = this.worker.getDirection();
            if(state.getValue(FenceGateBlock.FACING) == direction.getOpposite()){
                state = state.setValue(FenceGateBlock.FACING, direction);
            }
            level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, true), 10);
            level.playSound(null, pos, SoundEvents.FENCE_GATE_OPEN, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
            level.gameEvent(this.worker, GameEvent.BLOCK_OPEN, pos);
            this.openedDoors.add(pos);
        }
        else if(state.getBlock() instanceof DoorBlock doorBlock){
            if(state.getValue(DoorBlock.OPEN)) return;

            doorBlock.setOpen(this.worker, level, state, pos, true);
            this.openedDoors.add(pos);
        }
    }

    private void closeDoor(Level level, BlockPos pos){
        BlockState state = level.getBlockState(pos);
        if(state.getBlock() instanceof FenceGateBlock){
            if(!state.getValue(FenceGateBlock.OPEN)) return;

            level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, false), 10);
            level.playSound(null, pos, SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
            level.gameEvent(this.worker, GameEvent.BLOCK_CLOSE, pos);
        }
        else if(state.getBlock() instanceof DoorBlock doorBlock){
            doorBlock.setOpen(this.worker, level, state, pos, false);
        }
    }

    @Override
    public boolean canUse() {
        // NOTE: intentionally NOT using GoalUtils.hasGroundPathNavigation() here.
        // Worker/recruit navigations extend the async PathNavigation, not vanilla
        // GroundPathNavigation, so that helper returns false and would block the
        // goal entirely. We type-check WorkersGroundPathNavigation directly below.
        if (!this.worker.horizontalCollision) {
            return false;
        } else {
            WorkersGroundPathNavigation groundpathnavigation = this.worker.getNavigation() instanceof WorkersGroundPathNavigation wNav ? wNav : null;
            if (groundpathnavigation == null) return false;
            Path path = groundpathnavigation.getPath();
            if (path != null && !path.isDone() && groundpathnavigation.canOpenDoors()) {
                Level level = this.worker.getCommandSenderWorld();
                for(int i = 0; i < Math.min(path.getNextNodeIndex() + 2, path.getNodeCount()); ++i) {
                    Node node = path.getNode(i);
                    this.doorPos = new BlockPos(node.x, node.y, node.z);
                    if (!(this.worker.distanceToSqr((double)this.doorPos.getX(), this.worker.getY(), (double)this.doorPos.getZ()) > 5D)) {
                        this.hasDoor = isClosedDoor(level, this.doorPos);
                        if (this.hasDoor) {
                            return true;
                        }
                    }
                }

                this.doorPos = this.worker.blockPosition().above();
                this.hasDoor = isClosedDoor(level, this.doorPos);
                return this.hasDoor;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.closeDoor && this.forgetTime > 0 && !this.cleared;
    }

    @Override
    public void start() {
        this.forgetTime = 40;
        this.passed = false;
        this.wasInside = false;
        this.cleared = false;
        this.doorOpenDirX = (float)((double)this.doorPos.getX() + 0.5D - this.worker.getX());
        this.doorOpenDirZ = (float)((double)this.doorPos.getZ() + 0.5D - this.worker.getZ());
        this.setOpen(true);
    }

    @Override
    public void stop() {
        if(this.closeDoor) this.setOpen(false);
        else this.openedDoors.clear();
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

        // Close only after the worker fully left the door block, not when his center passed it.
        boolean inside = this.isWorkerInDoor();
        if(inside) this.wasInside = true;
        else if(this.passed || this.wasInside) this.cleared = true;
    }

    private boolean isWorkerInDoor(){
        AABB workerBox = this.worker.getBoundingBox().inflate(0.1D);
        for(BlockPos pos : this.openedDoors){
            if(workerBox.intersects(new AABB(pos))) return true;
        }
        return false;
    }
}
