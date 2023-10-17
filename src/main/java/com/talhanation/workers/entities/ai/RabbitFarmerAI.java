package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.RabbitFarmerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RabbitFarmerAI extends Goal {
    private Optional<Rabbit> rabbit;
    private final RabbitFarmerEntity rabbitFarmer;
    private boolean breeding;
    private boolean slaughtering;
    private BlockPos workPos;


    public RabbitFarmerAI(RabbitFarmerEntity worker) {
        this.rabbitFarmer = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.rabbitFarmer.getCommandSenderWorld().isDay()) {
            return false;
        }
        else return rabbitFarmer.getIsWorking() && !rabbitFarmer.getFollow();
    }

    @Override
    public void start() {
        super.start();
        this.workPos = rabbitFarmer.getStartPos();
        this.breeding = true;
        this.slaughtering = false;
    }

    @Override
    public void tick() {
        super.tick();
        if (workPos != null && !workPos.closerThan(rabbitFarmer.getOnPos(), 10D) && !rabbitFarmer.getFollow())
            this.rabbitFarmer.getNavigation().moveTo(workPos.getX(), workPos.getY(), workPos.getZ(), 1);

        if (breeding){
            this.rabbit = findRabbitBreeding();
            if (this.rabbit.isPresent() ) {
                int i = rabbit.get().getAge();

                if (i == 0 && this.hasSeeds()) {
                    this.rabbitFarmer.getNavigation().moveTo(this.rabbit.get(), 1);
                    this.rabbitFarmer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WHEAT_SEEDS));

                    if (rabbit.get().closerThan(this.rabbitFarmer, 1.5)) {
                        this.consumeSeed();
                        this.rabbitFarmer.getLookControl().setLookAt(rabbit.get().getX(), rabbit.get().getEyeY(), rabbit.get().getZ(), 10.0F, (float) this.rabbitFarmer.getMaxHeadXRot());
                        rabbit.get().setInLove(null);
                        this.rabbit = Optional.empty();
                    }
                }
                else {
                    breeding = false;
                    slaughtering = true;
                    this.rabbitFarmer.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                }
            } else {
                breeding = false;
                slaughtering = true;
                this.rabbitFarmer.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
        }


        if (slaughtering) {
            List<Rabbit> rabbits = findRabbitSlaughtering();
            if (rabbits.size() > rabbitFarmer.getMaxAnimalCount()) {
                rabbit = rabbits.stream().findFirst();

                if (rabbit.isPresent()) {
                    this.rabbitFarmer.getNavigation().moveTo(rabbit.get().getX(), rabbit.get().getY(), rabbit.get().getZ(), 1);
                    if (rabbit.get().closerThan(this.rabbitFarmer, 1.5)) {
                        rabbit.get().kill();
                        rabbitFarmer.workerSwingArm();
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
        SimpleContainer inventory = rabbitFarmer.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.DANDELION) || itemStack.getItem().equals(Items.CARROT) || itemStack.getItem().equals(Items.GOLDEN_CARROT)){
                itemStack.shrink(1);
                break;
            }
        }
    }


    private Optional<Rabbit> findRabbitBreeding() {
        return  rabbitFarmer.getCommandSenderWorld().getEntitiesOfClass(Rabbit.class, rabbitFarmer.getBoundingBox()
                .inflate(8D), Rabbit::isAlive)
                .stream()
                .filter(((Predicate<Rabbit>) Rabbit::isBaby).negate())
                .filter(((Predicate<Rabbit>) Rabbit::isInLove).negate())
                .findAny();
    }

    private List<Rabbit> findRabbitSlaughtering() {
        return  rabbitFarmer.getCommandSenderWorld().getEntitiesOfClass(Rabbit.class, rabbitFarmer.getBoundingBox()
                        .inflate(8D), Rabbit::isAlive)
                .stream()
                .filter(((Predicate<Rabbit>) Rabbit::isBaby).negate())
                .filter(((Predicate<Rabbit>) Rabbit::isInLove).negate())
                .collect(Collectors.toList());
    }

    private boolean hasSeeds() {
        SimpleContainer inventory = rabbitFarmer.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.DANDELION) || itemStack.getItem().equals(Items.CARROT) || itemStack.getItem().equals(Items.GOLDEN_CARROT))
                if (itemStack.getCount() >= 2)
                    return true;
        }
        return false;
    }
}
