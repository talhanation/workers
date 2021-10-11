package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.math.AxisAlignedBB;


public class BreedAnimalGoal extends Goal {

    public AbstractWorkerEntity worker;
    public AnimalEntity animal;

    public BreedAnimalGoal(AbstractWorkerEntity worker, AnimalEntity animal) {
        this.worker = worker;
        this.animal = animal;
    }

    public boolean canUse() {
        return true;
    }

    protected AxisAlignedBB getTargetSearchArea(double area) {
        return this.worker.getBoundingBox().inflate(area, 8.0D, area);
    }

    protected void findAnimal() {
        AnimalEntity animalTarget = this.worker.level.getEntitiesOfClass(this.animal, null, this.worker, this.worker.getX(), this.worker.getY(), this.worker.getZ(), this.getTargetSearchArea(16D));
        if (animalTarget != null){
            if (animalTarget.canFallInLove() && !animalTarget.isBaby())
                this.target = animalTarget;
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.findAnimal();

        if (this.target != null){
            this.worker.getNavigation().moveTo(this.target, 0.85);

            if (target.closerThan(this.worker, 3) && target.canFallInLove() && !target.isBaby()){
                target.setInLove(null);

                this.worker.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ(), 10.0F, (float) this.worker.getMaxHeadXRot());

               if (!this.worker.swinging) {
                    this.worker.swing(this.worker.getUsedItemHand());
                }

            }
        }
    }

}