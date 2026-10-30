package com.talhanation.workers.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.entities.workarea.MarketArea;
import com.talhanation.workers.entities.workarea.StorageArea;
import com.talhanation.workers.world.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Trading trips of a travelling merchant ({@link MerchantEntity#isFarTrader()}). During the day he packs goods
 * and currency, visits one market after another (on foot when close, otherwise he is taken out of the world
 * for the travel time by {@link WorkersRemoteTravel}), trades with the merchant there for 1-2 minutes and
 * goes home when nothing is left to do or no market can be reached before the night.
 * Everything that must survive the travel is stored in the {@link MerchantTrip} on the merchant.
 */
public class MerchantTripGoal extends Goal {

    private static final int CHECK_INTERVAL   = 600;    // how often an idle merchant thinks about a trip
    private static final int WALK_DISTANCE    = 64;     // closer markets are reached on foot
    private static final int WALK_TIMEOUT     = 1200;   // after one minute of walking he is put into the market
    private static final int STAY_MIN         = 1200;   // time at a market: 1-2 minutes
    private static final int STAY_MAX         = 2400;
    private static final int DAY_END          = 12000;  // merchants do not work at night
    private static final int ARRIVAL_SPREAD   = 16;     // travellers appear this far from the market
    private static final int ESCORT_RANGE     = 16;
    private static final int TRADE_COOLDOWN   = 50;
    private static final int INTERACT_TIME    = 20;
    private static final int MAX_CURRENCY_STACKS = 2;
    private static final int FIRST_INV_SLOT   = 6;
    private static final double WALK_SPEED    = 4.0D;   // blocks per second, only to estimate walking time

    private final MerchantEntity merchant;
    private int checkCooldown;
    private int interactTimer;
    private int tradeCooldown;
    private int tradesThisVisit;
    @Nullable private MarketArea pendingHome;
    @Nullable private BlockPos moveTarget;
    /** Storages of the visited market, read once per visit. */
    private List<StorageArea> hostStorages = new ArrayList<>();

    public MerchantTripGoal(MerchantEntity merchant) {
        this.merchant = merchant;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    // ── Goal ──────────────────────────────────────────────────────────────────

    @Override
    public boolean canUse() {
        if (!(merchant.getCommandSenderWorld() instanceof ServerLevel level)) return false;
        if (merchant.getTrip() != null) return merchant.shouldWork();
        return shouldStartTrip(level);
    }

    @Override
    public boolean canContinueToUse() {
        return merchant.getTrip() != null && merchant.shouldWork();
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        if (merchant.getFollowState() == 0) merchant.setFollowState(6);
        if (merchant.getTrip() == null && pendingHome != null) {
            MerchantTrip trip = new MerchantTrip(pendingHome.getUUID(), pendingHome.blockPosition());
            merchant.setTrip(trip);
        }
        pendingHome = null;
        interactTimer = 0;
        tradeCooldown = 0;
        moveTarget = null;
    }

    @Override
    public void stop() {
        merchant.getNavigation().stop();
        moveTarget = null;
    }

    @Override
    public void tick() {
        if (!(merchant.getCommandSenderWorld() instanceof ServerLevel level)) return;
        MerchantTrip trip = merchant.getTrip();
        if (trip == null) return;

        switch (trip.phase) {
            case PACK      -> tickPack(level, trip);
            case TO_MARKET -> tickToMarket(level, trip);
            case AT_MARKET -> tickAtMarket(level, trip);
            case RETURN    -> tickReturn(level, trip);
            case UNPACK    -> tickUnpack(level, trip);
        }
    }

    // ── Start ─────────────────────────────────────────────────────────────────

    private boolean shouldStartTrip(ServerLevel level) {
        if (!merchant.isFarTrader() || merchant.isCreative() || !merchant.shouldWork()) return false;
        if (++checkCooldown < CHECK_INTERVAL) return false;
        checkCooldown = 0;

        MarketArea home = merchant.currentMarketArea;
        if (home == null || !merchant.idleAtMarket || merchant.hasVisitor()) return false;
        if (!isWorkTime(level)) return false;
        if (!hasSomethingToDo(level, home)) return false;
        if (chooseTarget(level, merchant.blockPosition(), null) == null) return false;

        pendingHome = home;
        return true;
    }

    /** Goods above the amount he keeps, or a buy trade below its target with currency above the reserve. */
    private boolean hasSomethingToDo(ServerLevel level, MarketArea home) {
        List<StorageArea> storages = home.findStorages(level, merchant);
        int currency = home.countItemInContainers(VillagerPriceTable.getCurrencyStack(), false) - home.getCurrencyReserve();
        for (WorkersMerchantTrade trade : merchant.getTrades()) {
            if (!isTradable(trade)) continue;
            int stock = countStock(home, storages, trade.tradeItem);
            if (trade.isVillagerBuy()) {
                VillagerPriceTable.Lot lot = VillagerPriceTable.getLot(trade.tradeItem.getItem());
                if (lot != null && trade.storageAmount - stock > 0 && currency >= lot.price()) return true;
            }
            else if (stock - trade.storageAmount > 0 && home.countItemInContainers(trade.tradeItem, false) > 0) {
                return true;
            }
        }
        return false;
    }

    // ── Phases ────────────────────────────────────────────────────────────────

    /** Takes the goods above the kept amount and a currency budget out of the home market. */
    private void tickPack(ServerLevel level, MerchantTrip trip) {
        MarketArea home = findMarket(level, trip.homeMarket);
        if (home == null) {
            endTrip(level, trip, null);
            return;
        }
        home.scanContainers();
        BlockPos chest = findNearestContainer(home);
        if (chest != null && !interactAt(chest)) return;

        List<StorageArea> storages = home.findStorages(level, merchant);
        boolean buys = false;
        for (WorkersMerchantTrade trade : merchant.getTrades()) {
            if (!isTradable(trade) || !trade.isVillagerBuy()) continue;
            if (VillagerPriceTable.getLot(trade.tradeItem.getItem()) == null) continue;
            int left = trade.storageAmount - countStock(home, storages, trade.tradeItem);
            if (left > 0) {
                trip.buyLeft.put(trade.uuid, left);
                buys = true;
            }
        }

        // currency first, it is needed for every purchase
        ItemStack currency = VillagerPriceTable.getCurrencyStack();
        if (buys) {
            int need = 0;
            for (WorkersMerchantTrade trade : merchant.getTrades()) {
                Integer left = trip.buyLeft.get(trade.uuid);
                VillagerPriceTable.Lot lot = VillagerPriceTable.getLot(trade.tradeItem.getItem());
                if (left == null || lot == null) continue;
                need += (left + lot.items() - 1) / lot.items() * lot.price();
            }
            int available = home.countItemInContainers(currency, false) - home.getCurrencyReserve();
            int budget = Math.min(need, Math.min(available, MAX_CURRENCY_STACKS * currency.getMaxStackSize()));
            if (budget > 0) takeFromMarket(home, currency, budget, 0);
        }

        // goods to sell, two free slots stay for the purchases
        int keepFree = buys ? 2 : 0;
        for (WorkersMerchantTrade trade : merchant.getTrades()) {
            if (!isTradable(trade) || trade.isVillagerBuy()) continue;
            if (VillagerPriceTable.getLot(trade.tradeItem.getItem()) == null) continue;
            int surplus = countStock(home, storages, trade.tradeItem) - trade.storageAmount;
            int inMarket = home.countItemInContainers(trade.tradeItem, false);
            int amount = Math.min(surplus, inMarket);
            if (amount > 0) takeFromMarket(home, trade.tradeItem, amount, keepFree);
        }
        home.scanContainers();
        trip.currencyPacked = countCarried(currency);

        if (!hasWorkLeft(trip)) {
            trip.phase = MerchantTrip.Phase.UNPACK;
            interactTimer = 0;
            return;
        }

        MarketDirectory.Entry target = chooseTarget(level, merchant.blockPosition(), trip);
        if (target == null) {
            trip.phase = MerchantTrip.Phase.UNPACK;
            interactTimer = 0;
            return;
        }

        home.setMerchantName(merchant.getName().getString());
        home.setMerchantTravelling(true);
        headTo(level, trip, target);
    }

    private void tickToMarket(ServerLevel level, MerchantTrip trip) {
        if (!isWorkTime(level)) {
            goHome(level, trip);
            return;
        }
        if (trip.walkStart == 0) trip.walkStart = level.getGameTime();

        MarketArea market = trip.targetMarket != null ? findMarket(level, trip.targetMarket) : null;
        BlockPos center = market != null ? BlockPos.containing(market.getArea().getCenter()) : trip.targetPos;

        boolean arrived = isNear(center, 4);
        if (!arrived && level.getGameTime() - trip.walkStart > WALK_TIMEOUT) {
            teleportWithEscort(level, center);
            arrived = true;
        }
        if (!arrived) {
            moveTo(center);
            return;
        }

        merchant.getNavigation().stop();
        if (market == null) {
            // the market is gone
            if (trip.targetMarket != null) trip.visited.add(trip.targetMarket);
            nextOrHome(level, trip);
            return;
        }
        arriveAtMarket(level, trip, market);
    }

    private void tickAtMarket(ServerLevel level, MerchantTrip trip) {
        MarketArea market = trip.targetMarket != null ? findMarket(level, trip.targetMarket) : null;
        if (market == null || !isWorkTime(level) || level.getGameTime() >= trip.stayUntil) {
            leaveMarket(level, trip);
            return;
        }

        MerchantEntity host = getHost(level, market);
        if (host == null || !canTradeAt(market)) {
            // nobody to trade with: wait at the market
            BlockPos center = BlockPos.containing(market.getArea().getCenter());
            if (!isNear(center, 4)) moveTo(center);
            else merchant.getNavigation().stop();
            return;
        }

        host.setVisitor(merchant.getUUID(), level.getGameTime() + 100);
        host.getLookControl().setLookAt(merchant, 30, 30);
        if (merchant.distanceTo(host) > 3.0F) {
            moveTo(host.blockPosition());
            return;
        }
        merchant.getNavigation().stop();
        merchant.getLookControl().setLookAt(host, 30, 30);

        if (--tradeCooldown > 0) return;
        tradeCooldown = TRADE_COOLDOWN;
        if (tradeOnce(level, trip, host, market)) {
            tradesThisVisit++;
            MerchantMemory.Entry entry = merchant.getMemory().get(market.getUUID());
            if (entry != null) entry.trades++;
            spawnTradeEffects(level, host);
        }
    }

    private void tickReturn(ServerLevel level, MerchantTrip trip) {
        if (trip.walkStart == 0) trip.walkStart = level.getGameTime();

        MarketArea home = findMarket(level, trip.homeMarket);
        BlockPos center = home != null ? BlockPos.containing(home.getArea().getCenter()) : trip.homePos;

        boolean arrived = isNear(center, 4);
        if (!arrived && level.getGameTime() - trip.walkStart > WALK_TIMEOUT) {
            teleportWithEscort(level, center);
            arrived = true;
        }
        if (!arrived) {
            moveTo(center);
            return;
        }
        merchant.getNavigation().stop();
        trip.phase = MerchantTrip.Phase.UNPACK;
        interactTimer = 0;
    }

    /** Puts goods and currency back into the home market and reports to the owner. */
    private void tickUnpack(ServerLevel level, MerchantTrip trip) {
        MarketArea home = findMarket(level, trip.homeMarket);
        if (home == null) {
            endTrip(level, trip, null);
            return;
        }
        home.scanContainers();
        BlockPos chest = findNearestContainer(home);
        if (chest != null && !interactAt(chest)) return;

        int currencyBack = countCarried(VillagerPriceTable.getCurrencyStack());
        SimpleContainer inventory = merchant.getInventory();
        for (int i = FIRST_INV_SLOT; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !isTripItem(stack)) continue;
            int rest = home.depositItemToContainers(stack, stack.getCount());
            if (rest <= 0) inventory.setItem(i, ItemStack.EMPTY);
            else stack.setCount(rest);
        }
        inventory.setChanged();
        home.scanContainers();

        endTrip(level, trip, home);
        if (!trip.visited.isEmpty()) sendReport(trip, currencyBack - trip.currencyPacked);
    }

    // ── Moving between markets ────────────────────────────────────────────────

    private void headTo(ServerLevel level, MerchantTrip trip, MarketDirectory.Entry target) {
        trip.setTarget(target.uuid(), target.pos(), target.name());
        trip.phase = MerchantTrip.Phase.TO_MARKET;
        travel(level, trip, target.pos());
    }

    private void goHome(ServerLevel level, MerchantTrip trip) {
        trip.phase = MerchantTrip.Phase.RETURN;
        trip.walkStart = 0;
        travel(level, trip, trip.homePos);
    }

    /** Far away: out of the world for the travel time. Close: walking, the walk timer starts now. */
    private void travel(ServerLevel level, MerchantTrip trip, BlockPos destination) {
        merchant.getNavigation().stop();
        moveTarget = null;
        double distance = Math.sqrt(merchant.blockPosition().distSqr(new BlockPos(destination.getX(), merchant.getBlockY(), destination.getZ())));
        if (distance > WALK_DISTANCE) {
            trip.walkStart = 0; // starts on arrival
            List<AbstractRecruitEntity> escort = findEscort(level);
            if (WorkersRemoteTravel.depart(level, merchant, escort, destination, ARRIVAL_SPREAD, WorkersRemoteTravel.getTravelTicks(distance))) return;
        }
        trip.walkStart = level.getGameTime();
    }

    private void arriveAtMarket(ServerLevel level, MerchantTrip trip, MarketArea market) {
        trip.phase = MerchantTrip.Phase.AT_MARKET;
        trip.visited.add(market.getUUID());
        trip.stayUntil = level.getGameTime() + STAY_MIN + merchant.getRandom().nextInt(STAY_MAX - STAY_MIN + 1);
        tradesThisVisit = 0;
        tradeCooldown = TRADE_COOLDOWN;

        MerchantEntity host = getHost(level, market);
        hostStorages = host != null ? market.findStorages(level, host) : new ArrayList<>();
        learnMarket(level, market, host);
    }

    private void leaveMarket(ServerLevel level, MerchantTrip trip) {
        if (trip.targetMarket != null) {
            MerchantMemory.Entry entry = merchant.getMemory().get(trip.targetMarket);
            if (entry != null) {
                if (tradesThisVisit > 0) entry.emptyVisits = 0;
                else entry.emptyVisits++;
            }
        }
        hostStorages = new ArrayList<>();
        nextOrHome(level, trip);
    }

    private void nextOrHome(ServerLevel level, MerchantTrip trip) {
        MarketDirectory.Entry next = isWorkTime(level) && hasWorkLeft(trip) ? chooseTarget(level, merchant.blockPosition(), trip) : null;
        if (next != null) headTo(level, trip, next);
        else goHome(level, trip);
    }

    private void endTrip(ServerLevel level, MerchantTrip trip, @Nullable MarketArea home) {
        if (home != null) home.setMerchantTravelling(false);
        merchant.getMemory().forgetOld(level.getGameTime());
        merchant.setTrip(null);
        merchant.getNavigation().stop();
    }

    // ── Choosing markets ──────────────────────────────────────────────────────

    /**
     * The best market to visit next: favorites first, then markets that buy what he sells or sell what he buys,
     * then unknown or long not visited markets. Only markets that can be reached before the night, nearest first.
     */
    @Nullable
    private MarketDirectory.Entry chooseTarget(ServerLevel level, BlockPos from, @Nullable MerchantTrip trip) {
        long now = level.getGameTime();
        String dimension = level.dimension().location().toString();
        Set<String> sellItems = new HashSet<>();
        Set<String> buyItems = new HashSet<>();
        for (WorkersMerchantTrade trade : merchant.getTrades()) {
            if (!isTradable(trade)) continue;
            String id = itemId(trade.tradeItem);
            if (trade.isVillagerBuy()) {
                if (trip == null || trip.buyLeft.getOrDefault(trade.uuid, 0) > 0) buyItems.add(id);
            }
            else if (trip == null || countCarried(trade.tradeItem) > 0) {
                sellItems.add(id);
            }
        }

        String team = merchant.getTeam() != null ? merchant.getTeam().getName() : "";
        MarketDirectory.Entry best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (MarketDirectory.Entry entry : MarketDirectory.getMarkets(level.getServer())) {
            if (!entry.dimension().equals(dimension) || !entry.acceptForeign() || entry.merchant() == null) continue;
            if (entry.uuid().equals(merchant.marketAreaUUID)) continue;
            if (trip != null && (trip.visited.contains(entry.uuid()) || entry.uuid().equals(trip.homeMarket))) continue;
            if (!MerchantTradeRules.canTrade(merchant.getOwnerUUID(), team, entry.owner(), entry.team())) continue;

            MerchantMemory.Entry known = merchant.getMemory().get(entry.uuid());
            if (known != null && known.avoid) continue;
            // nothing was traded there last time: wait a day before trying again
            if (known != null && known.emptyVisits > 0 && now - known.lastVisit < 24000L) continue;

            double distance = horizontalDistance(from, entry.pos());
            if (!canArriveToday(level.getDayTime(), distance)) continue;

            double score;
            boolean favorite = known != null && known.favorite;
            if (known != null && !known.isStale(now)) {
                boolean match = containsAny(known.buys, sellItems) || containsAny(known.sells, buyItems);
                if (!match && !favorite) continue;
                score = (favorite ? 10000 : 0) + (match ? 1000 : 0);
            }
            else {
                // unknown or old knowledge: worth a look
                score = (favorite ? 10000 : 0) + 100;
            }
            score -= distance / 16D;
            if (score > bestScore) {
                bestScore = score;
                best = entry;
            }
        }
        return best;
    }

    /**
     * Whether the merchant can arrive and stay before the night. Shared with the market list GUI.
     * @param dayTime level day time
     */
    public static boolean canArriveToday(long dayTime, double distance) {
        long timeOfDay = dayTime % 24000L;
        long travel = distance <= WALK_DISTANCE ? (long) (distance / WALK_SPEED * 20D) : WorkersRemoteTravel.getTravelTicks(distance);
        return timeOfDay + travel + WALK_TIMEOUT + STAY_MAX <= DAY_END;
    }

    private static boolean isWorkTime(ServerLevel level) {
        return level.isDay() && level.getDayTime() % 24000L < DAY_END;
    }

    // ── Trading ───────────────────────────────────────────────────────────────

    /**
     * One trade with the host merchant, the first that is possible:
     * 1. sell to a buy trade of the host, 2. buy from a cheaper player trade of the host, 3. buy from a sell trade of the host.
     * Villager trades of both merchants trade at the item value (lots like a villager trade).
     */
    private boolean tradeOnce(ServerLevel level, MerchantTrip trip, MerchantEntity host, MarketArea market) {
        ItemStack currency = VillagerPriceTable.getCurrencyStack();
        List<WorkersMerchantTrade> hostTrades = host.getTrades();

        // 1. sell
        for (WorkersMerchantTrade mine : merchant.getTrades()) {
            if (!isTradable(mine) || mine.isVillagerBuy()) continue;
            VillagerPriceTable.Lot lot = VillagerPriceTable.getLot(mine.tradeItem.getItem());
            if (lot == null || countCarried(mine.tradeItem) < lot.items()) continue;

            for (WorkersMerchantTrade theirs : hostTrades) {
                if (!isTradable(theirs) || !theirs.isVillagerBuy() || !sameItem(theirs.tradeItem, mine.tradeItem)) continue;
                if (theirs.storageAmount - countStock(market, hostStorages, theirs.tradeItem) < lot.items()) continue;
                if (market.countItemInContainers(currency, false) - market.getCurrencyReserve() < lot.price()) continue;
                if (getMarketSpaceFor(market, mine.tradeItem) < lot.items() || getSpaceFor(currency) < lot.price()) continue;

                removeCarried(mine.tradeItem, lot.items());
                market.depositItemToContainers(mine.tradeItem, lot.items());
                market.shrinkItemFromContainers(currency, lot.price(), false);
                addCarried(currency, lot.price());
                trip.sold.merge(itemId(mine.tradeItem), lot.items(), Integer::sum);
                return true;
            }
        }

        for (WorkersMerchantTrade mine : merchant.getTrades()) {
            if (!isTradable(mine) || !mine.isVillagerBuy()) continue;
            int left = trip.buyLeft.getOrDefault(mine.uuid, 0);
            if (left <= 0) continue;
            double value = VillagerPriceTable.getValue(mine.tradeItem.getItem());
            if (value <= 0) continue;

            // 2. a player trade of the host that is cheaper than the villager price, its limits apply
            for (WorkersMerchantTrade theirs : hostTrades) {
                if (theirs.isVillagerTrade || !theirs.enabled || theirs.tradeItem.isEmpty()) continue;
                if (!sameItem(theirs.tradeItem, mine.tradeItem) || !theirs.currencyItem.is(currency.getItem())) continue;
                int amount = theirs.tradeItem.getCount();
                int price = theirs.currencyItem.getCount();
                if (amount <= 0 || (double) price / amount >= value) continue;
                if (theirs.maxTrades != -1 && theirs.currentTrades >= theirs.maxTrades) continue;
                if (market.countItemInContainers(theirs.tradeItem, theirs.allowDamagedCurrency) < amount) continue;
                if (countCarried(currency) < price || getSpaceFor(theirs.tradeItem) < amount || getMarketSpaceFor(market, currency) < price) continue;

                market.shrinkItemFromContainers(theirs.tradeItem, amount, theirs.allowDamagedCurrency);
                market.depositItemToContainers(currency, price);
                removeCarried(currency, price);
                addCarried(theirs.tradeItem, amount);
                countHostTrade(host, theirs);
                trip.buyLeft.put(mine.uuid, left - amount);
                trip.bought.merge(itemId(mine.tradeItem), amount, Integer::sum);
                return true;
            }

            // 3. a sell trade of the host at the item value
            VillagerPriceTable.Lot lot = VillagerPriceTable.getLot(mine.tradeItem.getItem());
            if (lot == null) continue;
            for (WorkersMerchantTrade theirs : hostTrades) {
                if (!isTradable(theirs) || theirs.isVillagerBuy() || !sameItem(theirs.tradeItem, mine.tradeItem)) continue;
                if (countStock(market, hostStorages, theirs.tradeItem) - theirs.storageAmount < lot.items()) continue;
                if (market.countItemInContainers(theirs.tradeItem, false) < lot.items()) continue;
                if (countCarried(currency) < lot.price() || getSpaceFor(theirs.tradeItem) < lot.items() || getMarketSpaceFor(market, currency) < lot.price()) continue;

                market.shrinkItemFromContainers(theirs.tradeItem, lot.items(), false);
                market.depositItemToContainers(currency, lot.price());
                removeCarried(currency, lot.price());
                addCarried(theirs.tradeItem, lot.items());
                trip.buyLeft.put(mine.uuid, left - lot.items());
                trip.bought.merge(itemId(mine.tradeItem), lot.items(), Integer::sum);
                return true;
            }
        }
        return false;
    }

    private static void countHostTrade(MerchantEntity host, WorkersMerchantTrade trade) {
        List<WorkersMerchantTrade> list = host.getTrades();
        for (WorkersMerchantTrade stored : list) {
            if (stored.uuid.equals(trade.uuid)) {
                stored.currentTrades++;
                break;
            }
        }
        host.setTrades(list);
    }

    /** The merchant of the market, if he is there and free (no villager session, not travelling himself). */
    @Nullable
    private MerchantEntity getHost(ServerLevel level, MarketArea market) {
        if (market.assignedMerchantUUID == null) return null;
        Entity entity = level.getEntity(market.assignedMerchantUUID);
        if (!(entity instanceof MerchantEntity host) || !host.isAlive() || host == merchant) return null;
        if (host.getTrip() != null || host.currentMarketArea == null || !host.currentMarketArea.getUUID().equals(market.getUUID())) return null;
        if (host.activeTradingVillager != null) return null;
        if (!host.idleAtMarket && !host.isVisitor(merchant.getUUID())) return null;
        return host;
    }

    private boolean canTradeAt(MarketArea market) {
        if (!market.getAcceptForeignMerchants()) return false;
        String team = merchant.getTeam() != null ? merchant.getTeam().getName() : "";
        return MerchantTradeRules.canTrade(merchant.getOwnerUUID(), team, market.getPlayerUUID(), market.getTeamStringID());
    }

    /** Remembers what the market buys and sells. The host learns the market of the visitor in return. */
    private void learnMarket(ServerLevel level, MarketArea market, @Nullable MerchantEntity host) {
        long now = level.getGameTime();
        String dimension = level.dimension().location().toString();
        List<String> buys = new ArrayList<>();
        List<String> sells = new ArrayList<>();
        if (host != null) collectOffers(host.getTrades(), buys, sells);
        merchant.getMemory().learn(market.getUUID(), market.getMarketName(), market.blockPosition(), dimension, market.getPlayerName(), buys, sells, now);

        MerchantTrip trip = merchant.getTrip();
        if (host == null || trip == null) return;
        MarketDirectory.Entry home = MarketDirectory.getMarket(level.getServer(), trip.homeMarket);
        if (home == null) return;
        List<String> myBuys = new ArrayList<>();
        List<String> mySells = new ArrayList<>();
        collectOffers(merchant.getTrades(), myBuys, mySells);
        host.getMemory().learn(home.uuid(), home.name(), home.pos(), home.dimension(), home.ownerName(), myBuys, mySells, now);
    }

    /** Villager trades buy and sell, player trades that are paid with the villager currency sell. */
    private static void collectOffers(List<WorkersMerchantTrade> trades, List<String> buys, List<String> sells) {
        Item currency = VillagerPriceTable.getCurrency();
        for (WorkersMerchantTrade trade : trades) {
            if (!trade.enabled || trade.tradeItem.isEmpty()) continue;
            String id = itemId(trade.tradeItem);
            if (trade.isVillagerTrade) {
                List<String> list = trade.isVillagerBuy() ? buys : sells;
                if (!list.contains(id)) list.add(id);
            }
            else if (trade.currencyItem.is(currency) && !sells.contains(id)) {
                sells.add(id);
            }
        }
    }

    private void spawnTradeEffects(ServerLevel level, MerchantEntity host) {
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, merchant.getX(), merchant.getY() + 1.8, merchant.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, host.getX(), host.getY() + 1.8, host.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
        level.playSound(null, merchant.blockPosition(), SoundEvents.VILLAGER_TRADE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        merchant.swing(InteractionHand.MAIN_HAND);
    }

    // ── Report ────────────────────────────────────────────────────────────────

    private void sendReport(MerchantTrip trip, int currencyDelta) {
        Component currency = VillagerPriceTable.getCurrencyStack().getHoverName();
        String delta = (currencyDelta >= 0 ? "+" : "") + currencyDelta;
        merchant.notifyOwnerAlways(Component.translatable("chat.workers.text.merchantTripReport",
                merchant.getName().getString(), trip.visited.size(), delta, currency, listItems(trip.sold), listItems(trip.bought)));
    }

    private static Component listItems(Map<String, Integer> counts) {
        if (counts.isEmpty()) return Component.literal("-");
        MutableComponent out = Component.empty();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            Item item = id != null ? BuiltInRegistries.ITEM.get(id) : null;
            if (!first) out.append(", ");
            out.append(entry.getValue() + "x ");
            out.append(item != null ? item.getDescription() : Component.literal(entry.getKey()));
            first = false;
        }
        return out;
    }

    // ── Escort ────────────────────────────────────────────────────────────────

    /** Recruits that protect the merchant travel with him. */
    private List<AbstractRecruitEntity> findEscort(ServerLevel level) {
        return level.getEntitiesOfClass(AbstractRecruitEntity.class, merchant.getBoundingBox().inflate(ESCORT_RANGE),
                recruit -> recruit.isAlive() && recruit.getShouldProtect() && merchant.getUUID().equals(recruit.getProtectUUID()));
    }

    private void teleportWithEscort(ServerLevel level, BlockPos center) {
        List<AbstractRecruitEntity> escort = findEscort(level);
        BlockPos pos = WorkersRemoteTravel.findSpawnPos(level, center, 2, merchant.getRandom());
        merchant.teleportTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        for (AbstractRecruitEntity recruit : escort) {
            BlockPos near = WorkersRemoteTravel.findSpawnPos(level, pos, 3, merchant.getRandom());
            Entity root = recruit.getRootVehicle();
            root.teleportTo(near.getX() + 0.5D, near.getY(), near.getZ() + 0.5D);
        }
        merchant.getNavigation().stop();
    }

    // ── Trades and stock ──────────────────────────────────────────────────────

    /** Villager trades with an item that has a value can be traded between merchants. */
    private static boolean isTradable(WorkersMerchantTrade trade) {
        return trade.isVillagerTrade && trade.enabled && !trade.tradeItem.isEmpty()
                && !trade.tradeItem.is(VillagerPriceTable.getCurrency());
    }

    private boolean hasWorkLeft(MerchantTrip trip) {
        boolean hasCurrency = countCarried(VillagerPriceTable.getCurrencyStack()) > 0;
        for (WorkersMerchantTrade trade : merchant.getTrades()) {
            if (!isTradable(trade)) continue;
            if (trade.isVillagerBuy()) {
                if (hasCurrency && trip.buyLeft.getOrDefault(trade.uuid, 0) > 0) return true;
            }
            else if (countCarried(trade.tradeItem) > 0) {
                return true;
            }
        }
        return false;
    }

    /** Items he took along or bought: the currency and the items of his trades. */
    private boolean isTripItem(ItemStack stack) {
        if (stack.is(VillagerPriceTable.getCurrency())) return true;
        for (WorkersMerchantTrade trade : merchant.getTrades()) {
            if (trade.isVillagerTrade && sameItem(trade.tradeItem, stack)) return true;
        }
        return false;
    }

    private static int countStock(MarketArea market, List<StorageArea> storages, ItemStack template) {
        int stock = market.countItemInContainers(template, false);
        for (StorageArea storage : storages) {
            for (Container container : storage.storageMap.values()) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (sameItem(template, stack)) stock += stack.getCount();
                }
            }
        }
        return stock;
    }

    /** With NBT (e.g. an enchanted book) the item must match exactly, otherwise the item is enough. */
    private static boolean sameItem(ItemStack template, ItemStack stack) {
        if (template.isEmpty() || stack.isEmpty()) return false;
        if (template.hasTag()) return ItemStack.isSameItemSameTags(template, stack);
        return stack.is(template.getItem());
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    // ── Inventory ─────────────────────────────────────────────────────────────

    private int countCarried(ItemStack template) {
        int count = 0;
        SimpleContainer inventory = merchant.getInventory();
        for (int i = FIRST_INV_SLOT; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (sameItem(template, stack)) count += stack.getCount();
        }
        return count;
    }

    private void removeCarried(ItemStack template, int amount) {
        SimpleContainer inventory = merchant.getInventory();
        for (int i = FIRST_INV_SLOT; i < inventory.getContainerSize() && amount > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!sameItem(template, stack)) continue;
            int take = Math.min(amount, stack.getCount());
            stack.shrink(take);
            amount -= take;
            if (stack.isEmpty()) inventory.setItem(i, ItemStack.EMPTY);
        }
        inventory.setChanged();
    }

    private void addCarried(ItemStack template, int amount) {
        while (amount > 0) {
            int put = Math.min(amount, template.getMaxStackSize());
            ItemStack rest = merchant.addItem(template.copyWithCount(put));
            int added = put - rest.getCount();
            if (added <= 0) break;
            amount -= added;
        }
    }

    /** How many of the item still fit into the inventory. */
    private int getSpaceFor(ItemStack template) {
        int space = 0;
        SimpleContainer inventory = merchant.getInventory();
        for (int i = FIRST_INV_SLOT; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) space += template.getMaxStackSize();
            else if (ItemStack.isSameItemSameTags(stack, template)) space += stack.getMaxStackSize() - stack.getCount();
        }
        return space;
    }

    private int countFreeSlots() {
        int free = 0;
        SimpleContainer inventory = merchant.getInventory();
        for (int i = FIRST_INV_SLOT; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).isEmpty()) free++;
        }
        return free;
    }

    /** Moves up to amount from the market into the inventory, keepFree slots stay empty. */
    private void takeFromMarket(MarketArea market, ItemStack template, int amount, int keepFree) {
        int max = template.getMaxStackSize();
        while (amount > 0) {
            int free = countFreeSlots();
            int partial = getSpaceFor(template) - free * max;
            int put;
            if (partial > 0) put = Math.min(amount, partial);
            else if (free > keepFree) put = Math.min(amount, max); // a new stack needs a free slot
            else break;

            put = Math.min(put, market.countItemInContainers(template, false));
            if (put <= 0) break;
            market.shrinkItemFromContainers(template, put, false);
            addCarried(template, put);
            amount -= put;
        }
    }

    /** How many of the item still fit into the market containers. */
    private static int getMarketSpaceFor(MarketArea market, ItemStack template) {
        int space = 0;
        for (Container container : market.containerMap.values()) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty()) space += template.getMaxStackSize();
                else if (ItemStack.isSameItemSameTags(stack, template)) space += stack.getMaxStackSize() - stack.getCount();
            }
        }
        return space;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Nullable
    private static MarketArea findMarket(ServerLevel level, UUID uuid) {
        Entity entity = level.getEntity(uuid);
        return entity instanceof MarketArea market && !market.isRemoved() ? market : null;
    }

    @Nullable
    private BlockPos findNearestContainer(MarketArea market) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : market.containerMap.keySet()) {
            double dist = pos.distToCenterSqr(merchant.position());
            if (dist < bestDist) {
                bestDist = dist;
                best = pos;
            }
        }
        return best;
    }

    /** Walks to the chest and stands there for a moment. True when done. */
    private boolean interactAt(BlockPos pos) {
        if (!isNear(pos, 3) && interactTimer == 0) {
            moveTo(pos);
            return false;
        }
        merchant.getNavigation().stop();
        merchant.getLookControl().setLookAt(Vec3.atCenterOf(pos));
        if (++interactTimer < INTERACT_TIME) {
            if (interactTimer % 5 == 0) merchant.swing(InteractionHand.MAIN_HAND);
            return false;
        }
        interactTimer = 0;
        return true;
    }

    private boolean isNear(BlockPos pos, double blocks) {
        return merchant.getHorizontalDistanceTo(Vec3.atCenterOf(pos)) < blocks * blocks;
    }

    private void moveTo(BlockPos pos) {
        if (!pos.equals(moveTarget) || merchant.getNavigation().isDone()) {
            moveTarget = pos;
            merchant.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.8D);
        }
    }

    private static double horizontalDistance(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static boolean containsAny(Collection<String> a, Collection<String> b) {
        for (String value : a) {
            if (b.contains(value)) return true;
        }
        return false;
    }
}
