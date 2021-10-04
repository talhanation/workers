package com.talhanation.workers.entities.ai;


import java.util.EnumSet;
import java.util.Optional;

;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;

public class WorkerMoveToBlockPosGoal extends Goal {
    protected final AbstractWorkerEntity mob;
    public final double speedModifier;
    protected int nextStartTick;
    protected int tryTicks;
    private int maxStayTicks;
    protected Optional<BlockPos> blockPos;
    private boolean reachedTarget;
    private final int searchRange;
    private final int verticalSearchRange;
    protected int verticalSearchStart;

    public WorkerMoveToBlockPosGoal(AbstractWorkerEntity entity, double speed, int searchRange, int verticalSearchRange) {
        this.mob = entity;
        this.speedModifier = speed;
        this.searchRange = searchRange;
        this.verticalSearchStart = 0;
        this.verticalSearchRange = verticalSearchRange;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        this.blockPos = entity.getStartPos();
    }

    public boolean canUse() {
        if (this.nextStartTick > 0) {
            --this.nextStartTick;
            return false;
        } else {
            this.nextStartTick = this.nextStartTick(this.mob);
            return this.mob.getStartPos().isPresent();
        }
    }

    protected int nextStartTick(CreatureEntity p_203109_1_) {
        return 200 + p_203109_1_.getRandom().nextInt(200);
    }

    public boolean canContinueToUse() {
        return this.tryTicks >= -this.maxStayTicks && this.tryTicks <= 1200;
    }

    public void start() {
        this.moveMobToBlock();
        this.tryTicks = 0;
        this.maxStayTicks = this.mob.getRandom().nextInt(this.mob.getRandom().nextInt(1200) + 1200) + 1200;
    }

    protected void moveMobToBlock() {
        this.mob.getNavigation().moveTo(this.blockPos.get().getX(), this.blockPos.get().getY(), this.blockPos.get().getZ(), this.speedModifier);
    }

    public double acceptedDistance() {
        return 1.0D;
    }

    protected BlockPos getMoveToTarget() {
        return this.blockPos.get().above();
    }

    public void tick() {
        BlockPos blockpos = this.getMoveToTarget();
        if (!blockpos.closerThan(this.mob.position(), this.acceptedDistance())) {
            this.reachedTarget = false;
            ++this.tryTicks;
            if (this.shouldRecalculatePath()) {
                this.mob.getNavigation().moveTo((double)((float)blockpos.getX()) + 0.5D, (double)blockpos.getY(), (double)((float)blockpos.getZ()) + 0.5D, this.speedModifier);
            }
        } else {
            this.reachedTarget = true;
            --this.tryTicks;
        }

    }

    public boolean shouldRecalculatePath() {
        return this.tryTicks % 40 == 0;
    }

}