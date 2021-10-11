package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.TargetGoal;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.function.Predicate;

public class SheerSheepGoal<T extends SheepEntity> extends TargetGoal {
    protected final Class<T> targetType;
    public SheepEntity target;
    public AbstractWorkerEntity worker;
    public EntityPredicate targetConditions;

    public SheerSheepGoal(AbstractWorkerEntity worker, Class<T> target, boolean p_i50313_3_) {
        this(worker, target, p_i50313_3_, false);
        this.worker = worker;
    }

    public SheerSheepGoal(AbstractWorkerEntity worker, Class<T> target, boolean p_i50314_3_, boolean p_i50314_4_) {
        this(worker, target, p_i50314_3_, p_i50314_4_, null);
    }

    public SheerSheepGoal(AbstractWorkerEntity worker, Class<T> target, boolean p_i50315_4_, boolean p_i50315_5_, @Nullable Predicate<LivingEntity> predicate) {
        super(worker, p_i50315_4_, p_i50315_5_);
        this.targetType = target;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        this.targetConditions = (new EntityPredicate()).range(this.getFollowDistance()).selector(predicate);
    }

    public boolean canUse() {
        return true;
    }

    protected AxisAlignedBB getTargetSearchArea(double area) {
        return this.mob.getBoundingBox().inflate(area, 8.0D, area);
    }

    protected void findSheep() {
        SheepEntity sheepTarget = this.mob.level.getNearestLoadedEntity(this.targetType, this.targetConditions, this.mob, this.mob.getX(), this.mob.getY(), this.mob.getZ(), this.getTargetSearchArea(this.getFollowDistance()));
        if (sheepTarget != null){
            if (sheepTarget.readyForShearing())
                this.target = sheepTarget;
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.findSheep();

        if (!this.target.readyForShearing()){

        }
        if (this.target != null){
            this.worker.getNavigation().moveTo(this.target, 0.85);

            if (target.closerThan(this.worker, 3)){
                sheerSheep(this.target);
                this.worker.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ(), 10.0F, (float) this.worker.getMaxHeadXRot());

               if (!this.worker.swinging) {
                    this.worker.swing(this.worker.getUsedItemHand());
                }

            }
        }
    }

    public void sheerSheep(SheepEntity sheep) {
        sheep.shear(SoundCategory.PLAYERS);
    }

}