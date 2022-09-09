package com.talhanation.workers.entities.ai;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public class SleepGoal extends Goal {

    private final AbstractWorkerEntity worker;
    private BlockPos sleepPos;
    private BlockPos homePos;

    private final TranslatableComponent NEED_HOME = new TranslatableComponent("chat.workers.needHome");
    private final TranslatableComponent NEED_BED = new TranslatableComponent("chat.workers.needBed");

    public SleepGoal(AbstractWorkerEntity worker){
        this.worker = worker;
    }

    @Override
    public boolean canUse() {
        return worker.needsToSleep() || (worker.isSleeping() && worker.level.isDay());
    }

    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        if(worker.getHomePos() != null) this.homePos = worker.getHomePos();

        if (homePos == null && worker.getOwner() != null){
            worker.getOwner().sendMessage(new TextComponent(worker.getName().getString() + ": " + NEED_HOME.getString()),worker.getOwner().getUUID());
        }
        else if (homePos != null){
            this.sleepPos = this.findSleepPos();

            if (sleepPos == null && worker.getOwner() != null){
                worker.getOwner().sendMessage(new TextComponent(worker.getName().getString() + ": " + NEED_BED.getString()), worker.getOwner().getUUID());
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        if(worker.getStartPos() != null )this.worker.setIsWorking(true);
        this.worker.stopSleeping();
        this.worker.clearSleepingPos();
    }

    @Override
    public void tick() {
        if(!worker.level.isDay() && !worker.isSleeping()){
            if (homePos != null) sleepPos = this.findSleepPos();

        }

        if(!worker.isSleeping() && sleepPos != null){
            this.worker.getNavigation().moveTo(sleepPos.getX(), sleepPos.getY(), sleepPos.getZ(), 1.1D);
            this.worker.getLookControl().setLookAt(sleepPos.getX(), sleepPos.getY() + 1, sleepPos.getZ(), 10.0F, (float) this.worker.getMaxHeadXRot());

            if (sleepPos.closerThan(worker.getOnPos(), 4)) {
                this.worker.startSleeping(sleepPos);
                this.worker.setSleepingPos(sleepPos);
            }
        }

        if(worker.isSleeping()) {
            this.worker.getNavigation().stop();

            if (!worker.level.isDay()) {
                this.worker.heal(0.025F);
            }

            if (worker.level.isDay()) {
                this.stop();
            }
        }
    }

    @Nullable
    private BlockPos findSleepPos() {
        if(this.worker.getHomePos() != null) {
            BlockPos bedPos;
            int range = 8;

            for (int x = -range; x < range; x++) {
                for (int y = -range; y < range; y++) {
                    for (int z = -range; z < range; z++) {
                        bedPos = homePos.offset(x, y, z);
                        BlockState state = worker.level.getBlockState(bedPos);

                        if (state.isBed(worker.level, bedPos, this.worker) && !state.getValue(BlockStateProperties.OCCUPIED)) {
                            return bedPos;
                        }
                    }
                }
            }
        }
        return null;
    }
}
