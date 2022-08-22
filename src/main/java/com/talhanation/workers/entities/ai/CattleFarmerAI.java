package com.talhanation.workers.entities.ai;


import com.talhanation.workers.entities.CattleFarmerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Predicates.not;

public class CattleFarmerAI extends Goal {
    private Optional<Cow> cow;
    private final CattleFarmerEntity cattleFarmer;
    private boolean milking;
    private boolean breeding;
    private boolean slaughtering;
    private BlockPos workPos;



    public CattleFarmerAI(CattleFarmerEntity worker, int coolDown) {
        this.cattleFarmer = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.cattleFarmer.level.isDay()) {
            return false;
        }
        else return cattleFarmer.getIsWorking() && !cattleFarmer.getFollow();
    }

    @Override
    public void start() {
        super.start();
        this.workPos = cattleFarmer.getStartPos();
        this.milking = true;
        this.breeding = false;
        this.slaughtering = false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!workPos.closerThan(cattleFarmer.getOnPos(), 10D) && workPos != null && !cattleFarmer.getFollow())
            this.cattleFarmer.getNavigation().moveTo(workPos.getX(), workPos.getY(), workPos.getZ(), 1);

        if (milking){
            this.cow = findCowMilking();
            if (this.cow.isPresent() && hasBucket()) {
                this.cattleFarmer.getNavigation().moveTo(this.cow.get(), 1);

                if (cow.get().closerThan(this.cattleFarmer, 1.5)) {
                    milkCow(this.cow.get());
                    this.cattleFarmer.getLookControl().setLookAt(cow.get().getX(), cow.get().getEyeY(), cow.get().getZ(), 10.0F, (float) this.cattleFarmer.getMaxHeadXRot());

                    this.cow = Optional.empty();
                }
            }
            else {
                milking = false;
                breeding = true;
            }

        }

        if (breeding){
            this.cow = findCowMilking();
            if (this.cow.isPresent() ) {
                int i = cow.get().getAge();

                if (i == 0 && this.hasWheat()) {
                    this.cattleFarmer.getNavigation().moveTo(this.cow.get(), 1);

                    if (cow.get().closerThan(this.cattleFarmer, 1.5)) {
                        this.consumeWheat();
                        this.cattleFarmer.getLookControl().setLookAt(cow.get().getX(), cow.get().getEyeY(), cow.get().getZ(), 10.0F, (float) this.cattleFarmer.getMaxHeadXRot());
                        cow.get().setInLove(null);
                        this.cow = Optional.empty();
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

            List<Cow> cows = findCowSlaughtering();
            if (cows.size() > cattleFarmer.getMaxAnimalCount()) {
                cow = cows.stream().findFirst();

                if (cow.isPresent()) {
                    this.cattleFarmer.getNavigation().moveTo(cow.get().getX(), cow.get().getY(), cow.get().getZ(), 1);
                    if (cow.get().closerThan(this.cattleFarmer, 1.5)) {
                        cow.get().kill();
                        cattleFarmer.workerSwingArm();
                    }
                }

            }
            else {
                slaughtering = false;
                milking = true;
            }
        }

    }

    private void consumeWheat(){
        SimpleContainer inventory = cattleFarmer.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.WHEAT))
                itemStack.shrink(1);
        }
    }

    public void milkCow(Cow cow) {
       cattleFarmer.workerSwingArm();

        SimpleContainer inventory = cattleFarmer.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.BUCKET)){
                itemStack.shrink(1);
            }
        }
        inventory.addItem(Items.MILK_BUCKET.getDefaultInstance());

        cow.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
    }

    public boolean hasBucket(){
        SimpleContainer inventory = cattleFarmer.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.BUCKET)){
                return true;
            }
        }
        return false;
    }

    private Optional<Cow> findCowMilking() {
        return  cattleFarmer.level.getEntitiesOfClass(Cow.class, cattleFarmer.getBoundingBox()
                .inflate(8D), Cow::isAlive)
                .stream()
                .filter(not(Cow::isBaby))
                .filter(not(Cow::isInLove))
                .findAny();
    }

    private List<Cow> findCowSlaughtering() {
        return  cattleFarmer.level.getEntitiesOfClass(Cow.class, cattleFarmer.getBoundingBox()
                        .inflate(8D), Cow::isAlive)
                .stream()
                .filter(not(Cow::isBaby))
                .filter(not(Cow::isInLove))
                .collect(Collectors.toList());
    }

    private boolean hasWheat() {
        SimpleContainer inventory = cattleFarmer.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.WHEAT))
                if (itemStack.getCount() >= 2)
                    return true;
        }
        return false;
    }
}
