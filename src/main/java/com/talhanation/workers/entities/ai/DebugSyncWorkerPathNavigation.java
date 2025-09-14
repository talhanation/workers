package com.talhanation.workers.entities.ai;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.jetbrains.annotations.NotNull;

public class DebugSyncWorkerPathNavigation extends GroundPathNavigation { // ONLY FOR DEBUGGING OF NODE EVALUATOR NOT INTENDED TO USE IN MOD ITSELF
    private AbstractWorkerEntity worker;
    public DebugSyncWorkerPathNavigation(AbstractWorkerEntity worker, Level world) {
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
        this.nodeEvaluator = new RecruitsPathNodeEvaluator();
        this.nodeEvaluator.setCanOpenDoors(true);
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanFloat(true);
        return new PathFinder(this.nodeEvaluator, range);
    }

    public boolean moveTo(double x, double y, double z, double g) {
        this.worker.setMaxFallDistance(1);
        Main.LOGGER.info("Target: " + y);
        ((RecruitsPathNodeEvaluator)this.nodeEvaluator).setTarget((int) x, (int) y, (int) z);
        return this.moveTo(this.createPath(x, y, z, 1), g);
    }
}