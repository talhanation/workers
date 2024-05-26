package com.talhanation.workers.entities.ai.navigation;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.jetbrains.annotations.NotNull;

public class WorkersPathNavigation extends GroundPathNavigation {

    private AbstractWorkerEntity worker;
    public WorkersPathNavigation(AbstractWorkerEntity worker, Level world) {
        super(worker, world);
        this.worker = worker;
        worker.setPathfindingMalus(BlockPathTypes.WATER, 32.0F);
        worker.setPathfindingMalus(BlockPathTypes.TRAPDOOR, 32.0F);
        worker.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 32.0F);
        worker.setPathfindingMalus(BlockPathTypes.DAMAGE_CAUTIOUS, 32.0F);
        worker.setPathfindingMalus(BlockPathTypes.DANGER_POWDER_SNOW, -1.0F);
        worker.setPathfindingMalus(BlockPathTypes.DOOR_WOOD_CLOSED, 0.0F);
        worker.setPathfindingMalus(BlockPathTypes.FENCE, 32.0F);
        worker.setPathfindingMalus(BlockPathTypes.LAVA, -1.0F);
    }

    protected @NotNull PathFinder createPathFinder(int range) {
        this.nodeEvaluator = new WorkersNodeEvaluator();
        this.nodeEvaluator.setCanOpenDoors(true);
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanFloat(true);
        return new PathFinder(this.nodeEvaluator, range);
    }

    public boolean moveTo(double p_26520_, double p_26521_, double p_26522_, double p_26523_) {
        this.worker.setMaxFallDistance(1);
        return this.moveTo(this.createPath(p_26520_, p_26521_, p_26522_, 1), p_26523_);
    }

}