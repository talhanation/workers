package com.talhanation.workers.entities.ai;


import com.talhanation.workers.Translatable;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.CattleFarmerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Predicates.not;

public class CattleFarmerAI extends AnimalFarmerAI {
    private Optional<Cow> cow;
    private boolean milking;
    private boolean breeding;
    private boolean slaughtering;
    private BlockPos workPos;

    public CattleFarmerAI(CattleFarmerEntity worker) {
        this.animalFarmer = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return animalFarmer.getStatus() == AbstractWorkerEntity.Status.WORK;
    }

    @Override
    public void start() {
        super.start();
        this.workPos = animalFarmer.getStartPos();
        this.milking = true;
        this.breeding = false;
        this.slaughtering = false;
    }

    @Override
    public void performWork() {
        if (!workPos.closerThan(animalFarmer.getOnPos(), 10D) && workPos != null)
            this.animalFarmer.getNavigation().moveTo(workPos.getX(), workPos.getY(), workPos.getZ(), 1);

        if (milking){
            this.cow = findCowMilking();
            if (this.cow.isPresent() && hasBucket()) {
                this.animalFarmer.getNavigation().moveTo(this.cow.get(), 1);

                if(!animalFarmer.isRequiredMainTool(animalFarmer.getMainHandItem())) this.animalFarmer.changeToTool(true);

                if (cow.get().closerThan(this.animalFarmer, 2)) {

                    this.animalFarmer.getLookControl().setLookAt(cow.get().getX(), cow.get().getEyeY(), cow.get().getZ(), 10.0F, (float) this.animalFarmer.getMaxHeadXRot());

                    animalFarmer.workerSwingArm();
                    animalFarmer.increaseFarmedItems();
                    milkCow(this.cow.get());
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

                if (i == 0 && this.hasBreedItem(Items.WHEAT)) {
                    this.animalFarmer.getNavigation().moveTo(this.cow.get(), 1);
                    this.animalFarmer.changeToBreedItem(Items.WHEAT);

                    if (cow.get().closerThan(this.animalFarmer, 2)) {

                        this.animalFarmer.getLookControl().setLookAt(cow.get().getX(), cow.get().getEyeY(), cow.get().getZ(), 10.0F, (float) this.animalFarmer.getMaxHeadXRot());

                        this.consumeBreedItem(Items.WHEAT);
                        animalFarmer.workerSwingArm();
                        this.cow.get().setInLove(null);
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
            if (cows.size() > animalFarmer.getMaxAnimalCount() && animalFarmer.hasMainToolInInv()) {
                cow = cows.stream().findFirst();

                if(!animalFarmer.isRequiredSecondTool(animalFarmer.getMainHandItem())) this.animalFarmer.changeToTool(false);

                if (cow.isPresent()) {
                    this.animalFarmer.getNavigation().moveTo(cow.get().getX(), cow.get().getY(), cow.get().getZ(), 1);
                    if (cow.get().closerThan(this.animalFarmer, 2)) {
                        cow.get().kill();
                        animalFarmer.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1F, 1F);

                        this.animalFarmer.consumeToolDurability();
                        animalFarmer.increaseFarmedItems();
                        animalFarmer.workerSwingArm();
                    }
                }

            }
            else {
                if(!animalFarmer.hasMainToolInInv()){
                    this.animalFarmer.needsMainTool = true;
                    this.animalFarmer.updateNeedsTool();
                }
                slaughtering = false;
                milking = true;
            }
        }

    }

    public void milkCow(Cow cow) {
        animalFarmer.workerSwingArm();

        SimpleContainer inventory = animalFarmer.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.BUCKET)){
                itemStack.shrink(1);
            }
        }
        inventory.addItem(Items.MILK_BUCKET.getDefaultInstance());
        animalFarmer.increaseFarmedItems();
        cow.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
    }

    public boolean hasBucket(){
        SimpleContainer inventory = animalFarmer.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.BUCKET)){
                return true;
            }
        }
        return false;
    }

    private Optional<Cow> findCowMilking() {
        return  animalFarmer.level.getEntitiesOfClass(Cow.class, animalFarmer.getBoundingBox()
                .inflate(8D), Cow::isAlive)
                .stream()
                .filter(not(Cow::isBaby))
                .filter(not(Cow::isInLove))
                .findAny();
    }

    private List<Cow> findCowSlaughtering() {
        return  animalFarmer.level.getEntitiesOfClass(Cow.class, animalFarmer.getBoundingBox()
                        .inflate(8D), Cow::isAlive)
                .stream()
                .filter(not(Cow::isBaby))
                .filter(not(Cow::isInLove))
                .collect(Collectors.toList());
    }
}
