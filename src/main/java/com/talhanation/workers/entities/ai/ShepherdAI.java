package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.ShepherdEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static java.util.function.Predicate.not;

public class ShepherdAI extends Goal {
    private Optional<Sheep> sheep;
    private final ShepherdEntity shepherd;
    private boolean sheering;
    private boolean breeding;
    private boolean slaughtering;


    public ShepherdAI(ShepherdEntity worker, int coolDown) {
        this.shepherd = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return shepherd.getIsWorking() && !shepherd.getFollow();
    }

    @Override
    public void start() {
        super.start();
        this.sheering = true;
        this.breeding = false;
        this.slaughtering = false;
    }

    @Override
    public void tick() {
        super.tick();




        if (sheering){
            this.sheep = findSheepSheering();
            if (this.sheep.isPresent() && sheep.get().readyForShearing()) {
                this.shepherd.getNavigation().moveTo(this.sheep.get(), 1);

                if (sheep.get().closerThan(this.shepherd, 1.5)) {
                    sheerSheep(this.sheep.get());
                    this.shepherd.getLookControl().setLookAt(sheep.get().getX(), sheep.get().getEyeY(), sheep.get().getZ(), 10.0F, (float) this.shepherd.getMaxHeadXRot());
                    //timer = 0;
                    this.sheep = Optional.empty();
                }
            }
            else {
                sheering = false;
                breeding = true;
            }

        }

        if (breeding){
            this.sheep = findSheepBreeding();
            if (this.sheep.isPresent() ) {
                int i = sheep.get().getAge();

                if (i == 0 && this.hasWheat()) {
                    this.shepherd.getNavigation().moveTo(this.sheep.get(), 1);

                    if (sheep.get().closerThan(this.shepherd, 1.5)) {
                        this.consumeWheat();
                        this.shepherd.getLookControl().setLookAt(sheep.get().getX(), sheep.get().getEyeY(), sheep.get().getZ(), 10.0F, (float) this.shepherd.getMaxHeadXRot());
                        sheep.get().setInLove(null);
                        this.sheep = Optional.empty();
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

            List<Sheep> sheeps = findSheepSlaughtering();
            if (sheeps.size() > shepherd.getMaxSheepCount()) {
                sheep = sheeps.stream().findFirst();

                if (sheep.isPresent()) {
                    this.shepherd.getNavigation().moveTo(sheep.get().getX(), sheep.get().getY(), sheep.get().getZ(), 1);
                    if (sheep.get().getOnPos().closerThan(shepherd.getOnPos(), 1.5)) {
                        sheep.get().kill();
                        shepherd.workerSwingArm();
                    }
                }

            }
            else {
                slaughtering = false;
                sheering = true;
            }
        }

    }

    private void consumeWheat(){
        SimpleContainer inventory = shepherd.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.WHEAT))
                itemStack.shrink(1);
        }
    }

    public void sheerSheep(Sheep sheepEntity) {
        sheepEntity.shear(SoundSource.PLAYERS);
        sheepEntity.setSheared(true);
        if (!this.shepherd.swinging) {
            this.shepherd.workerSwingArm();
        }
    }

    private Optional<Sheep> findSheepSheering() {
        return  shepherd.level.getEntitiesOfClass(Sheep.class, shepherd.getBoundingBox()
                .inflate(8D), Sheep::readyForShearing)
                .stream()
                .filter(not(Sheep::isBaby))
                .findAny();
    }

    private Optional<Sheep> findSheepBreeding() {
        return  shepherd.level.getEntitiesOfClass(Sheep.class, shepherd.getBoundingBox()
            .inflate(8D), Sheep::canFallInLove)
            .stream()
            .filter(not(Sheep::isBaby))
            .filter(not(Sheep::isInLove))
            .findAny();
    }
    private List<Sheep> findSheepSlaughtering() {
        return  shepherd.level.getEntitiesOfClass(Sheep.class, shepherd.getBoundingBox()
                        .inflate(8D), Sheep::isAlive)
                .stream()
                .filter(not(Sheep::isBaby))
                .toList();
    }

    private boolean hasWheat() {
        SimpleContainer inventory = shepherd.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.WHEAT))
                if (itemStack.getCount() >= 2)
                return true;
        }
        return false;
    }
}
