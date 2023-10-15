package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.SwineherdEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Predicates.not;

public class SwineherdAI extends AnimalFarmerAI {
    private Optional<Pig> pig;
    private boolean breeding;
    private boolean slaughtering;
    private BlockPos workPos;

    public SwineherdAI(SwineherdEntity worker) {
        this.animalFarmer = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return animalFarmer.canWork();
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
        if (!workPos.closerThan(animalFarmer.getOnPos(), 10D) && workPos != null && !animalFarmer.getFollow())
            this.animalFarmer.getNavigation().moveTo(workPos.getX(), workPos.getY(), workPos.getZ(), 1);


        if (breeding){
            this.pig = findPigBreeding();
            if (this.pig.isPresent() ) {
                int i = pig.get().getAge();

                if (i == 0 && this.hasBreedItem(Items.CARROT)) {
                    this.animalFarmer.getNavigation().moveTo(this.pig.get(), 1);
                    this.animalFarmer.changeToBreedItem(Items.CARROT);

                    if (pig.get().closerThan(this.animalFarmer, 2)) {
                        this.animalFarmer.workerSwingArm();
                        this.consumeBreedItem(Items.CARROT);
                        this.animalFarmer.getLookControl().setLookAt(pig.get().getX(), pig.get().getEyeY(), pig.get().getZ(), 10.0F, (float) this.animalFarmer.getMaxHeadXRot());
                        pig.get().setInLove(null);
                        this.pig = Optional.empty();
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
            List<Pig> cows = findPigSlaughtering();
            if (cows.size() > animalFarmer.getMaxAnimalCount()) {
                pig = cows.stream().findFirst();

                if (pig.isPresent()) {
                    if(!animalFarmer.isRequiredMainTool(animalFarmer.getMainHandItem())) this.animalFarmer.changeToTool(true);
                    this.animalFarmer.getNavigation().moveTo(pig.get().getX(), pig.get().getY(), pig.get().getZ(), 1);

                    if (pig.get().closerThan(this.animalFarmer, 2)) {
                        pig.get().kill();

                        this.animalFarmer.workerSwingArm();
                        this.animalFarmer.playSound(SoundEvents.PLAYER_ATTACK_STRONG,1.0F, 1.0F);
                        this.animalFarmer.consumeToolDurability();
                        this.animalFarmer.increaseFarmedItems();
                    }
                }
                else {
                    slaughtering = false;
                    breeding = true;
                }
            }
            else {
                slaughtering = false;
                breeding = true;
            }
        }

    }
    private Optional<Pig> findPigBreeding() {
        return  this.animalFarmer.level.getEntitiesOfClass(Pig.class, this.animalFarmer.getBoundingBox()
                .inflate(8D), Pig::isAlive)
                .stream()
                .filter(not(Pig::isBaby))
                .filter(not(Pig::isInLove))
                .findAny();
    }
    private List<Pig> findPigSlaughtering() {
        return this.animalFarmer.level.getEntitiesOfClass(Pig.class, this.animalFarmer.getBoundingBox()
                        .inflate(8D), Pig::isAlive)
                .stream()
                .filter(not(Pig::isBaby))
                .filter(not(Pig::isInLove))
                .collect(Collectors.toList());
    }
}
