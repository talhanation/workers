package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.entities.workarea.MarketArea;
import com.talhanation.workers.entities.workarea.StorageArea;
import com.talhanation.workers.entities.workarea.WorkshopArea;
import com.talhanation.workers.world.VillagerInviteRegistry;
import com.talhanation.workers.world.VillagerPriceTable;
import com.talhanation.workers.world.WorkersMerchantTrade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.*;

public class MerchantWorkGoal extends Goal {

    private static final int VILLAGER_SCAN_INTERVAL = 200;
    private static final int VILLAGER_TIMEOUT       = 500;
    private static final int TRADE_COOLDOWN         = 50;
    private static final int TRADES_MIN             = 3;
    private static final int TRADES_MAX             = 8;    // exclusive upper bound for nextInt
    private static final int STORAGE_CHECK_INTERVAL = 600;  // how often the market looks into the storages
    private static final int INTERACT_TIME          = 20;   // ~1 second spent at each chest
    private static final int MAX_FETCH_STACKS       = 4;
    private static final int MAX_MOVE_TICKS         = 600;

    private final MerchantEntity merchant;
    private State state;
    private int cooldown;
    private int villagerScanCooldown;

    // ── Storage: refill the market / bring bought items away ──────────────────
    private int storageCooldown;
    @Nullable private BlockPos openContainerPos;
    private int interactTimer;
    @Nullable private BlockPos moveTarget;
    private int moveTicks;
    private final List<BlockPos> fetchQueue = new ArrayList<>();
    private final Map<BlockPos, Container> fetchContainers = new HashMap<>();
    private final List<Wanted> fetchWanted = new ArrayList<>();
    /** Items the merchant brought from a storage: they go into the market containers. */
    private final Set<Item> fetchedItems = new HashSet<>();
    /** Non-currency costs of villager buy offers the market could not pay, fetched on the next storage check. */
    private final List<Wanted> helperWanted = new ArrayList<>();
    private final List<StorageArea> deliverStorages = new ArrayList<>();
    @Nullable private StorageArea deliverStorage;
    @Nullable private BlockPos deliverPos;
    /** Bought items the merchant carries to a storage. */
    private final Set<Item> deliverItems = new HashSet<>();

    public MerchantWorkGoal(MerchantEntity merchant) {
        this.merchant = merchant;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (merchant.isCreative()) return false;
        // a trading trip is run by the MerchantTripGoal
        if (merchant.getTrip() != null) return false;
        return !merchant.needsToSleep() && merchant.shouldWork() && !merchant.needsToGetToChest() && this.isAreaNotRemoved();
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
        if(merchant.getFollowState() == 0) merchant.setFollowState(6); //Working
        setState(State.SELECT_WORK_AREA);
    }

    @Override
    public void stop() {
        if (openContainerPos != null && merchant.getCommandSenderWorld() instanceof ServerLevel sl) {
            closeOpenContainer(sl);
        }
        if (merchant.currentMarketArea != null) {
            merchant.currentMarketArea.setBeingWorkedOn(false);
            if (this.merchant.getFollowState() == 6) this.merchant.setFollowState(0);//Wander
            merchant.currentMarketArea.setMerchantName("None");

            merchant.currentMarketArea = null;
            merchant.setCurrentMarketName("");
        }
        merchant.clearVillagerTrade();
        merchant.getNavigation().stop();
        merchant.idleAtMarket = false;
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

        // aiStep frees the market while the merchant is far away (at a storage), take it back when he returns
        MarketArea current = merchant.currentMarketArea;
        if (current != null && state != State.SELECT_WORK_AREA && !current.isBeingWorkedOn()
                && merchant.getHorizontalDistanceTo(current.position()) < 1000) {
            current.setBeingWorkedOn(true);
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
                found.assignMerchant(merchant.getUUID(), merchant.getName().getString());
                merchant.marketAreaUUID = found.getUUID();
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
                // start() already claimed the working state; calling it here every
                // tick would override owner commands and pull the merchant back to work.

                Player nearby = merchant.getCommandSenderWorld().getNearestPlayer(merchant, 8);
                if (nearby != null) {
                    merchant.getLookControl().setLookAt(nearby, 30, 30);
                }
                else {
                    merchant.setYRot(merchant.currentMarketArea.getFacing().getOpposite().toYRot());
                }

                merchant.setCurrentMarketName(merchant.currentMarketArea.getMarketName());

                tickVillagerTrading((ServerLevel) merchant.getCommandSenderWorld());
                tickStorage((ServerLevel) merchant.getCommandSenderWorld());
                merchant.idleAtMarket = state == State.WORKING && merchant.activeTradingVillager == null;
            }

            case FETCH         -> tickFetch((ServerLevel) merchant.getCommandSenderWorld());
            case STORE_FETCHED -> tickStoreFetched((ServerLevel) merchant.getCommandSenderWorld());
            case DELIVER       -> tickDeliver((ServerLevel) merchant.getCommandSenderWorld());
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

        // a visiting merchant trades with him, no new villager meanwhile
        if(merchant.hasVisitor()) return;
        if(++villagerScanCooldown < VILLAGER_SCAN_INTERVAL) return;
        villagerScanCooldown = 0;
        inviteVillager(level);
    }

