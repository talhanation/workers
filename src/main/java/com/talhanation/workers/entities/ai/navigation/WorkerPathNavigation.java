package com.talhanation.workers.entities.ai.navigation;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.pathfinding.AsyncGroundPathNavigation;
import com.talhanation.recruits.pathfinding.NodeEvaluatorGenerator;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

/**
 * Navigation for workers.
 *
 * Workers are an addon to the recruits mod. They build on the recruits async
 * navigation INFRASTRUCTURE ({@link AsyncGroundPathNavigation}, which the
 * released recruits mod exposes), but use WORKER-OWN copies of the improved
 * pathfinder + node evaluator ({@link WorkersAsyncPathfinder},
 * {@link WorkersNodeEvaluator}) rather than the recruits ones, because the
 * released recruits mod ships the old algorithm.
 *
 * This is what gives workers (miners especially) the exact-arrival /
 * underground-capable pathing.
 */
public class WorkerPathNavigation extends WorkersGroundPathNavigation {

    private static BiFunction<Integer, NodeEvaluator, PathFinder> pathfinderSupplier =
            (range, nodeEvaluator) -> new PathFinder(nodeEvaluator, range);

    private final AbstractWorkerEntity worker;

    private static final NodeEvaluatorGenerator nodeEvaluatorGenerator = () -> {
        NodeEvaluator nodeEvaluator = new WorkersNodeEvaluator();
        nodeEvaluator.setCanOpenDoors(true);
        nodeEvaluator.setCanPassDoors(true);
        nodeEvaluator.setCanFloat(true);
        return nodeEvaluator;
    };

    public WorkerPathNavigation(AbstractWorkerEntity worker, Level world) {
        super(worker, world);
        this.worker = worker;
        if (RecruitsServerConfig.UseAsyncPathfinding.get()) {
            pathfinderSupplier = (range, nodeEvaluator) ->
                    new WorkersAsyncPathfinder(nodeEvaluator, range, nodeEvaluatorGenerator, this.level);
        }
    }

    @Override
    protected @NotNull PathFinder createPathFinder(int range) {
        this.nodeEvaluator = new WorkersNodeEvaluator();
        this.nodeEvaluator.setCanOpenDoors(true);
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanFloat(true);

        return pathfinderSupplier.apply(range, this.nodeEvaluator);
    }

    @Override
    public boolean moveTo(double x, double y, double z, double speed) {
        // Allow descent up to the evaluator's safe-fall limit so workers (miners
        // especially) can step down to underground targets instead of hugging
        // the surface, then delegate to the unified coordinate moveTo.
        this.worker.setMaxFallDistance(4);
        ((WorkersNodeEvaluator) this.nodeEvaluator).setTarget((int) x, (int) y, (int) z);
        return super.moveTo(x, y, z, speed);
    }
}
