package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.ShepherdEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;


public class SheerSheepGoal extends Goal {
    private SheepEntity sheep;
    private final AbstractWorkerEntity worker;
    private final int coolDown;
    private int timer = 0;

    public SheerSheepGoal(ShepherdEntity worker, int coolDown) {
        this.worker = worker;
        this.coolDown = coolDown;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public boolean canUse() {
        return worker.getIsWorking();
    }

    protected AxisAlignedBB getSearchArea(double area) {
        return this.worker.getBoundingBox().inflate(area, 8.0D, area);
    }

    @Override
    public void tick() {
        super.tick();
        timer++;

        if (this.sheep != null && sheep.readyForShearing()) {
            this.worker.getNavigation().moveTo(this.sheep, 1);

            if (sheep.closerThan(this.worker, 1.5)) {
                sheerSheep(this.sheep);
                this.worker.getLookControl().setLookAt(sheep.getX(), sheep.getEyeY(), sheep.getZ(), 10.0F, (float) this.worker.getMaxHeadXRot());

                if (!this.worker.swinging) {
                    this.worker.workerSwingArm();
                }
                timer = 0;
                this.sheep = null;
                this.findSheep();
            }
        }
        findSheep();
    }

    public void sheerSheep(SheepEntity sheepEntity) {
        sheepEntity.shear(SoundCategory.PLAYERS);
        sheepEntity.setSheared(true);
    }

    public void findSheep() {
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
}