    private void inviteVillager(ServerLevel level) {
        if(!level.isDay()) return;

        List<Villager> nearby = level.getEntitiesOfClass(Villager.class, merchant.getBoundingBox().inflate(60));
        if(nearby.isEmpty()) return;

        List<WorkersMerchantTrade> trades = merchant.getTrades();
        // How many items a villager trade may still buy (until the target amount) or sell (down to the amount it keeps)
        Map<UUID, Integer> limits = new HashMap<>();
        List<StorageArea> storages = null;
        for(WorkersMerchantTrade trade : trades) {
            if(!trade.isVillagerTrade || !trade.enabled || trade.tradeItem.isEmpty()) continue;
            if(storages == null) storages = findStorages(level, merchant.currentMarketArea);
            limits.put(trade.uuid, trade.isVillagerBuy() ? getBuyLimit(trade, storages) : getSellLimit(trade, storages));
        }

        List<Candidate> candidates = new ArrayList<>();

        for(Villager villager : nearby) {
            if(villager.isRemoved() || villager.isSleeping() || villager.isTrading()) continue;
            if(VillagerInviteRegistry.isInvited(villager.getUUID())) continue;

            for(WorkersMerchantTrade trade : trades) {
                if(!trade.isVillagerTrade || !trade.enabled) continue;
                if(trade.maxTrades != -1 && trade.currentTrades >= trade.maxTrades) continue;
                if(trade.tradeItem.isEmpty()) continue;

                int limit = limits.getOrDefault(trade.uuid, 0);
                if(trade.isVillagerBuy()) {
                    if(limit <= 0) continue;

                    MerchantOffer offer = findBuyOffer(villager, trade);
                    if(offer == null) continue;

                    if(!merchant.canPayVillagerOffer(offer)) {
                        rememberHelperItems(trade, offer, trades);
                        continue;
                    }
                    if(tradesAboveReserve(offer) <= 0) continue;
                    candidates.add(new Candidate(villager, trade, offer));
                    continue;
                }

                MerchantOffer offer = findMatchingOffer(villager, trade.tradeItem.getItem());
                if(offer == null || offer.isOutOfStock() || !isNormalPrice(offer)) continue;

                int perTrade = offer.getCostA().getCount();
                if(limit < perTrade || merchant.countMerchantItemStack(trade.tradeItem, false) < perTrade) continue;

                candidates.add(new Candidate(villager, trade, offer));
            }
        }

        if(candidates.isEmpty()) return;

        Candidate chosen = candidates.get(merchant.getRandom().nextInt(candidates.size()));

        if(!VillagerInviteRegistry.tryInvite(chosen.villager().getUUID(), merchant.getUUID())) return;

        // Assign a random number of trades for this session (TRADES_MIN..TRADES_MAX-1)
        int sessionTrades = TRADES_MIN + merchant.getRandom().nextInt(TRADES_MAX - TRADES_MIN + 1);

        // Buying stops at the target amount and at the currency reserve, selling at the amount that is kept
        int limit = limits.getOrDefault(chosen.trade().uuid, 0);
        if(chosen.trade().isVillagerBuy()) {
            int resultCount = Math.max(1, chosen.offer().getResult().getCount());
            sessionTrades = Math.min(sessionTrades, (limit + resultCount - 1) / resultCount);
            sessionTrades = Math.max(1, Math.min(sessionTrades, tradesAboveReserve(chosen.offer())));
        }
        else {
            int perTrade = Math.max(1, chosen.offer().getCostA().getCount());
            sessionTrades = Math.max(1, Math.min(sessionTrades, limit / perTrade));
        }

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

        boolean success;
        if(trade.isVillagerBuy()) {
            // costA/costB = what merchant pays (e.g. 1 emerald), result = what merchant receives (e.g. 6 bread)
            success = merchant.tryExecuteVillagerBuy(trade, offer);
        }
        else {
            // costA = what merchant gives (e.g. 10 clay), result = what merchant receives (e.g. 1 emerald)
            ItemStack costItem   = offer.getCostA();
            ItemStack rewardItem = offer.getResult();
            success = merchant.tryExecuteVillagerTrade(trade, costItem, rewardItem);
        }

        if(!success) {
            merchant.clearVillagerTrade();
            return;
        }

        // Vanilla XP + level-up mechanics (the trade itself was counted by the merchant)
        offer.increaseUses();
        villager.setVillagerXp(villager.getVillagerXp() + offer.getXp());

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
        if(trade.isVillagerBuy()) {
            return isNormalPrice(offer) && merchant.canPayVillagerOffer(offer) && tradesAboveReserve(offer) > 0;
        }
        return isNormalPrice(offer) && merchant.countMerchantItemStack(trade.tradeItem, false) >= offer.getCostA().getCount();
    }

