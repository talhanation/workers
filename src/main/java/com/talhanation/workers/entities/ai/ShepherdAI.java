package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.ShepherdEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Predicates.not;

public class ShepherdAI extends AnimalFarmerAI{
    private Optional<Sheep> sheep;
    private boolean sheering;
    private boolean breeding;
    private boolean slaughtering;
    private BlockPos workPos;

    public ShepherdAI(ShepherdEntity worker) {
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
        this.sheering = true;
        this.breeding = false;
        this.slaughtering = false;
    }

    @Override
    public void performWork() {

        if (workPos != null && !workPos.closerThan(animalFarmer.getOnPos(), 10D) && !animalFarmer.getFollow())
            this.animalFarmer.getNavigation().moveTo(workPos.getX(), workPos.getY(), workPos.getZ(), 1);

        if (sheering) {
            this.sheep = findSheepSheering();
            if (this.sheep.isPresent() && sheep.get().readyForShearing() && hasMainToolInInv()) {

                this.animalFarmer.getNavigation().moveTo(this.sheep.get(), 1);

                if(!animalFarmer.isRequiredMainTool(animalFarmer.getMainHandItem())) this.animalFarmer.changeToTool(true);

                if (sheep.get().closerThan(this.animalFarmer, 2)) {
                    this.sheerSheep(this.sheep.get());
                    this.animalFarmer.getLookControl().setLookAt(sheep.get().getX(), sheep.get().getEyeY(), sheep.get().getZ(), 10.0F, (float) this.animalFarmer.getMaxHeadXRot());
                    this.sheep = Optional.empty();
                }
            } else {
                sheering = false;
                breeding = true;
            }

        }

        if (breeding) {
            this.sheep = findSheepBreeding();
            if (this.sheep.isPresent()) {
                int i = sheep.get().getAge();

                if (i == 0 && this.hasBreedItem(Items.WHEAT)) {
                    this.animalFarmer.changeToBreedItem(Items.WHEAT);

                    this.animalFarmer.getNavigation().moveTo(this.sheep.get(), 1);
                    if (sheep.get().closerThan(this.animalFarmer, 2)) {
                        this.animalFarmer.workerSwingArm();
                        this.consumeBreedItem(Items.WHEAT);
                        this.animalFarmer.getLookControl().setLookAt(sheep.get().getX(), sheep.get().getEyeY(),
                                sheep.get().getZ(), 10.0F, (float) this.animalFarmer.getMaxHeadXRot());
                        sheep.get().setInLove(null);
                        this.sheep = Optional.empty();
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
            List<Sheep> sheeps = findSheepSlaughtering();
            if (sheeps.size() > animalFarmer.getMaxAnimalCount()) {
                sheep = sheeps.stream().findFirst();

                if (sheep.isPresent() && hasSecondToolInInv()) {
                    this.animalFarmer.getNavigation().moveTo(sheep.get().getX(), sheep.get().getY(), sheep.get().getZ(), 1);

                    if(!animalFarmer.isRequiredSecondTool(animalFarmer.getMainHandItem())) this.animalFarmer.changeToTool(false);

                    if (sheep.get().getOnPos().closerThan(animalFarmer.getOnPos(), 2)) {

                        animalFarmer.workerSwingArm();

                        sheep.get().kill();
                        animalFarmer.playSound(SoundEvents.PLAYER_ATTACK_STRONG);

                        this.animalFarmer.consumeToolDurability();
                        animalFarmer.increaseFarmedItems();
                    }
                } else {
                    slaughtering = false;
                    sheering = true;
                }
            } else {
                slaughtering = false;
                sheering = true;
            }
        }
    }

    public void sheerSheep(Sheep sheepEntity) {
        sheepEntity.shear(SoundSource.PLAYERS);
        sheepEntity.setSheared(true);
        animalFarmer.increaseFarmedItems();

        if (!this.animalFarmer.swinging) {
            this.animalFarmer.workerSwingArm();
        }
        this.animalFarmer.consumeToolDurability();
    }

    private Optional<Sheep> findSheepSheering() {
        return animalFarmer.level
                .getEntitiesOfClass(Sheep.class, animalFarmer.getBoundingBox().inflate(8D), Sheep::readyForShearing)
                .stream().filter(not(Sheep::isBaby)).findAny();
    }

    private Optional<Sheep> findSheepBreeding() {
        return animalFarmer.level
                .getEntitiesOfClass(Sheep.class, animalFarmer.getBoundingBox().inflate(8D), Sheep::canFallInLove).stream()
                .filter(not(Sheep::isBaby)).filter(not(Sheep::isInLove)).findAny();
    }

    private List<Sheep> findSheepSlaughtering() {
        return animalFarmer.level.getEntitiesOfClass(Sheep.class, animalFarmer.getBoundingBox().inflate(8D), Sheep::isAlive)
                .stream().filter(not(Sheep::isBaby)).filter(not(Sheep::isInLove)).collect(Collectors.toList());

    }

}
