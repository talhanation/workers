package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.entities.workarea.MarketArea;
import com.talhanation.workers.world.VillagerInviteRegistry;
import com.talhanation.workers.world.WorkersMerchantTrade;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.*;

public class MerchantWorkGoal extends Goal {

    private static final int VILLAGER_SCAN_INTERVAL = 100;
    private static final int VILLAGER_TIMEOUT = 400;

    private final MerchantEntity merchant;
    private State state;
    private int cooldown;
    private int villagerScanCooldown;

    public MerchantWorkGoal(MerchantEntity merchant) {
        this.merchant = merchant;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }
    @Override
    public boolean canUse() {
        if (merchant.isCreative()) return false;
        return merchant.shouldWork() && !merchant.needsToGetToChest() && this.isAreaNotRemoved();
    }

    private boolean isAreaNotRemoved() {
        if(merchant.currentMarketArea == null || !merchant.currentMarketArea.isRemoved()) return true;
        else {
            merchant.currentMarketArea = null;
        }
        return false;
    }


    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean isInterruptable(){
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick(){
        return true;
    }

    @Override
    public void start() {
        merchant.setFollowState(6); //Working
        setState(State.SELECT_WORK_AREA);
    }

    @Override
    public void stop() {
        if (merchant.currentMarketArea != null) {
            merchant.currentMarketArea.setBeingWorkedOn(false);
            this.merchant.setFollowState(0);//Wander
            merchant.currentMarketArea = null;
            merchant.setCurrentMarketName("");
        }
        merchant.clearVillagerTrade();
        merchant.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (merchant.getCommandSenderWorld().isClientSide()) return;
        if (state == null) return;

        if (state != State.SELECT_WORK_AREA && isCurrentAreaGone()) {
            leaveCurrentArea();
            setState(State.SELECT_WORK_AREA);
            return;
        }

        switch (state) {
            case SELECT_WORK_AREA -> {
                if (merchant.currentMarketArea != null) {
                    setState(State.WALK_TO_CENTER);
                    return;
                }

                if (++cooldown < merchant.getRandom().nextInt(200)) return;
                cooldown = 0;

                MarketArea found = findBestArea((ServerLevel) merchant.getCommandSenderWorld());
                if (found == null) return;

                merchant.currentMarketArea = found;
                found.setBeingWorkedOn(true);
                found.setTime(0);

                merchant.setCurrentMarketName(found.getMarketName());
                setState(State.WALK_TO_CENTER);
            }

            case WALK_TO_CENTER -> {
                if (moveToPosition(BlockPos.containing(merchant.currentMarketArea.getArea().getCenter()), 3)) return;
                merchant.getNavigation().stop();
                setState(State.WORKING);
            }

            case WORKING -> {
                if (!merchant.currentMarketArea.isOpen()) {
                    leaveCurrentArea();
                    setState(State.SELECT_WORK_AREA);
                    return;
                }

                merchant.getNavigation().stop();
                merchant.setFollowState(6);

                Player nearby = merchant.getCommandSenderWorld()
                        .getNearestPlayer(merchant, 8);
                if (nearby != null) {
                    merchant.getLookControl().setLookAt(nearby, 30, 30);
                }
                else {
                    merchant.setYRot(merchant.currentMarketArea.getFacing().getOpposite().toYRot());
                }

                merchant.setCurrentMarketName(merchant.currentMarketArea.getMarketName());

                tickVillagerTrading((ServerLevel) merchant.getCommandSenderWorld());
            }
        }
    }


    private void tickVillagerTrading(ServerLevel level) {
        if(merchant.activeTradingVillager != null) {
            Villager v = merchant.activeTradingVillager;

            if(v.isRemoved() || !isVillagerTradeStillValid()) {
                merchant.clearVillagerTrade();
                return;
            }

            if(++merchant.villagerTradeTimeout > VILLAGER_TIMEOUT) {
                merchant.clearVillagerTrade();
                return;
            }

            if(merchant.distanceTo(v) < 3.5) {
                executeVillagerTrade(v);
            }
            return;
        }

        if(++villagerScanCooldown < VILLAGER_SCAN_INTERVAL) return;
        villagerScanCooldown = 0;
        inviteVillager(level);
    }

    private void inviteVillager(ServerLevel level) {
        List<Villager> nearby = level.getEntitiesOfClass(
                Villager.class, merchant.getBoundingBox().inflate(48));
        if(nearby.isEmpty()) return;

        List<Candidate> candidates = new ArrayList<>();

        for(Villager v : nearby) {
            if(v.isRemoved() || v.isSleeping() || v.isTrading()) continue;
            if(VillagerInviteRegistry.isInvited(v.getUUID())) continue;

            for(WorkersMerchantTrade trade : merchant.getTrades()) {
                if(!trade.isVillagerTrade || !trade.enabled) continue;
                if(trade.maxTrades != -1 && trade.currentTrades >= trade.maxTrades) continue;
                if(trade.tradeItem.isEmpty()) continue;

                MerchantOffer offer = findMatchingOffer(v, trade.tradeItem.getItem());
                if(offer == null || offer.isOutOfStock()) continue;

                if(merchant.countMerchantItemStack(trade.tradeItem, false) < offer.getCostA().getCount()) continue;

                candidates.add(new Candidate(v, trade, offer));
            }
        }

        if(candidates.isEmpty()) return;

        Candidate chosen = candidates.get(merchant.getRandom().nextInt(candidates.size()));

        if(!VillagerInviteRegistry.tryInvite(chosen.villager().getUUID(), merchant.getUUID())) return;

        merchant.activeTradingVillager = chosen.villager();
        merchant.activeVillagerTrade   = chosen.trade();
        merchant.activeVillagerOffer   = chosen.offer();
        merchant.villagerTradeTimeout  = 0;
    }

    private void executeVillagerTrade(Villager villager) {
        MerchantOffer offer = merchant.activeVillagerOffer;
        WorkersMerchantTrade trade = merchant.activeVillagerTrade;

        if(offer == null || offer.isOutOfStock() || trade == null) {
            merchant.clearVillagerTrade();
            return;
        }

        ItemStack costItem   = offer.getCostA();
        ItemStack rewardItem = offer.getResult();

        boolean success = merchant.tryExecuteVillagerTrade(trade, costItem, rewardItem);
        if(!success) {
            merchant.clearVillagerTrade();
            return;
        }

        offer.increaseUses();
        villager.setVillagerXp(villager.getVillagerXp() + offer.getXp());
        if (villager.shouldIncreaseLevel()) {
            villager.updateMerchantTimer = 40;
            villager.increaseProfessionLevelOnUpdate = true;
            //villager. last player trade to owner?
        }

        merchant.clearVillagerTrade();
    }

    private boolean isVillagerTradeStillValid() {
        MerchantOffer offer = merchant.activeVillagerOffer;
        WorkersMerchantTrade trade = merchant.activeVillagerTrade;
        if(offer == null || trade == null) return false;
        if(offer.isOutOfStock()) return false;
        if(trade.maxTrades != -1 && trade.currentTrades >= trade.maxTrades) return false;
        return merchant.countMerchantItemStack(trade.tradeItem, false) >= offer.getCostA().getCount();
    }

    @Nullable
    private MerchantOffer findMatchingOffer(Villager villager, Item item) {
        MerchantOffers offers = villager.getOffers();
        if(offers == null) return null;
        for(MerchantOffer offer : offers) {
            if(offer.getResult().getItem() == Items.EMERALD
                    && offer.getCostA().getItem() == item) {
                return offer;
            }
        }
        return null;
    }

    private record Candidate(Villager villager, WorkersMerchantTrade trade, MerchantOffer offer) {}


    private void leaveCurrentArea() {
        if (merchant.currentMarketArea != null) {
            merchant.currentMarketArea.setBeingWorkedOn(false);
            merchant.currentMarketArea = null;
            merchant.setCurrentMarketName("");
        }
    }

    private boolean isCurrentAreaGone() {
        return merchant.currentMarketArea == null || merchant.currentMarketArea.isRemoved();
    }


    private boolean moveToPosition(BlockPos pos, int thresholdBlocks) {
        double dist = merchant.getHorizontalDistanceTo(pos.getCenter());
        if (dist < thresholdBlocks) {
            merchant.getNavigation().stop();
            return false;
        }
        merchant.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.8F);
        merchant.setFollowState(6);
        merchant.getLookControl().setLookAt(pos.getCenter());
        return true;
    }

    @Nullable
    private MarketArea findBestArea(ServerLevel level) {
        List<MarketArea> areas = level.getEntitiesOfClass(MarketArea.class, merchant.getBoundingBox().inflate(64));

        MarketArea best = null;
        int bestScore = -1;

        for (MarketArea area : areas) {
            if (area == null) continue;
            if (!area.canWorkHere(merchant)) continue;
            if (area.isBeingWorkedOn()) continue;
            int score = 0;
            score += area.getTime() * 10;

            if (score > bestScore) {
                bestScore = score;
                best = area;
            }
        }
        return best;
    }

    private void setState(State s) {
        this.state = s;
    }

    public enum State {
        SELECT_WORK_AREA,
        WALK_TO_CENTER,
        WORKING
    }
}
