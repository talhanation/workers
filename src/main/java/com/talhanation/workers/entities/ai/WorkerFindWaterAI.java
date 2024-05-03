package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.Random;

public class WorkerFindWaterAI extends Goal {
    private final AbstractWorkerEntity worker;
    private BlockPos targetPos;

    public WorkerFindWaterAI(AbstractWorkerEntity worker) {
        this.worker = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public boolean canUse() {
        if (this.worker.isOnGround() && worker.getStatus() == AbstractWorkerEntity.Status.WORK){

            targetPos = findBlockWater();

            return targetPos != null;
        }
        return false;
    }


    public void tick() {
        if(targetPos != null){
            this.worker.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1D);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !targetPos.closerThan(worker.getOnPos(), 10);
    }

    public BlockPos findBlockWater() {
        BlockPos blockpos = null;
        Random random = new Random();
        int range = 14;
        for(int i = 0; i < 15; i++){
            BlockPos blockpos1 = this.worker.getWorkerOnPos().offset(random.nextInt(range) - range/2, 3, random.nextInt(range) - range/2);
            while(this.worker.level.isEmptyBlock(blockpos1) && blockpos1.getY() > 1){
                blockpos1 = blockpos1.below();
            }
            if(this.worker.level.getFluidState(blockpos1).is(FluidTags.WATER)){
                blockpos = blockpos1;
            }
        }
        return blockpos;
    }
}

