package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorkerTakeCoverGoal extends Goal {

    private static final long   BELL_COOLDOWN_TICKS  = 1200L; // 1 minute
    private static final int    COVER_DURATION       = 1200;  // 1 minute wait
    private static final double BELL_DETECTION_RANGE = 48.0;
    private static final double RAID_DETECTION_RANGE = 64.0;

    // UUID → gameTime when bell triggered this worker
    private static final Map<UUID, Long> BELL_TRIGGERS = new HashMap<>();

    private final AbstractWorkerEntity worker;
    private int   ticksRemaining;
    private int   previousFollowState;

    public WorkerTakeCoverGoal(AbstractWorkerEntity worker) {
        this.worker = worker;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    // ── Static trigger (called from MixinBellBlockEntity) ────────────────────

    public static void onBellRing(Level level, BlockPos bellPos) {
        if (level.isClientSide()) return;

        level.getEntitiesOfClass(AbstractWorkerEntity.class,
                        new AABB(bellPos).inflate(BELL_DETECTION_RANGE))
                .forEach(w -> BELL_TRIGGERS.put(w.getUUID(), level.getGameTime()));
    }

    // ── Goal logic ────────────────────────────────────────────────────────────

    @Override
    public boolean canUse() {
        if (!worker.isAlive()) return false;
        // Wander freely = owner already released the worker, don't override
        if (worker.getFollowState() == 0) return false;
        // Already in hold position state
        if (worker.getFollowState() == 3) return false;
        return isBellCooldownActive() || isRaidNearby();
    }

    @Override
    public boolean canContinueToUse() {
        // Owner pressed wander freely → release immediately
        if (worker.getFollowState() == 0) return false;
        return ticksRemaining > 0;
    }

    @Override
    public void start() {
        previousFollowState = worker.getFollowState();
        ticksRemaining      = COVER_DURATION;

        worker.setHoldPos(worker.blockPosition().getCenter());
        worker.setFollowState(3);
    }

    @Override
    public void stop() {
        // Restore previous state only if owner didn't manually intervene
        if (worker.getFollowState() == 3) {
            worker.setFollowState(previousFollowState);
        }
        ticksRemaining = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticksRemaining--;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isBellCooldownActive() {
        Long triggerTime = BELL_TRIGGERS.get(worker.getUUID());
        if (triggerTime == null) return false;

        long elapsed = worker.level().getGameTime() - triggerTime;
        if (elapsed > BELL_COOLDOWN_TICKS) {
            BELL_TRIGGERS.remove(worker.getUUID());
            return false;
        }
        return true;
    }

    private boolean isRaidNearby() {
        if (!(worker.level() instanceof ServerLevel serverLevel)) return false;

        Raid raid = serverLevel.getRaidAt(worker.blockPosition());
        if (raid == null || !raid.isActive() || raid.isOver()) return false;

        return worker.blockPosition().distSqr(raid.getCenter())
                <= RAID_DETECTION_RANGE * RAID_DETECTION_RANGE;
    }
}
