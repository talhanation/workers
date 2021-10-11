package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;
import java.util.function.Predicate;
import net.minecraft.entity.ai.goal.TargetGoal;
import net.minecraft.util.math.AxisAlignedBB;
import javax.annotation.Nullable;


public class MoveToEntityGoal<T extends LivingEntity> extends TargetGoal {
    protected final Class<T> targetType;
    public LivingEntity target;
    public AbstractWorkerEntity worker;
    public EntityPredicate targetConditions;

    public MoveToEntityGoal(AbstractWorkerEntity worker, Class<T> target, boolean p_i50313_3_) {
        this(worker, target, p_i50313_3_, false);
        this.worker = worker;
    }

    public MoveToEntityGoal(AbstractWorkerEntity worker, Class<T> target, boolean p_i50314_3_, boolean p_i50314_4_) {
        this(worker, target, p_i50314_3_, p_i50314_4_, null);
    }

    public MoveToEntityGoal(AbstractWorkerEntity worker, Class<T> target, boolean p_i50315_4_, boolean p_i50315_5_, @Nullable Predicate<LivingEntity> predicate) {
        super(worker, p_i50315_4_, p_i50315_5_);
        this.targetType = target;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        this.targetConditions = (new EntityPredicate()).range(this.getFollowDistance()).selector(predicate);
    }

    public boolean canUse() {
        this.findTarget();
        return true;
    }

    protected AxisAlignedBB getTargetSearchArea(double area) {
        return this.mob.getBoundingBox().inflate(area, 8.0D, area);
    }

    protected void findTarget() {
        this.target = this.mob.level.getNearestLoadedEntity(this.targetType, this.targetConditions, this.mob, this.mob.getX(), this.mob.getY(), this.mob.getZ(), this.getTargetSearchArea(this.getFollowDistance()));

    }

    public void start() {
        if (this.target != null)
        this.mob.getNavigation().moveTo(this.target, 1);
        super.start();
    }

}