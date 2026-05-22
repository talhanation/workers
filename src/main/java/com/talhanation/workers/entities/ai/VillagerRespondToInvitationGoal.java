package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.world.VillagerInviteRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

public class VillagerRespondToInvitationGoal extends Goal {

    private final Villager villager;
    @Nullable private ICanInviteVillager invitingWorker;

    public VillagerRespondToInvitationGoal(Villager villager) {
        this.villager = villager;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if(villager.level().isClientSide()) return false;
        if(villager.isSleeping() || villager.isTrading()) return false;
        invitingWorker = findInvitingWorker();
        return invitingWorker != null;
    }

    @Override
    public boolean canContinueToUse() {
        return invitingWorker != null
                && !invitingWorker.getWorker().isRemoved()
                && villager.equals(invitingWorker.getActiveTradingVillager());
    }

    @Override
    public void stop() {
        invitingWorker = null;
        villager.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if(invitingWorker == null) return;

        if(villager.distanceTo(invitingWorker.getWorker()) > 2.5) {
            villager.getNavigation().moveTo(invitingWorker.getWorker(), 0.6);
        }
        else {
            villager.getNavigation().stop();
            villager.getLookControl().setLookAt(invitingWorker.getWorker(), 30, 30);
        }
    }

    @Nullable
    private ICanInviteVillager findInvitingWorker() {
        UUID workerUUID = VillagerInviteRegistry.getInvitedBy(villager.getUUID());
        if(workerUUID == null) return null;

        Entity entity = ((ServerLevel) villager.level()).getEntity(workerUUID);
        if(entity instanceof ICanInviteVillager inviter && !inviter.getWorker().isRemoved()) {
            return inviter;
        }

        VillagerInviteRegistry.release(villager.getUUID());
        return null;
    }
}
