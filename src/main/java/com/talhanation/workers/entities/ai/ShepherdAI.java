package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.ShepherdEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class ShepherdAI extends Goal {
    private SheepEntity sheep;
    private final ShepherdEntity shepherd;
    private boolean sheering;
    private boolean breeding;
    private boolean slaughtering;


    public ShepherdAI(ShepherdEntity worker, int coolDown) {
        this.shepherd = worker;
        //this.coolDown = coolDown;
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
        if (shepherd.getOwner() != null){
            //worker.getOwner().sendMessage(new StringTextComponent("sheering= " + sheering), worker.getOwner().getUUID());
            //worker.getOwner().sendMessage(new StringTextComponent("breeding= " + breeding), worker.getOwner().getUUID());
            //worker.getOwner().sendMessage(new StringTextComponent("slaughtering= " + slaughtering), worker.getOwner().getUUID());
        }


        if (sheering){
            findSheepSheering();
            if (this.sheep != null && sheep.readyForShearing()) {
                this.shepherd.getNavigation().moveTo(this.sheep, 1);

                if (sheep.closerThan(this.shepherd, 1.5)) {
                    sheerSheep(this.sheep);
                    this.shepherd.getLookControl().setLookAt(sheep.getX(), sheep.getEyeY(), sheep.getZ(), 10.0F, (float) this.shepherd.getMaxHeadXRot());
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
                    this.shepherd.getNavigation().moveTo(this.sheep, 1);

                    if (sheep.closerThan(this.shepherd, 1.5)) {
                        this.consumeWheat();
                        this.shepherd.getLookControl().setLookAt(sheep.getX(), sheep.getEyeY(), sheep.getZ(), 10.0F, (float) this.shepherd.getMaxHeadXRot());
                        sheep.setInLove(null);
                        this.sheep = null;
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
            List<SheepEntity> list = Objects.requireNonNull(shepherd.level.getEntitiesOfClass(SheepEntity.class, this.getSearchArea(12)));
            for (SheepEntity animals : list) {
                if (animals.isAlive() && !animals.isBaby()) {
                    this.sheep = list.get(0);
                }
            }
            if (list.size() > shepherd.getMaxSheepCount()) {
                if (sheep != null) {
                    this.shepherd.getNavigation().moveTo(sheep.getX(), sheep.getY(), sheep.getZ(), 1);
                    if (sheep.blockPosition().closerThan(shepherd.position(), 1.5)) sheep.kill();
                    shepherd.workerSwingArm();
                }
            } else {
                slaughtering = false;
                sheering = true;
            }
        }
    }

    private void consumeWheat(){
        Inventory inventory = shepherd.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.WHEAT))
                itemStack.shrink(1);
        }
    }

    public void sheerSheep(SheepEntity sheepEntity) {
        sheepEntity.shear(SoundCategory.PLAYERS);
        sheepEntity.setSheared(true);
        if (!this.shepherd.swinging) {
            this.shepherd.workerSwingArm();
        }
    }

    public void findSheepSheering() {
        List<SheepEntity> list = Objects.requireNonNull(shepherd.level.getEntitiesOfClass(SheepEntity.class, this.getSearchArea(12)));
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
        List<SheepEntity> list = Objects.requireNonNull(shepherd.level.getEntitiesOfClass(SheepEntity.class, this.getSearchArea(12)));
        SheepEntity closeanimal;
        for (SheepEntity animals : list) {
            for (int i = 0; i < list.size(); i++) {
                closeanimal = animals;
                if (closeanimal != null && closeanimal.canFallInLove() && !closeanimal.isBaby()) {
                    this.sheep = closeanimal;
                }
            }
        }
    }

    protected AxisAlignedBB getSearchArea(double area) {
        return this.shepherd.getBoundingBox().inflate(area, 8.0D, area);
    }

    private boolean hasWheat() {
        Inventory inventory = shepherd.getInventory();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack.getItem().equals(Items.WHEAT))
                if (itemStack.getCount() >= 2)
                return true;
        }
        return false;
    }

}
