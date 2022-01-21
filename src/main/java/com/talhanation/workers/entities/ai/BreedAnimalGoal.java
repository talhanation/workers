package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.talhanation.workers.entities.FarmerEntity.WANTED_SEEDS;


public class BreedAnimalGoal<T extends AnimalEntity> extends Goal {
    protected final Class<T> animalClass;
    public AbstractWorkerEntity worker;
    public AnimalEntity animal;
    Random random = new Random();

    public BreedAnimalGoal(AbstractWorkerEntity worker,Class<T> animalClass) {
        this.worker = worker;
        this.animalClass = animalClass;
    }

    public boolean canUse() {
        return true;
    }

    protected AxisAlignedBB getSearchArea(double area) {
        return this.worker.getBoundingBox().inflate(area, 8.0D, area);
    }

    protected void findAnimal() {
        List<AnimalEntity> list = Objects.requireNonNull(worker.level.getEntitiesOfClass(animalClass, this.getSearchArea(12)));
        AnimalEntity closeanimal;
        for (AnimalEntity animals : list){
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
        Inventory inventory = worker.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.WHEAT))
                return true;
        }
        return false;
    }

}