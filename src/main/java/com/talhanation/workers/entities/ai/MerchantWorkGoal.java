package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.entities.workarea.MarketArea;
import com.talhanation.workers.world.VillagerInviteRegistry;
import com.talhanation.workers.world.WorkersMerchantTrade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import javax.annotation.Nullable;
import java.util.*;

public class MerchantWorkGoal extends Goal {

    private static final int VILLAGER_SCAN_INTERVAL = 200;
    private static final int VILLAGER_TIMEOUT       = 500;
    private static final int TRADE_COOLDOWN         = 50;
    private static final int TRADES_MIN             = 3;
    private static final int TRADES_MAX             = 8;    // exclusive upper bound for nextInt

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
            merchant.currentMarketArea.setMerchantName("None");

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
                found.setMerchantName(merchant.getName().getString());
                found.scanContainers();

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

                Player nearby = merchant.getCommandSenderWorld().getNearestPlayer(merchant, 8);
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

    // ── Villager-Trade session logic ──────────────────────────────────────────

    private void tickVillagerTrading(ServerLevel level) {
        if(merchant.activeTradingVillager != null) {
            Villager v = merchant.activeTradingVillager;

            // Villager disappeared or trade conditions no longer hold
            if(v.isRemoved() || !isVillagerTradeStillValid()) {
                merchant.clearVillagerTrade();
                return;
            }

            // Safety: villager never showed up
            if(++merchant.villagerTradeTimeout > VILLAGER_TIMEOUT) {
                merchant.clearVillagerTrade();
                return;
            }

            // Villager not close enough yet
            if(merchant.distanceTo(v) >= 3.5) return;

            // Tick down the per-trade cooldown
            if(merchant.villagerTradeCooldown > 0) {
                merchant.villagerTradeCooldown--;
                return;
            }

            // Execute one trade of the session
            executeVillagerTrade(v, level);
            return;
        }

        if(++villagerScanCooldown < VILLAGER_SCAN_INTERVAL) return;
        villagerScanCooldown = 0;
        inviteVillager(level);
    }

    private void inviteVillager(ServerLevel level) {
        if(!level.isDay()) return;

        List<Villager> nearby = level.getEntitiesOfClass(Villager.class, merchant.getBoundingBox().inflate(60));
        if(nearby.isEmpty()) return;

        List<Candidate> candidates = new ArrayList<>();

        for(Villager villager : nearby) {
            if(villager.isRemoved() || villager.isSleeping() || villager.isTrading()) continue;
            if(VillagerInviteRegistry.isInvited(villager.getUUID())) continue;

            for(WorkersMerchantTrade trade : merchant.getTrades()) {
                if(!trade.isVillagerTrade || !trade.enabled) continue;
                if(trade.maxTrades != -1 && trade.currentTrades >= trade.maxTrades) continue;
                if(trade.tradeItem.isEmpty()) continue;

                MerchantOffer offer = findMatchingOffer(villager, trade.tradeItem.getItem());
                if(offer == null || offer.isOutOfStock()) continue;

                if(merchant.countMerchantItemStack(trade.tradeItem, false) < offer.getCostA().getCount()) continue;

                candidates.add(new Candidate(villager, trade, offer));
            }
        }

        if(candidates.isEmpty()) return;

        Candidate chosen = candidates.get(merchant.getRandom().nextInt(candidates.size()));

        if(!VillagerInviteRegistry.tryInvite(chosen.villager().getUUID(), merchant.getUUID())) return;

        // Assign a random number of trades for this session (TRADES_MIN..TRADES_MAX-1)
        int sessionTrades = TRADES_MIN + merchant.getRandom().nextInt(TRADES_MAX - TRADES_MIN + 1);

        merchant.activeTradingVillager    = chosen.villager();
        merchant.activeVillagerTrade      = chosen.trade();
        merchant.activeVillagerOffer      = chosen.offer();
        merchant.villagerTradeTimeout     = 0;
        merchant.villagerTradesRemaining  = sessionTrades;
        merchant.villagerTradeCooldown    = 0; // first trade fires immediately on arrival
    }

    private void executeVillagerTrade(Villager villager, ServerLevel level) {
        MerchantOffer offer = merchant.activeVillagerOffer;
        WorkersMerchantTrade trade = merchant.activeVillagerTrade;

        if(offer == null || offer.isOutOfStock() || trade == null) {
            merchant.clearVillagerTrade();
            return;
        }

        // costA = what merchant gives (e.g. 10 clay), result = what merchant receives (e.g. 1 emerald)
        ItemStack costItem   = offer.getCostA();
        ItemStack rewardItem = offer.getResult();

        boolean success = merchant.tryExecuteVillagerTrade(trade, costItem, rewardItem);
        if(!success) {
            merchant.clearVillagerTrade();
            return;
        }

        // Vanilla XP + level-up mechanics
        offer.increaseUses();
        villager.setVillagerXp(villager.getVillagerXp() + offer.getXp());
        trade.currentTrades += 1;

        if(villager.shouldIncreaseLevel()) {
            villager.updateMerchantTimer = 40;
            villager.increaseProfessionLevelOnUpdate = true;
        }

        // Visual & audio feedback
        spawnTradeParticles(level, villager);
        level.playSound(null, merchant.blockPosition(), SoundEvents.VILLAGER_TRADE, SoundSource.NEUTRAL, 1.0F, 1.0F);

        // Decrement session counter and decide whether to continue
        merchant.villagerTradesRemaining--;

        if(merchant.villagerTradesRemaining <= 0 || !isVillagerTradeStillValid()) {
            merchant.clearVillagerTrade();
        }
        else {
            // Reset timeout so villager doesn't get kicked mid-session, and set inter-trade cooldown
            merchant.villagerTradeTimeout = 0;
            merchant.villagerTradeCooldown = TRADE_COOLDOWN;
        }
    }

    private void spawnTradeParticles(ServerLevel level, Villager villager) {
        double vx = villager.getX();
        double vy = villager.getY() + 1.8;
        double vz = villager.getZ();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, vx, vy, vz, 6, 0.3, 0.3, 0.3, 0.0);

        double mx = merchant.getX();
        double my = merchant.getY() + 1.8;
        double mz = merchant.getZ();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mx, my, mz, 6, 0.3, 0.3, 0.3, 0.0);
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