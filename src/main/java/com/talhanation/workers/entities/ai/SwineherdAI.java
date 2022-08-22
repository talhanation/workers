package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.CattleFarmerEntity;
import com.talhanation.workers.entities.SwineherdEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Predicates.not;

public class SwineherdAI extends Goal {
    private Optional<Pig> pig;
    private final SwineherdEntity swineherd;
    private boolean breeding;
    private boolean slaughtering;
    private BlockPos workPos;



    public SwineherdAI(SwineherdEntity worker, int coolDown) {
        this.swineherd = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.swineherd.level.isDay()) {
            return false;
        }
        else return swineherd.getIsWorking() && !swineherd.getFollow();
    }

    @Override
    public void start() {
        super.start();
        this.workPos = swineherd.getStartPos();
        this.breeding = true;
        this.slaughtering = false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!workPos.closerThan(swineherd.getOnPos(), 10D) && workPos != null && !swineherd.getFollow())
            this.swineherd.getNavigation().moveTo(workPos.getX(), workPos.getY(), workPos.getZ(), 1);


        if (breeding){
            this.pig = findCowMilking();
            if (this.pig.isPresent() ) {
                int i = pig.get().getAge();

                if (i == 0 && this.hasWheat()) {
                    this.swineherd.getNavigation().moveTo(this.pig.get(), 1);

                    if (pig.get().closerThan(this.swineherd, 1.5)) {
                        this.consumeWheat();
                        this.swineherd.getLookControl().setLookAt(pig.get().getX(), pig.get().getEyeY(), pig.get().getZ(), 10.0F, (float) this.swineherd.getMaxHeadXRot());
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

            List<Pig> cows = findCowSlaughtering();
            if (cows.size() > swineherd.getMaxAnimalCount()) {
                pig = cows.stream().findFirst();

                if (pig.isPresent()) {
                    this.swineherd.getNavigation().moveTo(pig.get().getX(), pig.get().getY(), pig.get().getZ(), 1);
                    if (pig.get().closerThan(this.swineherd, 1.5)) {
                        pig.get().kill();
                        swineherd.workerSwingArm();
                    }
                }

            }
            else {
                slaughtering = false;
            }
        }

    }

    private void consumeWheat(){
        SimpleContainer inventory = swineherd.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.WHEAT))
                itemStack.shrink(1);
        }
    }

    public void milkCow(Cow cow) {
       swineherd.workerSwingArm();

        SimpleContainer inventory = swineherd.getInventory();
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
        SimpleContainer inventory = swineherd.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.BUCKET)){
                return true;
            }
        }
        return false;
    }

    private Optional<Pig> findCowMilking() {
        return  swineherd.level.getEntitiesOfClass(Pig.class, swineherd.getBoundingBox()
                .inflate(8D), Pig::isAlive)
                .stream()
                .filter(not(Pig::isBaby))
                .filter(not(Pig::isInLove))
                .findAny();
    }

    private List<Pig> findCowSlaughtering() {
        return  swineherd.level.getEntitiesOfClass(Pig.class, swineherd.getBoundingBox()
                        .inflate(8D), Pig::isAlive)
                .stream()
                .filter(not(Pig::isBaby))
                .collect(Collectors.toList());
    }

    private boolean hasWheat() {
        SimpleContainer inventory = swineherd.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.WHEAT))
                if (itemStack.getCount() >= 2)
                    return true;
        }
        return false;
    }
}
