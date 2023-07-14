package com.talhanation.workers.entities.ai.navigation;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class WorkersPathNavigation extends GroundPathNavigation {

    public WorkersPathNavigation(AbstractWorkerEntity recruit, Level world) {
        super(recruit, world);
    }

    protected PathFinder createPathFinder(int range) {
        this.nodeEvaluator = new WalkNodeEvaluator();
        this.nodeEvaluator.setCanOpenDoors(true);
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanFloat(true);
        return new PathFinder(this.nodeEvaluator, range);
    }
}