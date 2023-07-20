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
import java.util.Set;

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
    /*
    @Nullable
    protected Path createPath(Set<BlockPos> blockPosSet, int additionalOffsetXYZ, boolean targetBlockAbove, int reachRange, float FOLLOW_RANGE) {
        if (blockPosSet.isEmpty()) {
            return null;
        } else if (this.mob.getY() < (double)this.level.getMinBuildHeight()) {
            return null;
        } else if (!this.canUpdatePath()) {
            return null;
        } else if (this.path != null && !this.path.isDone() && blockPosSet.contains(this.targetPos)) {
            return this.path;
        } else {
            this.level.getProfiler().push("pathfind");
            BlockPos blockpos = targetBlockAbove ? this.mob.blockPosition().above() : this.mob.blockPosition();
            int i = (int)(FOLLOW_RANGE + (float)additionalOffsetXYZ);
            PathNavigationRegion pathnavigationregion = new PathNavigationRegion(this.level, blockpos.offset(-i, -i, -i), blockpos.offset(i, i, i));
            Path path = this.pathFinder.findPath(pathnavigationregion, this.mob, blockPosSet, FOLLOW_RANGE, reachRange, this.maxVisitedNodesMultiplier);
            this.level.getProfiler().pop();
            if (path != null && path.getTarget() != null) {
                this.targetPos = path.getTarget();
                this.reachRange = reachRange;
                this.resetStuckTimeout();
            }

            return path;
        }
    }

     */
}
