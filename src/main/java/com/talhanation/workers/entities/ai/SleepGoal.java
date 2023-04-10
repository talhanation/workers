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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public class SleepGoal extends Goal {
    private final AbstractWorkerEntity worker;
    private final MutableComponent NEED_BED = Component.translatable("chat.workers.needBed");
    private final MutableComponent CANT_FIND_BED = Component.translatable("chat.workers.cantFindBed");

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
        if (owner == null) {
            this.goToBed(this.grabRandomBed());
            return;
        }

        if (owner != null && worker.needsBed()) {
            worker.tellPlayer(owner, NEED_BED);
            return;
        }

        BlockPos sleepPos = worker.getBedPos();
        if (sleepPos == null) {
            worker.tellPlayer(owner, NEED_BED);
            worker.setNeedsBed(true);
            return;
        }

        BlockEntity bedEntity = worker.level.getBlockEntity(sleepPos);
        if (bedEntity == null || !bedEntity.getBlockState().isBed(worker.level, sleepPos, worker)) {
            worker.tellPlayer(owner, CANT_FIND_BED);
            worker.setNeedsBed(true);
            return;
        }
        
        if (bedEntity.getBlockState().getValue(BlockStateProperties.OCCUPIED)) {
            this.goToBed(this.grabRandomBed());
        } else {
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
    @Nullable
    private BlockPos grabRandomBed() {
        BlockPos workerPos = this.worker.getOnPos();
        if (workerPos == null) return null;
        BlockPos bedPos;
        int range = 16;

        for (int x = -range; x < range; x++) {
            for (int y = -range; y < range; y++) {
                for (int z = -range; z < range; z++) {
                    bedPos = workerPos.offset(x, y, z);
                    BlockState state = worker.level.getBlockState(bedPos);

                    if (
                        state.isBed(worker.level, bedPos, this.worker) && 
                        state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD &&
                        !state.getValue(BlockStateProperties.OCCUPIED)
                    ) {
                        return bedPos;
                    }
                }
            }
        }
        return null;
    }
}
