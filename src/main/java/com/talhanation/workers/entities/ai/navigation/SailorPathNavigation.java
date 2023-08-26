package com.talhanation.workers.entities.ai.navigation;

import com.google.common.collect.ImmutableSet;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.IBoatController;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class SailorPathNavigation extends WaterBoundPathNavigation {

    AbstractWorkerEntity worker;

    public SailorPathNavigation(IBoatController sailor, Level level) {
        super(sailor.getWorker(), level);
        this.worker = sailor.getWorker();
    }

    protected @NotNull PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new SailorNodeEvaluator();
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    @Override
    protected boolean canUpdatePath() {
        return worker.getVehicle() instanceof Boat;
    }

    @Nullable
    public Path createPath(@NotNull BlockPos blockPos, int additionalOffsetXYZ, boolean targetBlockAbove, int accuracy) {
        return this.createPath(ImmutableSet.of(blockPos), additionalOffsetXYZ, targetBlockAbove, accuracy, (float)this.mob.getAttributeValue(Attributes.FOLLOW_RANGE));
    }
}
