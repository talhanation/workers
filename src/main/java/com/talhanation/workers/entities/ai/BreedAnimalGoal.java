package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.talhanation.workers.entities.FarmerEntity.WANTED_SEEDS;


public class BreedAnimalGoal<T extends Animal> extends Goal {
    protected final Class<T> animalClass;
    public AbstractWorkerEntity worker;
    public Animal animal;
    Random random = new Random();

    public BreedAnimalGoal(AbstractWorkerEntity worker,Class<T> animalClass) {
        this.worker = worker;
        this.animalClass = animalClass;
    }

    public boolean canUse() {
        return true;
    }

    protected AABB getSearchArea(double area) {
        return this.worker.getBoundingBox().inflate(area, 8.0D, area);
    }

    protected void findAnimal() {
        List<T> list = Objects.requireNonNull(worker.level.getEntitiesOfClass(animalClass, this.getSearchArea(12)));
        Animal closeanimal;
        for (Animal animals : list){
            for (int i = 0; i < list.size(); i++) {
                closeanimal = animals;

                if (closeanimal != null && closeanimal.canFallInLove() && !closeanimal.isBaby()) {
                    this.animal = closeanimal;
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.animal != null) {
            int i = animal.getAge();

            if (i == 0 && this.hasWorkerWheat()) {
                this.worker.getNavigation().moveTo(this.animal, 1);

                if (animal.closerThan(this.worker, 3)) {
                    animal.setInLove(null);

                    this.worker.getLookControl().setLookAt(animal.getX(), animal.getEyeY(), animal.getZ(), 10.0F, (float) this.worker.getMaxHeadXRot());

                    if (!this.worker.swinging) {
                        this.worker.workerSwingArm();
                    }
                }
            }
        }
        findAnimal();
    }

    private boolean hasWorkerWheat() {
        SimpleContainer inventory = worker.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.WHEAT))
                return true;
        }
        return false;
    }

}