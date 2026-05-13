package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.CookEntity;
import com.talhanation.workers.world.VillagerInviteRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

public class VillagerRespondToCookGoal extends Goal {

    private final Villager villager;
    @Nullable private CookEntity invitingCook;

    public VillagerRespondToCookGoal(Villager villager) {
        this.villager = villager;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (villager.level().isClientSide()) return false;
        if (villager.isSleeping() || villager.isTrading()) return false;
        invitingCook = findInvitingCook();
        return invitingCook != null;
    }

    @Override
    public boolean canContinueToUse() {
        return invitingCook != null
                && !invitingCook.isRemoved()
                && villager.equals(invitingCook.activeTradingVillager);
    }

    @Override
    public void stop() {
        invitingCook = null;
        villager.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (invitingCook == null) return;

        if (villager.distanceTo(invitingCook) > 2.5) {
            villager.getNavigation().moveTo(invitingCook, 0.6);
        }
        else {
            villager.getNavigation().stop();
            villager.getLookControl().setLookAt(invitingCook, 30, 30);
        }
    }

    @Nullable
    private CookEntity findInvitingCook() {
        UUID cookUUID = VillagerInviteRegistry.getInvitedBy(villager.getUUID());
        if (cookUUID == null) return null;

        Entity entity = ((ServerLevel) villager.level()).getEntity(cookUUID);
        if (entity instanceof CookEntity cook && !cook.isRemoved()) {
            return cook;
        }

        VillagerInviteRegistry.release(villager.getUUID());
        return null;
    }
}
