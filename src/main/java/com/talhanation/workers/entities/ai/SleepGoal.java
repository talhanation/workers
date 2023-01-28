package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public class SleepGoal extends Goal {
    private final AbstractWorkerEntity worker;
    private BlockPos sleepPos;

    private final MutableComponent NEED_HOME = Component.translatable("chat.workers.needHome");
    private final MutableComponent NEED_BED = Component.translatable("chat.workers.needBed");

    public SleepGoal(AbstractWorkerEntity worker) {
        this.worker = worker;
    }

    @Override
    public boolean canUse() {
        if (
            this.worker.getOwner() == null ||
            this.worker.needsHome() ||
            this.worker.needsBed()
        ) {
            return false;
        }
        
        return worker.needsToSleep();
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

            if (!worker.level.isDay()) {
                this.worker.heal(0.025F);
            }

            if (worker.level.isDay()) {
                this.stop();
            }
        }

        LivingEntity owner = worker.getOwner();
        if (owner != null) {

            if (worker.getHomePos() == null) {
                worker.setNeedsHome(true);
                worker.tellPlayer(owner, NEED_HOME);
                return;
            }

            if (worker.needsBed()) {
                worker.tellPlayer(owner, NEED_BED);
                return;
            }
        }

        this.sleepPos = worker.getBedPos();
        // If the worker doesn't have an owner, grab a random bed.
        if (this.sleepPos == null) {
            this.sleepPos = this.findSleepPos();
        }

        if (this.sleepPos != null) {
            // Move to the bed and stay there.
            this.worker.getNavigation().moveTo(sleepPos.getX(), sleepPos.getY(), sleepPos.getZ(), 1.1D);
            this.worker.getLookControl().setLookAt(
                sleepPos.getX(), 
                sleepPos.getY() + 1, 
                sleepPos.getZ(), 
                10.0F,
                (float) this.worker.getMaxHeadXRot()
            );

            if (sleepPos.closerThan(worker.getOnPos(), 4)) {
                this.worker.startSleeping(sleepPos);
                this.worker.setSleepingPos(sleepPos);
            }
        } else {
            // If no beds nearby, do another goal, like wander or return to village.
            this.stop();
        }
    }

    @Nullable
    private BlockPos findSleepPos() {
        BlockPos homePos = this.worker.getHomePos();
        if (homePos != null) {
            BlockPos bedPos;
            int range = 16;

            for (int x = -range; x < range; x++) {
                for (int y = -range; y < range; y++) {
                    for (int z = -range; z < range; z++) {
                        bedPos = homePos.offset(x, y, z);
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
        }
        return null;
    }
}