    @Nullable
    private MerchantOffer findMatchingOffer(Villager villager, Item item) {
        MerchantOffers offers = villager.getOffers();
        if(offers == null) return null;
        for(MerchantOffer offer : offers) {
            if(offer.getResult().is(VillagerPriceTable.getCurrency())
                    && offer.getCostA().getItem() == item) {
                return offer;
            }
        }
        return null;
    }

    /** An offer of the villager that sells the trade item at its normal price (no demand surcharge). */
    @Nullable
    private MerchantOffer findBuyOffer(Villager villager, WorkersMerchantTrade trade) {
        MerchantOffers offers = villager.getOffers();
        if(offers == null) return null;
        for(MerchantOffer offer : offers) {
            ItemStack result = offer.getResult();
            if(result.isEmpty() || result.is(VillagerPriceTable.getCurrency())) continue;
            if(!matchesBuyItem(result, trade)) continue;
            if(offer.isOutOfStock() || !isNormalPrice(offer)) continue;
            return offer;
        }
        return null;
    }

    /** Never trade when the villager asks more than usual (demand or bad reputation): more emeralds when buying, more items when selling. */
    private boolean isNormalPrice(MerchantOffer offer) {
        return offer.getCostA().getCount() <= offer.getBaseCostA().getCount();
    }

    /** The item the player set: with NBT (e.g. a specific enchanted book) it must match exactly. */
    private boolean matchesBuyItem(ItemStack stack, WorkersMerchantTrade trade) {
        if(stack.isEmpty()) return false;
        if(trade.tradeItem.hasTag()) return ItemStack.isSameItemSameTags(stack, trade.tradeItem);
        return stack.is(trade.tradeItem.getItem());
    }

    /** How many items a villager buy trade may still buy until market and storages hold the target amount. */
    private int getBuyLimit(WorkersMerchantTrade trade, List<StorageArea> storages) {
        return trade.storageAmount - countStock(trade, storages);
    }

    /** How many items a villager sell trade may still sell, market and storages keep the set amount. */
    private int getSellLimit(WorkersMerchantTrade trade, List<StorageArea> storages) {
        return countStock(trade, storages) - trade.storageAmount;
    }

    private int countStock(WorkersMerchantTrade trade, List<StorageArea> storages) {
        MarketArea area = merchant.currentMarketArea;
        int stock = 0;
        if(area != null) stock += countMatching(area.containerMap.values(), stack -> matchesBuyItem(stack, trade));
        for(StorageArea storage : storages) {
            stock += countMatching(storage.storageMap.values(), stack -> matchesBuyItem(stack, trade));
        }
        return stock;
    }

    /** How many trades of the offer the market can pay without going below the currency reserve of the market. */
    private int tradesAboveReserve(MerchantOffer offer) {
        Item currency = VillagerPriceTable.getCurrency();
        int cost = 0;
        if(offer.getCostA().is(currency)) cost += offer.getCostA().getCount();
        if(offer.getCostB().is(currency)) cost += offer.getCostB().getCount();
        if(cost <= 0) return Integer.MAX_VALUE;

        MarketArea area = merchant.currentMarketArea;
        int reserve = area != null ? area.getCurrencyReserve() : 0;
        int have = merchant.countMerchantItemStack(VillagerPriceTable.getCurrencyStack(), false);
        return Math.max(0, (have - reserve) / cost);
    }

