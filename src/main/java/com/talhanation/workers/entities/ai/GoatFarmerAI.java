package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.ShepherdEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Predicates.not;

public class GoatFarmerAI extends Goal {
    private Optional<Goat> goat;
    private final ShepherdEntity shepherd;
    private boolean milking;
    private boolean breeding;
    private boolean slaughtering;
    private BlockPos workPos;

    public GoatFarmerAI(ShepherdEntity worker, int coolDown) {
        this.shepherd = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.shepherd.level.isDay()) {
            return false;
        } else
            return shepherd.getStatus() == AbstractWorkerEntity.Status.WORK;
    }

    @Override
    public void start() {
        super.start();
        this.workPos = shepherd.getStartPos();
        this.milking = true;
        this.breeding = false;
        this.slaughtering = false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!workPos.closerThan(shepherd.getOnPos(), 10D) && workPos != null)
            this.shepherd.getNavigation().moveTo(workPos.getX(), workPos.getY(), workPos.getZ(), 1);

        if (milking) {
            this.goat = findGoatMilking();
            if (this.goat.isPresent() && hasBucket()) {
                this.shepherd.getNavigation().moveTo(this.goat.get(), 1);
                this.shepherd.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BUCKET));

                if (goat.get().closerThan(this.shepherd, 1.5)) {
                    milkCow(this.goat.get());
                    this.shepherd.getLookControl().setLookAt(goat.get().getX(), goat.get().getEyeY(), goat.get().getZ(),
                            10.0F, (float) this.shepherd.getMaxHeadXRot());

                    this.goat = Optional.empty();
                }
            } else {
                milking = false;
                breeding = true;
                this.shepherd.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }

        }

        if (breeding) {
            this.goat = findGoatMilking();
            if (this.goat.isPresent()) {
                int i = goat.get().getAge();

                if (i == 0 && this.hasWheat()) {
                    this.shepherd.getNavigation().moveTo(this.goat.get(), 1);
                    this.shepherd.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WHEAT));

                    if (goat.get().closerThan(this.shepherd, 1.5)) {
                        this.consumeWheat();
                        this.shepherd.getLookControl().setLookAt(goat.get().getX(), goat.get().getEyeY(),
                                goat.get().getZ(), 10.0F, (float) this.shepherd.getMaxHeadXRot());
                        goat.get().setInLove(null);
                        this.goat = Optional.empty();
                    }
                } else {
                    this.shepherd.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    breeding = false;
                    slaughtering = true;
                }
            } else {
                this.shepherd.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                breeding = false;
                slaughtering = true;
            }
        }

        if (slaughtering) {

            List<Goat> cows = findGoatSlaughtering();
            if (cows.size() > shepherd.getMaxAnimalCount()) {
                goat = cows.stream().findFirst();

                if (goat.isPresent()) {
                    this.shepherd.getNavigation().moveTo(goat.get().getX(), goat.get().getY(), goat.get().getZ(), 1);
                    if (goat.get().closerThan(this.shepherd, 1.5)) {
                        ItemStack stack = Items.MUTTON.getDefaultInstance();
                        stack.setCount(shepherd.getRandom().nextInt(2));
                        goat.get().spawnAtLocation(stack);
                        goat.get().kill();
                        shepherd.workerSwingArm();
                    }
                }

            } else {
                slaughtering = false;
                milking = true;
            }
        }

    }

    private void consumeWheat() {
        SimpleContainer inventory = shepherd.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.WHEAT)) {
                itemStack.shrink(1);
                break;
            }
        }
    }

    public void milkCow(Goat cow) {
        shepherd.workerSwingArm();

        SimpleContainer inventory = shepherd.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.BUCKET)) {
                itemStack.shrink(1);
            }
        }
        inventory.addItem(Items.MILK_BUCKET.getDefaultInstance());

        cow.playSound(cow.isScreamingGoat() ? SoundEvents.GOAT_SCREAMING_MILK : SoundEvents.GOAT_MILK, 1.0F, 1.0F);
    }

    public boolean hasBucket() {
        SimpleContainer inventory = shepherd.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.BUCKET)) {
                return true;
            }
        }
        return false;
    }

    private Optional<Goat> findGoatMilking() {
        return shepherd.level.getEntitiesOfClass(Goat.class, shepherd.getBoundingBox().inflate(8D), Goat::isAlive)
                .stream().filter(not(Goat::isBaby)).filter(not(Goat::isInLove)).findAny();
    }

    private List<Goat> findGoatSlaughtering() {
        return shepherd.level.getEntitiesOfClass(Goat.class, shepherd.getBoundingBox().inflate(8D), Goat::isAlive)
                .stream().filter(not(Goat::isBaby)).filter(not(Goat::isInLove)).collect(Collectors.toList());
    }

    private boolean hasWheat() {
        SimpleContainer inventory = shepherd.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.WHEAT))
                if (itemStack.getCount() >= 2)
                    return true;
        }
        return false;
    }
}
