package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.ChickenFarmerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Predicates.not;

public class ChickenFarmerAI extends Goal {
    private Optional<Chicken> chicken;
    private final ChickenFarmerEntity chickenFarmer;
    private boolean breeding;
    private boolean slaughtering;
    private BlockPos workPos;


    public ChickenFarmerAI(ChickenFarmerEntity worker) {
        this.chickenFarmer = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.chickenFarmer.level.isDay()) {
            return false;
        }
        else return chickenFarmer.getIsWorking() && !chickenFarmer.getFollow();
    }

    @Override
    public void start() {
        super.start();
        this.workPos = chickenFarmer.getStartPos();
        this.breeding = true;
        this.slaughtering = false;
    }

    @Override
    public void tick() {
        super.tick();
        if ( workPos != null && !workPos.closerThan(chickenFarmer.getOnPos(), 10D) && !chickenFarmer.getFollow())
            this.chickenFarmer.getNavigation().moveTo(workPos.getX(), workPos.getY(), workPos.getZ(), 1);

        if (breeding){
            this.chicken = findChickenBreeding();
            if (this.chicken.isPresent() ) {
                int i = chicken.get().getAge();

                if (i == 0 && this.hasSeeds()) {
                    this.chickenFarmer.getNavigation().moveTo(this.chicken.get(), 1);

                    if (chicken.get().closerThan(this.chickenFarmer, 1.5)) {
                        this.consumeSeed();
                        this.chickenFarmer.getLookControl().setLookAt(chicken.get().getX(), chicken.get().getEyeY(), chicken.get().getZ(), 10.0F, (float) this.chickenFarmer.getMaxHeadXRot());
                        chicken.get().setInLove(null);
                        this.chicken = Optional.empty();
                    }
                }
                else {
                    breeding = false;
                    slaughtering = true;
                }
            } else {
                breeding = false;
                slaughtering = true;
            }
        }


        if (slaughtering) {

            List<Chicken> cows = findChickenSlaughtering();
            if (cows.size() > chickenFarmer.getMaxAnimalCount()) {
                chicken = cows.stream().findFirst();

                if (chicken.isPresent()) {
                    this.chickenFarmer.getNavigation().moveTo(chicken.get().getX(), chicken.get().getY(), chicken.get().getZ(), 1);
                    if (chicken.get().closerThan(this.chickenFarmer, 1.5)) {
                        chicken.get().kill();
                        chickenFarmer.workerSwingArm();
                    }
                }

            }
            else {
                slaughtering = false;
                breeding = true;
            }
        }

    }

    private void consumeSeed(){
        SimpleContainer inventory = chickenFarmer.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.WHEAT_SEEDS) || itemStack.getItem().equals(Items.MELON_SEEDS) || itemStack.getItem().equals(Items.BEETROOT_SEEDS) || itemStack.getItem().equals(Items.PUMPKIN_SEEDS))
                itemStack.shrink(1);
        }
    }


    private Optional<Chicken> findChickenBreeding() {
        return  chickenFarmer.level.getEntitiesOfClass(Chicken.class, chickenFarmer.getBoundingBox()
                .inflate(8D), Chicken::isAlive)
                .stream()
                .filter(not(Chicken::isBaby))
                .filter(not(Chicken::isInLove))
                .findAny();
    }

    private List<Chicken> findChickenSlaughtering() {
        return  chickenFarmer.level.getEntitiesOfClass(Chicken.class, chickenFarmer.getBoundingBox()
                        .inflate(8D), Chicken::isAlive)
                .stream()
                .filter(not(Chicken::isBaby))
                .filter(not(Chicken::isInLove))
                .collect(Collectors.toList());
    }

    private boolean hasSeeds() {
        SimpleContainer inventory = chickenFarmer.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.WHEAT_SEEDS) || itemStack.getItem().equals(Items.MELON_SEEDS) || itemStack.getItem().equals(Items.BEETROOT_SEEDS) || itemStack.getItem().equals(Items.PUMPKIN_SEEDS))
                if (itemStack.getCount() >= 2)
                    return true;
        }
        return false;
    }
}
