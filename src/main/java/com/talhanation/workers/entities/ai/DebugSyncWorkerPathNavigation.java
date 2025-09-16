package com.talhanation.workers.entities.ai;

import com.talhanation.recruits.entities.ai.RecruitFollowOwnerGoal;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

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
        return this.moveTo(this.createPath(x, y, z, 1), g);
    }

    public boolean moveTo(@Nullable Path path, double g) {
        this.worker.setMaxFallDistance(1);
        if(worker instanceof MinerEntity && nodeEvaluator instanceof RecruitsPathNodeEvaluator recruitsPathNodeEvaluator){
            int offset = worker.isWorking() && !(worker.needsToDeposit() || worker.needsToGetItems()) ? 3 : 0;
            recruitsPathNodeEvaluator.setFloorOffset(offset);
        }
        return super.moveTo(path, g);
    }
}