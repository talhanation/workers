package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.world.VillagerInviteRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

public class VillagerRespondToMerchantGoal extends Goal {

    private final Villager villager;
    @Nullable private MerchantEntity invitingMerchant;

    public VillagerRespondToMerchantGoal(Villager villager) {
        this.villager = villager;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if(villager.level().isClientSide()) return false;
        if(villager.isSleeping() || villager.isTrading()) return false;
        invitingMerchant = findInvitingMerchant();
        return invitingMerchant != null;
    }

    @Override
    public boolean canContinueToUse() {
        return invitingMerchant != null
                && !invitingMerchant.isRemoved()
                && villager.equals(invitingMerchant.activeTradingVillager);
    }

    @Override
    public void stop() {
        invitingMerchant = null;
        villager.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if(invitingMerchant == null) return;

        if(villager.distanceTo(invitingMerchant) > 2.5) {
            villager.getNavigation().moveTo(invitingMerchant, 0.6);
        }
        else {
            villager.getNavigation().stop();
            villager.getLookControl().setLookAt(invitingMerchant, 30, 30);
        }
    }

    @Nullable
    private MerchantEntity findInvitingMerchant() {
        UUID merchantUUID = VillagerInviteRegistry.getInvitedBy(villager.getUUID());
        if(merchantUUID == null) return null;

        Entity entity = ((ServerLevel) villager.level()).getEntity(merchantUUID);
        if(entity instanceof MerchantEntity merchant && !merchant.isRemoved()) {
            return merchant;
        }

        VillagerInviteRegistry.release(villager.getUUID());
        return null;
    }
}
