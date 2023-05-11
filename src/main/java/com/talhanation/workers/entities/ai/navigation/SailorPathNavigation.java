package com.talhanation.workers.entities.ai.navigation;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.IBoatController;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Path;

import javax.annotation.Nullable;
import java.util.Set;

public class SailorPathNavigation extends WaterBoundPathNavigation {

    AbstractWorkerEntity worker;
    public SailorPathNavigation(IBoatController sailor, Level level) {
        super(sailor.getWorker(), level);
        this.worker = sailor.getWorker();
    }

    @Override
    protected boolean canUpdatePath() {
        return worker.getVehicle() instanceof Boat;
    }
    /*
    @Nullable
    protected Path createPath(Set<BlockPos> blockPosSet, int p_148224_, boolean p_148225_, int p_148226_, float p_148227_) {
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
            BlockPos blockpos = p_148225_ ? this.mob.blockPosition().above() : this.mob.blockPosition();
            int i = (int)(p_148227_ + (float)p_148224_);
            PathNavigationRegion pathnavigationregion = new PathNavigationRegion(this.level, blockpos.offset(-i, -i, -i), blockpos.offset(i, i, i));
            Path path = this.pathFinder.findPath(pathnavigationregion, this.mob, blockPosSet, p_148227_, p_148226_, this.maxVisitedNodesMultiplier);
            this.level.getProfiler().pop();
            if (path != null && path.getTarget() != null) {
                this.targetPos = path.getTarget();
                this.reachRange = p_148226_;
                this.resetStuckTimeout();
            }

            return path;
        }
    }

     */
}
