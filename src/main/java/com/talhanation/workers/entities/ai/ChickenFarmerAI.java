package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.ChickenFarmerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Predicates.not;

public class ChickenFarmerAI extends AnimalFarmerAI {
    private Optional<Chicken> chicken;
    private boolean breeding;
    private boolean slaughtering;
    private BlockPos workPos;

    public ChickenFarmerAI(ChickenFarmerEntity worker) {
        this.animalFarmer = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.animalFarmer.canWork();
    }

    @Override
    public void start() {
        super.start();
        this.workPos = animalFarmer.getStartPos();
        this.breeding = true;
        this.slaughtering = false;
    }

    @Override
    public void performWork() {
        if (workPos != null && !workPos.closerThan(animalFarmer.getOnPos(), 10D) && !animalFarmer.getFollow())
            this.animalFarmer.getNavigation().moveTo(workPos.getX(), workPos.getY(), workPos.getZ(), 1);

        if (breeding) {
            this.chicken = findChickenBreeding();
            if (this.chicken.isPresent()) {
                int i = chicken.get().getAge();

                if (i == 0 && this.hasSeeds()) {
                    this.animalFarmer.changeToBreedItem(Items.WHEAT_SEEDS);

                    this.animalFarmer.getNavigation().moveTo(this.chicken.get(), 1);

                    if (chicken.get().closerThan(this.animalFarmer, 2)) {
                        this.consumeSeed();
                        this.animalFarmer.getLookControl().setLookAt(chicken.get().getX(), chicken.get().getEyeY(),
                                chicken.get().getZ(), 10.0F, (float) this.animalFarmer.getMaxHeadXRot());
                        chicken.get().setInLove(null);
                        animalFarmer.workerSwingArm();
                        this.chicken = Optional.empty();
                    }
                } else {
                    breeding = false;
                    slaughtering = true;
                }
            } else {
                breeding = false;
                slaughtering = true;
            }
        }

        if (slaughtering) {
            List<Chicken> chickens = findChickenSlaughtering();
            if (chickens.size() > animalFarmer.getMaxAnimalCount()) {
                chicken = chickens.stream().findFirst();

                if (chicken.isPresent()) {

                    if(!animalFarmer.isRequiredMainTool(animalFarmer.getMainHandItem())) this.animalFarmer.changeToTool(true);

                    this.animalFarmer.getNavigation().moveTo(chicken.get().getX(), chicken.get().getY(),
                            chicken.get().getZ(), 1);
                    if (chicken.get().closerThan(this.animalFarmer, 2)) {
                        chicken.get().kill();

                        animalFarmer.workerSwingArm();
                        animalFarmer.increaseFarmedItems();
                        animalFarmer.consumeToolDurability();
                    }
                }

            } else {
                slaughtering = false;
                breeding = true;
            }
        }

    }

    private void consumeSeed() {
        SimpleContainer inventory = animalFarmer.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.WHEAT_SEEDS) || itemStack.getItem().equals(Items.MELON_SEEDS)
                    || itemStack.getItem().equals(Items.BEETROOT_SEEDS)
                    || itemStack.getItem().equals(Items.PUMPKIN_SEEDS)) {
                itemStack.shrink(1);
                break;
            }

        }
    }

    private Optional<Chicken> findChickenBreeding() {
        return animalFarmer.level
                .getEntitiesOfClass(Chicken.class, animalFarmer.getBoundingBox().inflate(8D), Chicken::isAlive)
                .stream().filter(not(Chicken::isBaby)).filter(not(Chicken::isInLove)).findAny();
    }

    private List<Chicken> findChickenSlaughtering() {
        return animalFarmer.level
                .getEntitiesOfClass(Chicken.class, animalFarmer.getBoundingBox().inflate(8D), Chicken::isAlive)
                .stream().filter(not(Chicken::isBaby)).filter(not(Chicken::isInLove)).collect(Collectors.toList());
    }

    private boolean hasSeeds() {
        SimpleContainer inventory = animalFarmer.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.WHEAT_SEEDS) || itemStack.getItem().equals(Items.MELON_SEEDS)
                    || itemStack.getItem().equals(Items.BEETROOT_SEEDS)
                    || itemStack.getItem().equals(Items.PUMPKIN_SEEDS))
                if (itemStack.getCount() >= 2)
                    return true;
        }
        return false;
    }
}
