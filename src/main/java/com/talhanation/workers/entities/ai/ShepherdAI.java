package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.ShepherdEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.StringTextComponent;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class ShepherdAI extends Goal {
    private SheepEntity sheep;
    private final AbstractWorkerEntity worker;
    private boolean sheering;
    private boolean breeding;
    private boolean slaughtering;


    public ShepherdAI(ShepherdEntity worker, int coolDown) {
        this.worker = worker;
        //this.coolDown = coolDown;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return worker.getIsWorking() && !worker.getFollow();
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
        if (worker.getOwner() != null){
            worker.getOwner().sendMessage(new StringTextComponent("sheering= " + sheering), worker.getOwner().getUUID());
            worker.getOwner().sendMessage(new StringTextComponent("breeding= " + breeding), worker.getOwner().getUUID());
            worker.getOwner().sendMessage(new StringTextComponent("slaughtering= " + slaughtering), worker.getOwner().getUUID());
        }


        if (sheering){
            findSheepSheering();
            if (this.sheep != null && sheep.readyForShearing()) {
                this.worker.getNavigation().moveTo(this.sheep, 1);

                if (sheep.closerThan(this.worker, 1.5)) {
                    sheerSheep(this.sheep);
                    this.worker.getLookControl().setLookAt(sheep.getX(), sheep.getEyeY(), sheep.getZ(), 10.0F, (float) this.worker.getMaxHeadXRot());
                    //timer = 0;
                    this.sheep = null;
                    this.findSheepSheering();
                }
            }
            else {
                sheering = false;
                breeding = true;
            }

        }

        if (breeding){
            findSheepBreeding();
            if (this.sheep != null ) {
                int i = sheep.getAge();

                if (i == 0 && this.hasWheat()) {
                    this.worker.getNavigation().moveTo(this.sheep, 1);

                    if (sheep.closerThan(this.worker, 1.5)) {
                        this.consumeWheat();
                        sheep.setInLove(null);
                        this.sheep = null;
                        this.worker.getLookControl().setLookAt(sheep.getX(), sheep.getEyeY(), sheep.getZ(), 10.0F, (float) this.worker.getMaxHeadXRot());
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
            List<SheepEntity> list = Objects.requireNonNull(worker.level.getEntitiesOfClass(SheepEntity.class, this.getSearchArea(12)));
            for (SheepEntity animals : list) {
                if (animals.isAlive() && !animals.isBaby()) {
                    this.sheep = list.get(0);
                }
            }
            if (list.size() > 8) {
                if (sheep != null) {
                    this.worker.getNavigation().moveTo(sheep.getX(), sheep.getY(), sheep.getZ(), 1);
                    if (sheep.blockPosition().closerThan(worker.position(), 1.5)) sheep.kill();
                    worker.workerSwingArm();
                }
            } else {
                slaughtering = false;
                sheering = true;
            }
        }
    }

    private void consumeWheat(){
        Inventory inventory = worker.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.WHEAT))
                itemStack.shrink(1);
        }
    }

    public void sheerSheep(SheepEntity sheepEntity) {
        sheepEntity.shear(SoundCategory.PLAYERS);
        sheepEntity.setSheared(true);
        if (!this.worker.swinging) {
            this.worker.workerSwingArm();
        }
    }

    public void findSheepSheering() {
        List<SheepEntity> list = Objects.requireNonNull(worker.level.getEntitiesOfClass(SheepEntity.class, this.getSearchArea(12)));
        SheepEntity closeSheep;

        for (SheepEntity sheeps : list) {
            for (int i = 0; i < list.size(); i++) {
                closeSheep = sheeps;

                if (closeSheep != null && closeSheep.readyForShearing()) {
                    this.sheep = closeSheep;
                }
            }
        }
    }

    protected void findSheepBreeding() {
        List<SheepEntity> list = Objects.requireNonNull(worker.level.getEntitiesOfClass(SheepEntity.class, this.getSearchArea(12)));
        SheepEntity closeanimal;
        for (SheepEntity animals : list){
            for (int i = 0; i < list.size(); i++) {
                closeanimal = animals;

                if (closeanimal != null && closeanimal.canFallInLove() && !closeanimal.isBaby()) {
                    this.sheep = closeanimal;
                }
            }
        }
    }

    protected void findSheepSlaughter() {
        List<SheepEntity> list = Objects.requireNonNull(worker.level.getEntitiesOfClass(SheepEntity.class, this.getSearchArea(12)));
        for (SheepEntity animals : list) {
            if (animals.isAlive() && !animals.isBaby()) {
                this.sheep = list.get(0);
            }
        }
    }


    protected AxisAlignedBB getSearchArea(double area) {
        return this.worker.getBoundingBox().inflate(area, 8.0D, area);
    }

    private boolean hasWheat() {
        Inventory inventory = worker.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.WHEAT))
                if (itemStack.getCount() >= 2)
                return true;
        }
        return false;
    }

}
