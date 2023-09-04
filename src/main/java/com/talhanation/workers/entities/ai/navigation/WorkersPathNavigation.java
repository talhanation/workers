package com.talhanation.workers.entities.ai.navigation;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.jetbrains.annotations.NotNull;

public class WorkersPathNavigation extends GroundPathNavigation {
    public WorkersPathNavigation(AbstractWorkerEntity worker, Level world) {
        super(worker, world);
        worker.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
        worker.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 16F);
        worker.setPathfindingMalus(BlockPathTypes.TRAPDOOR, 16.0F);
    }

    protected @NotNull PathFinder createPathFinder(int range) {
        this.nodeEvaluator = new WalkNodeEvaluator();
        this.nodeEvaluator.setCanOpenDoors(true);
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanFloat(true);

        return new PathFinder(this.nodeEvaluator, range);
    }
}