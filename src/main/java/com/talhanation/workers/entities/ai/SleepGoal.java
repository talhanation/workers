package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class SleepGoal extends Goal {
    private final AbstractWorkerEntity worker;
    private final MutableComponent NEED_BED = Component.translatable("chat.workers.needBed");
    private final MutableComponent CANT_FIND_BED = Component.translatable("chat.workers.cantFindBed");
    private final MutableComponent BED_OCCUPIED = Component.translatable("chat.workers.bedOccupied");
    private boolean messageCantFindBed;
    private boolean messageBedOccupied;
    public SleepGoal(AbstractWorkerEntity worker) {
        this.worker = worker;
    }

    @Override
    public boolean canUse() {        
        return !this.worker.needsBed() && worker.needsToSleep() && !worker.getFollow();
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        super.start();
        this.messageCantFindBed = true;
        this.messageBedOccupied = true;
    }

    @Override
    public void stop() {
        super.stop();   
        this.worker.stopSleeping();
        this.worker.clearSleepingPos();

    }

    @Override
    public void tick() {
        if (worker.isSleeping()) {
            this.worker.getNavigation().stop();
            if (this.worker.needsToSleep()) this.worker.heal(0.025F);
            return;
        }

        LivingEntity owner = worker.getOwner();
        BlockPos sleepPos = worker.getBedPos();
        if (sleepPos == null) {
            if(owner != null) worker.tellPlayer(owner, NEED_BED);
            return;
        }

        BlockEntity bedEntity = worker.level.getBlockEntity(sleepPos);
        if (messageCantFindBed && bedEntity == null || !bedEntity.getBlockState().isBed(worker.level, sleepPos, worker)) {
            if(owner != null) worker.tellPlayer(owner, CANT_FIND_BED);
            messageCantFindBed = false;
            return;
        }
        
        if (bedEntity.getBlockState().getValue(BlockStateProperties.OCCUPIED)) {
            if(messageBedOccupied){
                if(owner != null) worker.tellPlayer(owner, BED_OCCUPIED);
                messageBedOccupied = false;
            }
        }
        else {
            this.goToBed(sleepPos);
        }
    }

    /**
     * Move to the bed.
     * @param bedPos The position of the bed.
     */
    private void goToBed(BlockPos bedPos) {
        if (bedPos == null) {
            return;
        }
        // Move to the bed and stay there.
        PathNavigation pathFinder = this.worker.getNavigation();
        pathFinder.moveTo(bedPos.getX(), bedPos.getY(), bedPos.getZ(), 1.1D);
        this.worker.getLookControl().setLookAt(
            bedPos.getX(), 
            bedPos.getY() + 1, 
            bedPos.getZ(), 
            10.0F,
            (float) this.worker.getMaxHeadXRot()
        );
    
        if (bedPos.distManhattan((Vec3i) worker.getWorkerOnPos()) <= 5) {
            this.worker.startSleeping(bedPos);
            this.worker.setSleepingPos(bedPos);
            pathFinder.stop();
        }
    }

    /**
     * Find a bed to sleep in.
     * @return The position of the bed.
     */

    /*
    @Nullable
    private BlockPos grabRandomBed() {
        BlockPos bedPos;
        int range = 16;

        for (int x = -range; x < range; x++) {
            for (int y = -range; y < range; y++) {
                for (int z = -range; z < range; z++) {
                    bedPos = worker.getOnPos().offset(x, y, z);
                    BlockState state = worker.level.getBlockState(bedPos);

                    if (state.isBed(worker.level, bedPos, this.worker) &&
                        state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD &&
                        !state.getValue(BlockStateProperties.OCCUPIED)) {
                        return bedPos;
                    }
                }
            }
        }
        return null;
    }

     */
}