    /** Villager trades always use the storages of the market, player trades only with their checkbox. */
    private static boolean usesStorage(WorkersMerchantTrade trade) {
        return trade.isVillagerTrade || trade.useStorage;
    }

    /** Remembers the helper items (e.g. books, raw fish) of an offer the market can not pay. Emeralds and currency never. */
    private void rememberHelperItems(WorkersMerchantTrade trade, MerchantOffer offer, List<WorkersMerchantTrade> trades) {
        MarketArea area = merchant.currentMarketArea;
        if(!usesStorage(trade) || area == null || !area.getUseStorage()) return;

        boolean added = false;
        for(ItemStack cost : List.of(offer.getCostA(), offer.getCostB())) {
            if(cost.isEmpty() || isCurrency(cost, trades)) continue;
            addWanted(helperWanted, cost, false, cost.getCount() * TRADES_MAX, 0);
            added = true;
        }
        // look into the storages right away
        if(added) storageCooldown = STORAGE_CHECK_INTERVAL;
    }

    private record Candidate(Villager villager, WorkersMerchantTrade trade, MerchantOffer offer) {}

    // ── Storage ───────────────────────────────────────────────────────────────

    private void tickStorage(ServerLevel level) {
        MarketArea area = merchant.currentMarketArea;
        if(area == null) return;
        // never leave a villager or a visiting merchant standing
        if(merchant.activeTradingVillager != null || merchant.hasVisitor()) return;

        // Items that are still carried are handled first
        if(carriesFetchedItems()) {
            interactTimer = 0;
            setState(State.STORE_FETCHED);
            return;
        }
        if(carriesDeliverItems()) {
            interactTimer = 0;
            setState(State.DELIVER);
            return;
        }

        if(!area.getUseStorage()) {
            helperWanted.clear();
            return;
        }
        if(++storageCooldown < STORAGE_CHECK_INTERVAL) return;
        storageCooldown = 0;

        area.scanContainers();
        List<StorageArea> storages = findStorages(level, area);
        if(storages.isEmpty()) {
            helperWanted.clear();
            return;
        }

        List<WorkersMerchantTrade> trades = merchant.getTrades();
        if(tryStartFetch(area, storages, trades)) return;
        tryStartDeliver(area, storages, trades);
    }

