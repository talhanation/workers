package com.talhanation.workers.entities.ai.animals;

import com.talhanation.workers.entities.AnimalFarmerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class WorkerTemptGoal extends Goal {
    private static final TargetingConditions TEMP_TARGETING = TargetingConditions.forNonCombat().range(10.0D).ignoreLineOfSight();
    private final TargetingConditions targetingConditions;
    protected final PathfinderMob mob;
    private final double speedModifier;
    @Nullable
    protected AnimalFarmerEntity animalFarmerEntity;
    private int calmDown;
    private final Ingredient items;


    public WorkerTemptGoal(PathfinderMob p_25939_, double p_25940_, Ingredient p_25941_) {
        this.mob = p_25939_;
        this.speedModifier = p_25940_;
        this.items = p_25941_;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.targetingConditions = TEMP_TARGETING.copy().selector(this::shouldFollow);
    }

    public boolean canUse() {
        if (this.calmDown > 0) {
            --this.calmDown;
            return false;
        } else {
            this.animalFarmerEntity = this.mob.level().getNearestEntity(AnimalFarmerEntity.class, this.targetingConditions, this.mob, this.mob.getX(), this.mob.getY(), this.mob.getZ(), this.mob.getBoundingBox().inflate(16.0D));
            return this.animalFarmerEntity != null;
        }
    }

    private boolean shouldFollow(LivingEntity p_148139_) {
        return this.items.test(p_148139_.getMainHandItem());
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }
    public void start() {

    }

    public void stop() {
        this.animalFarmerEntity = null;
        this.mob.getNavigation().stop();
        this.calmDown = reducedTickDelay(100);
    }

    public void tick() {
        this.mob.getLookControl().setLookAt(this.animalFarmerEntity, (float)(this.mob.getMaxHeadYRot() + 20), (float)this.mob.getMaxHeadXRot());
        if (this.mob.distanceToSqr(this.animalFarmerEntity) < 15.0D){
            this.mob.getNavigation().stop();
        } else {
            this.mob.getNavigation().moveTo(this.animalFarmerEntity, this.speedModifier);
        }

    }
}
