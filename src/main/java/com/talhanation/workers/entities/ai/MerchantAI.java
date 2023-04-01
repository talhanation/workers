package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.MerchantEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class MerchantAI extends Goal {

    private final MerchantEntity merchant;
    private BlockPos workPos;

    public MerchantAI(MerchantEntity merchant) {
        this.merchant = merchant;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean canUse() {
        return this.merchant.canWork();
    }

    @Override
    public void start() {
        this.workPos = merchant.getStartPos();
    }

    @Override
    public void tick() {
        if (workPos != null) {
            //Move to minePos -> normal movement
            if (!workPos.closerThan(merchant.getOnPos(), 12)) {
                this.merchant.walkTowards(workPos, 1F);
            }
            //Near Mine Pos -> presice movement
            if (!workPos.closerThan(merchant.getOnPos(), 1)) {
                this.merchant.getMoveControl().setWantedPosition(workPos.getX(), workPos.getY(), workPos.getZ(), 1);
            } else
                merchant.getNavigation().stop();
        }
    }
}