    /**
     * Checks without walking whether trade items run low in the market (or a buy offer misses a helper item)
     * and the storages hold more than the amount that has to stay there. If so, the merchant goes and takes it.
     */
    private boolean tryStartFetch(MarketArea area, List<StorageArea> storages, List<WorkersMerchantTrade> trades) {
        List<Wanted> wanted = new ArrayList<>();

        // Goods for sell trades (players and villagers): refill when the market holds less than half a stack
        for(WorkersMerchantTrade trade : trades) {
            if(!trade.enabled || !usesStorage(trade) || trade.isVillagerBuy() || trade.tradeItem.isEmpty()) continue;
            if(trade.maxTrades != -1 && trade.currentTrades >= trade.maxTrades) continue;
            if(isCurrency(trade.tradeItem, trades)) continue;

            boolean allowDamaged = !trade.isVillagerTrade && trade.allowDamagedCurrency;
            int perTrade = trade.isVillagerTrade ? 1 : Math.max(1, trade.tradeItem.getCount());
            int target = trade.tradeItem.getMaxStackSize();
            if(!trade.isVillagerTrade && trade.maxTrades != -1) {
                target = Math.min(target, (trade.maxTrades - trade.currentTrades) * perTrade);
            }

            int inMarket = area.countItemInContainers(trade.tradeItem, allowDamaged);
            if(inMarket >= Math.max(perTrade, target / 2)) continue;
            addWanted(wanted, trade.tradeItem, allowDamaged, target - inMarket, trade.storageAmount);
        }

        // Helper items for villager buy offers
        for(Wanted helper : helperWanted) {
            int inMarket = area.countItemInContainers(helper.template, false);
            if(inMarket >= helper.amount) continue;
            addWanted(wanted, helper.template, false, helper.amount - inMarket, 0);
        }
        helperWanted.clear();

        // Only what is above the amount that stays in the storages, and only what fits into the market
        wanted.removeIf(w -> !area.canAddItem(w.template));
        for(Wanted w : wanted) {
            int inStorages = 0;
            for(StorageArea storage : storages) {
                inStorages += countMatching(storage.storageMap.values(), w::matches);
            }
            w.amount = Math.min(w.amount, inStorages - w.reserve);
        }
        wanted.removeIf(w -> w.amount <= 0);
        if(wanted.isEmpty()) return false;
        if(countFreeInventorySlots() <= 0) return false;

        // Chests that hold the wanted items, nearest storage first
        Map<Wanted, Integer> left = new HashMap<>();
        for(Wanted w : wanted) left.put(w, w.amount);
        List<BlockPos> queue = new ArrayList<>();
        Map<BlockPos, Container> containers = new HashMap<>();

        for(StorageArea storage : storages) {
            for(Map.Entry<BlockPos, Container> entry : storage.storageMap.entrySet()) {
                boolean useful = false;
                Container container = entry.getValue();
                for(int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if(stack.isEmpty()) continue;
                    for(Wanted w : wanted) {
                        int rest = left.get(w);
                        if(rest <= 0 || !w.matches(stack)) continue;
                        left.put(w, rest - stack.getCount());
                        useful = true;
                        break;
                    }
                }
                if(useful) {
                    queue.add(entry.getKey());
                    containers.put(entry.getKey(), container);
                }
            }
        }
        if(queue.isEmpty()) return false;

        queue.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(merchant.position())));
        fetchWanted.clear();
        fetchWanted.addAll(wanted);
        fetchQueue.clear();
        fetchQueue.addAll(queue);
        fetchContainers.clear();
        fetchContainers.putAll(containers);
        interactTimer = 0;
        setState(State.FETCH);
        return true;
    }

    private void tickFetch(ServerLevel level) {
        boolean done = fetchWanted.stream().allMatch(w -> w.amount <= 0);
        if(fetchQueue.isEmpty() || done || countFreeInventorySlots() <= 0) {
            closeOpenContainer(level);
            fetchQueue.clear();
            fetchContainers.clear();
            fetchWanted.clear();
            interactTimer = 0;
            setState(State.STORE_FETCHED);
            return;
        }

        BlockPos pos = fetchQueue.get(0);
        if(moveToPositionTimed(pos, 3)) return;
        merchant.getNavigation().stop();
        merchant.getLookControl().setLookAt(pos.getCenter());

        if(interactTimer == 0) {
            openContainerPos = pos;
            interactContainer(level, pos, true);
        }
        if(interactTimer < INTERACT_TIME) {
            interactTimer++;
            if(interactTimer % 5 == 0) merchant.swing(InteractionHand.MAIN_HAND);
            return;
        }

        Container container = fetchContainers.get(pos);
        if(container != null) {
            boolean changed = false;
            for(int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if(stack.isEmpty()) continue;

                Wanted wanted = null;
                for(Wanted w : fetchWanted) {
                    if(w.amount > 0 && w.matches(stack)) {
                        wanted = w;
                        break;
                    }
                }
                if(wanted == null) continue;

                ItemStack moving = stack.copy();
                moving.setCount(Math.min(stack.getCount(), wanted.amount));
                if(!merchant.canAddItem(moving)) break;
                int before = moving.getCount();
                ItemStack rest = merchant.addItem(moving);
                int taken = before - rest.getCount();
                if(taken <= 0) continue;

                fetchedItems.add(stack.getItem());
                stack.shrink(taken);
                if(stack.isEmpty()) container.setItem(i, ItemStack.EMPTY);
                wanted.amount -= taken;
                changed = true;
            }
            if(changed) container.setChanged();
        }

        closeOpenContainer(level);
        fetchQueue.remove(0);
        interactTimer = 0;
    }

    /** Puts the items from the storage into the market containers, then back to work. */
    private void tickStoreFetched(ServerLevel level) {
        MarketArea area = merchant.currentMarketArea;
        if(area == null) return;

        BlockPos target = findNearestContainer(area, merchant.blockPosition());
        if(target == null || !carriesFetchedItems()) {
            fetchedItems.clear();
            setState(State.WALK_TO_CENTER);
            return;
        }

        if(moveToPositionTimed(target, 3)) return;
        merchant.getNavigation().stop();
        merchant.getLookControl().setLookAt(target.getCenter());

        if(interactTimer == 0) {
            openContainerPos = target;
            interactContainer(level, target, true);
        }
        if(interactTimer < INTERACT_TIME) {
            interactTimer++;
            if(interactTimer % 5 == 0) merchant.swing(InteractionHand.MAIN_HAND);
            return;
        }

        SimpleContainer inventory = merchant.getInventory();
        for(int i = 6; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if(stack.isEmpty() || !fetchedItems.contains(stack.getItem())) continue;
            int rest = area.depositItemToContainers(stack, stack.getCount());
            if(rest <= 0) inventory.setItem(i, ItemStack.EMPTY);
            else stack.setCount(rest);
        }
        inventory.setChanged();
        area.scanContainers();

        closeOpenContainer(level);
        // whatever did not fit stays with the merchant, no endless retry
        fetchedItems.clear();
        interactTimer = 0;
        setState(State.WALK_TO_CENTER);
    }

    // ── Bringing bought items to a storage ────────────────────────────────────

    /**
     * Bought items of buy trades with the storage checkbox go to the nearest storage.
     * Currency and goods that another trade sells stay in the market.
     */
    private boolean tryStartDeliver(MarketArea area, List<StorageArea> storages, List<WorkersMerchantTrade> trades) {
        List<WorkersMerchantTrade> deliverTrades = new ArrayList<>();
        for(WorkersMerchantTrade trade : trades) {
            if(!trade.enabled || !usesStorage(trade) || !trade.isVillagerBuy() || trade.tradeItem.isEmpty()) continue;
            if(isCurrency(trade.tradeItem, trades) || isSellGood(trade.tradeItem, trades)) continue;
            deliverTrades.add(trade);
        }
        if(deliverTrades.isEmpty()) return false;

        int room = Math.min(MAX_FETCH_STACKS, countFreeInventorySlots());
        int taken = 0;
        for(Container container : area.containerMap.values()) {
            boolean changed = false;
            for(int i = 0; i < container.getContainerSize() && taken < room; i++) {
                ItemStack stack = container.getItem(i);
                if(stack.isEmpty() || !merchant.canAddItem(stack)) continue;
                boolean bought = false;
                for(WorkersMerchantTrade trade : deliverTrades) {
                    if(matchesBuyItem(stack, trade)) {
                        bought = true;
                        break;
                    }
                }
                if(!bought) continue;

                Item item = stack.getItem();
                ItemStack rest = merchant.addItem(stack.copy());
                if(rest.getCount() == stack.getCount()) continue;
                container.setItem(i, rest);
                deliverItems.add(item);
                changed = true;
                taken++;
            }
            if(changed) container.setChanged();
        }
        if(taken == 0) return false;

        area.scanContainers();
        merchant.swing(InteractionHand.MAIN_HAND);
        deliverStorages.clear();
        deliverStorages.addAll(storages);
        deliverPos = null;
        interactTimer = 0;
        setState(State.DELIVER);
        return true;
    }

    private void tickDeliver(ServerLevel level) {
        if(!carriesDeliverItems()) {
            closeOpenContainer(level);
            deliverItems.clear();
            deliverPos = null;
            deliverStorage = null;
            setState(State.WALK_TO_CENTER);
            return;
        }

        if(deliverPos == null) {
            if(deliverStorages.isEmpty() && merchant.currentMarketArea != null) {
                deliverStorages.addAll(findStorages(level, merchant.currentMarketArea));
            }
            if(!findDeliverTarget()) {
                // no storage has space: the items go back into the market
                fetchedItems.addAll(deliverItems);
                deliverItems.clear();
                deliverStorages.clear();
                interactTimer = 0;
                setState(State.STORE_FETCHED);
                return;
            }
            interactTimer = 0;
        }

        if(moveToPositionTimed(deliverPos, 3)) return;
        merchant.getNavigation().stop();
        merchant.getLookControl().setLookAt(deliverPos.getCenter());

        if(interactTimer == 0) {
            openContainerPos = deliverPos;
            interactContainer(level, deliverPos, true);
        }
        if(interactTimer < INTERACT_TIME) {
            interactTimer++;
            if(interactTimer % 5 == 0) merchant.swing(InteractionHand.MAIN_HAND);
            return;
        }

        Container container = deliverStorage != null ? deliverStorage.storageMap.get(deliverPos) : null;
        if(container != null) {
            SimpleContainer inventory = merchant.getInventory();
            for(int i = 6; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if(stack.isEmpty() || !deliverItems.contains(stack.getItem())) continue;
                WorkshopArea.insertInto(container, stack);
                inventory.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        }
        else {
            // chest is gone: try another one
            deliverStorages.remove(deliverStorage);
        }

        closeOpenContainer(level);
        deliverPos = null;
        interactTimer = 0;
    }

    /** A storage chest for the carried items: chests that already hold the item first. */
    private boolean findDeliverTarget() {
        SimpleContainer inventory = merchant.getInventory();
        for(StorageArea storage : deliverStorages) {
            if(storage.isRemoved()) continue;
            BlockPos fallback = null;
            for(int i = 6; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if(stack.isEmpty() || !deliverItems.contains(stack.getItem())) continue;
                for(Map.Entry<BlockPos, Container> entry : storage.storageMap.entrySet()) {
                    Container container = entry.getValue();
                    if(WorkshopArea.getSpaceFor(container, stack) <= 0) continue;
                    for(int j = 0; j < container.getContainerSize(); j++) {
                        if(ItemStack.isSameItemSameTags(container.getItem(j), stack)) {
                            deliverStorage = storage;
                            deliverPos = entry.getKey();
                            return true;
                        }
                    }
                    if(fallback == null) fallback = entry.getKey();
                }
            }
            if(fallback != null) {
                deliverStorage = storage;
                deliverPos = fallback;
                return true;
            }
        }
        return false;
    }

    // ── Storage helpers ───────────────────────────────────────────────────────

    /** Storages with the merchant checkbox within range of the market, nearest first, already scanned. */
    private List<StorageArea> findStorages(ServerLevel level, @Nullable MarketArea area) {
        if(area == null || !area.getUseStorage()) return new ArrayList<>();
        List<StorageArea> storages = level.getEntitiesOfClass(StorageArea.class,
                new AABB(area.blockPosition()).inflate(MarketArea.STORAGE_RANGE),
                storage -> storage.isAlive() && storage.canWorkHere(merchant));
        storages.sort(Comparator.comparingDouble(storage -> storage.distanceToSqr(area)));
        storages.forEach(StorageArea::scanStorageBlocks);
        storages.removeIf(storage -> storage.storageMap.isEmpty());
        return storages;
    }

    /** Emeralds, the villager currency and every currency of a player trade never leave the market and are never taken from a storage. */
    private boolean isCurrency(ItemStack stack, List<WorkersMerchantTrade> trades) {
        if(stack.is(Items.EMERALD) || stack.is(VillagerPriceTable.getCurrency())) return true;
        for(WorkersMerchantTrade trade : trades) {
            if(!trade.enabled || trade.isVillagerTrade || trade.currencyItem.isEmpty()) continue;
            if(stack.is(trade.currencyItem.getItem())) return true;
        }
        return false;
    }

    /** Goods another trade sells stay in the market. */
    private boolean isSellGood(ItemStack stack, List<WorkersMerchantTrade> trades) {
        for(WorkersMerchantTrade trade : trades) {
            if(!trade.enabled || trade.isVillagerBuy() || trade.tradeItem.isEmpty()) continue;
            if(stack.is(trade.tradeItem.getItem())) return true;
        }
        return false;
    }

    private static int countMatching(Collection<Container> containers, java.util.function.Predicate<ItemStack> predicate) {
        int count = 0;
        for(Container container : containers) {
            for(int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if(!stack.isEmpty() && predicate.test(stack)) count += stack.getCount();
            }
        }
        return count;
    }

    /** Adds an item to the list, the same item keeps the higher amount and the higher reserve. */
    private static void addWanted(List<Wanted> list, ItemStack template, boolean allowDamaged, int amount, int reserve) {
        if(amount <= 0) return;
        for(Wanted w : list) {
            if(w.matches(template)) {
                w.amount = Math.max(w.amount, amount);
                w.reserve = Math.max(w.reserve, reserve);
                return;
            }
        }
        list.add(new Wanted(template, allowDamaged, amount, reserve));
    }

    private boolean carriesFetchedItems() {
        return carries(fetchedItems);
    }

    private boolean carriesDeliverItems() {
        return carries(deliverItems);
    }

    private boolean carries(Set<Item> items) {
        if(items.isEmpty()) return false;
        SimpleContainer inventory = merchant.getInventory();
        for(int i = 6; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if(!stack.isEmpty() && items.contains(stack.getItem())) return true;
        }
        return false;
    }

    private int countFreeInventorySlots() {
        int free = 0;
        SimpleContainer inventory = merchant.getInventory();
        for(int i = 6; i < inventory.getContainerSize(); i++) {
            if(inventory.getItem(i).isEmpty()) free++;
        }
        return free;
    }

    @Nullable
    private BlockPos findNearestContainer(MarketArea area, BlockPos near) {
        if(area.containerMap.isEmpty()) area.scanContainers();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for(BlockPos pos : area.containerMap.keySet()) {
            double d = pos.distSqr(near);
            if(d < bestDist) {
                bestDist = d;
                best = pos;
            }
        }
        return best;
    }

    private void closeOpenContainer(ServerLevel level) {
        if(openContainerPos != null) {
            interactContainer(level, openContainerPos, false);
            openContainerPos = null;
        }
    }

    // Opens/closes a chest with the vanilla animation + sound. No-op for non-chest containers.
    private void interactContainer(ServerLevel level, @Nullable BlockPos pos, boolean open) {
        if(pos == null) return;
        BlockEntity be = level.getBlockEntity(pos);
        if(!(be instanceof ChestBlockEntity)) return;

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        CompoundTag data = be.getPersistentData();
        boolean isOpened = data.getBoolean("isOpened");

        if(open && !isOpened) {
            level.blockEvent(pos, block, 1, 1);
            level.playSound(null, pos, SoundEvents.CHEST_OPEN, merchant.getSoundSource(), 0.7F, 0.8F + 0.4F * merchant.getRandom().nextFloat());
            data.putBoolean("isOpened", true);
        }
        else if(!open && isOpened) {
            level.blockEvent(pos, block, 1, 0);
            level.playSound(null, pos, SoundEvents.CHEST_CLOSE, merchant.getSoundSource(), 0.7F, 0.8F + 0.4F * merchant.getRandom().nextFloat());
            data.putBoolean("isOpened", false);
        }
    }

    /** Like moveToPosition, but gives up after MAX_MOVE_TICKS so an unreachable chest can not block the merchant. */
    private boolean moveToPositionTimed(BlockPos pos, int thresholdBlocks) {
        if(!pos.equals(moveTarget)) {
            moveTarget = pos;
            moveTicks = 0;
        }
        if(++moveTicks > MAX_MOVE_TICKS) {
            merchant.getNavigation().stop();
            return false;
        }
        return moveToPosition(pos, thresholdBlocks);
    }

    private static class Wanted {
        private final ItemStack template;
        private final boolean allowDamaged;
        private int amount;
        private int reserve;

        private Wanted(ItemStack template, boolean allowDamaged, int amount, int reserve) {
            this.template = template.copyWithCount(1);
            this.allowDamaged = allowDamaged;
            this.amount = amount;
            this.reserve = reserve;
        }

        private boolean matches(ItemStack stack) {
            return MerchantEntity.areItemStacksEqual(stack, template, allowDamaged);
        }
    }

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
        // start() already claimed the working state; calling it here every
        // tick would override owner commands and pull the merchant back to work.
        merchant.getLookControl().setLookAt(pos.getCenter());
        return true;
    }

    @Nullable
    private MarketArea findBestArea(ServerLevel level) {
        // 1. Reconnect to already-assigned market (survives restarts)
        if (merchant.marketAreaUUID != null) {
            MarketArea assigned = level.getEntitiesOfClass(MarketArea.class,
                            merchant.getBoundingBox().inflate(256))
                    .stream()
                    .filter(a -> merchant.marketAreaUUID.equals(a.getUUID()))
                    .findFirst()
                    .orElse(null);

            if (assigned != null && !assigned.isRemoved() && assigned.canWorkHere(merchant)) {
                return assigned;
            }
            // Assigned area gone or no longer usable
            merchant.marketAreaUUID = null;
        }

        // 2. Find the best free unassigned area
        List<MarketArea> areas = level.getEntitiesOfClass(MarketArea.class,
                merchant.getBoundingBox().inflate(64));

        MarketArea best      = null;
        int        bestScore = -1;

        for (MarketArea area : areas) {
            if (area == null) continue;
            if (!area.canWorkHere(merchant)) continue;
            if (area.isAssigned()) continue;

            int score = area.getTime() * 10;
            if (score > bestScore) {
                bestScore = score;
                best      = area;
            }
        }
        return best;
    }

    private void setState(State s) {
        this.state = s;
        this.moveTarget = null;
        merchant.idleAtMarket = false;
    }

    public enum State {
        SELECT_WORK_AREA,
        WALK_TO_CENTER,
        WORKING,
        FETCH,
        STORE_FETCHED,
        DELIVER
    }
}