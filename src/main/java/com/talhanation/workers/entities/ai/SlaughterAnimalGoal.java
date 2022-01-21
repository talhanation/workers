package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class SlaughterAnimalGoal<T extends AnimalEntity> extends Goal {

    private final AbstractWorkerEntity worker;
    private AnimalEntity animal;
    protected final Class<T> animalClass;
    private final int maxCount;

    public SlaughterAnimalGoal(AbstractWorkerEntity worker, Class<T> animalClass, int maxCount) {
        this.worker = worker;
        this.animalClass = animalClass;
        this.maxCount = maxCount;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        //return !worker.isSheering() && !worker.isBreeding();
        return worker.getIsWorking();
    }


    @Override
    public void tick() {
        super.tick();

        List<AnimalEntity> list = Objects.requireNonNull(worker.level.getEntitiesOfClass(animalClass, this.getSearchArea(12)));
        for (AnimalEntity animals : list){
            if (animals.getClass().equals(this.animalClass) && animals.isAlive() && !animals.isBaby()){
                this.animal = list.get(0);
            }

            if (list.size() > maxCount){
                if (animal != null) {
                    this.worker.getNavigation().moveTo(animal.getX(), animal.getY(), animal.getZ(), 1);
                    if (animal.blockPosition().closerThan(worker.position(), 2)) animal.kill();
                    worker.workerSwingArm();
                }
            }
        }
    }

    protected AxisAlignedBB getSearchArea(double area) {
        return this.worker.getBoundingBox().inflate(area, 8.0D, area);
    }

}
