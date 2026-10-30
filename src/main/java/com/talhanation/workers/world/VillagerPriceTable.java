package com.talhanation.workers.world;

import com.talhanation.workers.WorkersMain;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.trading.MerchantOffer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vanilla villager prices of every item, built once on server start from {@link VillagerTrades}
 * (Forge has already put the trades of other mods in there) and sent to the clients on login.
 * The currency is the item most villager offers pay with: emeralds in vanilla, but mods or datapacks
 * may replace it. Prices are currency per item, random trades (e.g. enchanted books) are sampled and stored as a range.
 */
public class VillagerPriceTable {
    private static final int SAMPLES = 8;
    /** Crafted or smelted items are worth their ingredients plus this markup. */
    private static final double CRAFT_MARKUP = 1.1D;
    private static final int VALUE_PASSES = 8;
    private static final Map<Item, Entry> PRICES = new HashMap<>();
    /** Server only: value of an item in currency, from villagers or derived from recipes. */
    private static final Map<Item, Double> VALUES = new HashMap<>();
    private static Item currency = Items.EMERALD;

    public static void build(ServerLevel level) {
        PRICES.clear();
        Villager dummy = new Villager(EntityType.VILLAGER, level);
        RandomSource random = RandomSource.create(42L);
        List<Sample> samples = new ArrayList<>();

        for (Map.Entry<VillagerProfession, Int2ObjectMap<VillagerTrades.ItemListing[]>> entry : VillagerTrades.TRADES.entrySet()) {
            ResourceLocation key = BuiltInRegistries.VILLAGER_PROFESSION.getKey(entry.getKey());
            if (key == null) continue;
            String trader = "entity." + key.getNamespace() + ".villager." + key.getPath();

            dummy.setVillagerData(dummy.getVillagerData().setProfession(entry.getKey()));
            for (VillagerTrades.ItemListing[] listings : entry.getValue().values()) {
                addListings(listings, dummy, random, trader, samples);
            }
        }

        for (VillagerTrades.ItemListing[] listings : VillagerTrades.WANDERING_TRADER_TRADES.values()) {
            addListings(listings, dummy, random, "entity.minecraft.wandering_trader", samples);
        }

        currency = findCurrency(samples);
        for (Sample sample : samples) {
            addOffer(sample.offer(), sample.trader());
        }
        computeValues(level);

        WorkersMain.LOGGER.info("Workers: villager price table built with {} items, currency {}, {} items with a value",
                PRICES.size(), BuiltInRegistries.ITEM.getKey(currency), VALUES.size());
    }

    private static void addListings(VillagerTrades.ItemListing[] listings, Villager dummy, RandomSource random, String trader, List<Sample> samples) {
        if (listings == null) return;
        for (VillagerTrades.ItemListing listing : listings) {
            if (listing == null) continue;
            //Treasure maps search for structures, that would lag the server start
            if (listing.getClass().getSimpleName().contains("TreasureMap")) continue;

            for (int i = 0; i < SAMPLES; i++) {
                MerchantOffer offer;
                try {
                    offer = listing.getOffer(dummy, random);
                } catch (Exception e) {
                    break;
                }
                if (offer != null && !offer.getBaseCostA().isEmpty() && !offer.getResult().isEmpty()) {
                    samples.add(new Sample(offer, trader));
                }
            }
        }
    }

    /** The item that appears in the most offers, emeralds win a tie. */
    private static Item findCurrency(List<Sample> samples) {
        Map<Item, Integer> counts = new HashMap<>();
        for (Sample sample : samples) {
            Item cost = sample.offer().getBaseCostA().getItem();
            Item result = sample.offer().getResult().getItem();
            counts.merge(cost, 1, Integer::sum);
            if (result != cost) counts.merge(result, 1, Integer::sum);
        }

        Item best = Items.EMERALD;
        int bestCount = counts.getOrDefault(Items.EMERALD, 0);
        for (Map.Entry<Item, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount && entry.getKey() != Items.AIR) {
                best = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        return best;
    }

    private static void addOffer(MerchantOffer offer, String trader) {
        ItemStack costA = offer.getBaseCostA();
        ItemStack costB = offer.getCostB();
        ItemStack result = offer.getResult();

        if (result.is(currency) && !costA.is(currency)) {
            //villager buys costA
            PRICES.computeIfAbsent(costA.getItem(), item -> new Entry()).addBuy((double) result.getCount() / costA.getCount(), trader);
        }
        else if (costA.is(currency) && !result.is(currency)) {
            //villager sells the result, costB is an extra item he wants (e.g. a book)
            PRICES.computeIfAbsent(result.getItem(), item -> new Entry()).addSell((double) costA.getCount() / result.getCount(), costB, trader);
        }
    }

    /**
     * Value of every item in currency: the mean villager price, otherwise the cheapest recipe of items with a value
     * plus a markup. The markup makes sure loops (iron block and ingots) can never lower a value.
     */
    private static void computeValues(ServerLevel level) {
        VALUES.clear();
        for (Map.Entry<Item, Entry> entry : PRICES.entrySet()) {
            double value = entry.getValue().getMeanValue();
            if (value > 0) VALUES.put(entry.getKey(), value);
        }
        VALUES.put(currency, 1D);

        RegistryAccess access = level.registryAccess();
        RecipeManager manager = level.getRecipeManager();
        List<Recipe<?>> recipes = new ArrayList<>();
        recipes.addAll(manager.getAllRecipesFor(RecipeType.CRAFTING));
        recipes.addAll(manager.getAllRecipesFor(RecipeType.SMELTING));

        for (int pass = 0; pass < VALUE_PASSES; pass++) {
            boolean changed = false;
            for (Recipe<?> recipe : recipes) {
                ItemStack result;
                try {
                    result = recipe.getResultItem(access);
                } catch (Exception e) {
                    continue;
                }
                if (result.isEmpty() || result.is(currency)) continue;
                // villager prices always win
                Entry villager = PRICES.get(result.getItem());
                if (villager != null && villager.getMeanValue() > 0) continue;

                double cost = getIngredientCost(recipe.getIngredients());
                if (cost <= 0) continue;
                double value = cost / result.getCount() * CRAFT_MARKUP;
                Double old = VALUES.get(result.getItem());
                if (old == null || value < old - 0.0001D) {
                    VALUES.put(result.getItem(), value);
                    changed = true;
                }
            }
            if (!changed) break;
        }
    }

    /** Sum of the cheapest item of every ingredient, -1 if an ingredient has no value. */
    private static double getIngredientCost(NonNullList<Ingredient> ingredients) {
        double total = 0;
        boolean any = false;
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) continue;
            double best = -1;
            for (ItemStack stack : ingredient.getItems()) {
                Double value = VALUES.get(stack.getItem());
                if (value != null && (best < 0 || value < best)) best = value;
            }
            if (best < 0) return -1;
            total += best;
            any = true;
        }
        return any ? total : -1;
    }

    /** Server only: value of the item in currency, -1 if it has none (then it is not traded between merchants). */
    public static double getValue(Item item) {
        Double value = VALUES.get(item);
        return value == null ? -1 : value;
    }

    /**
     * Server only: one trade between merchants at the item value, like a villager trade:
     * several items for one currency, or one item for several currency. Null if the item has no value.
     */
    @Nullable
    public static Lot getLot(Item item) {
        double value = getValue(item);
        if (value <= 0) return null;
        if (value >= 1) return new Lot(1, (int) Math.round(value));
        return new Lot(Math.max(1, (int) Math.round(1 / value)), 1);
    }

    public static Map<Item, Entry> getPrices() {
        return PRICES;
    }

    @Nullable
    public static Entry get(Item item) {
        return PRICES.get(item);
    }

    /** The item villagers pay and ask with. Set on the server by {@link #build}, on the client by the login message. */
    public static Item getCurrency() {
        return currency;
    }

    public static ItemStack getCurrencyStack() {
        return new ItemStack(currency);
    }

    public static void setCurrency(Item item) {
        currency = item == null || item == Items.AIR ? Items.EMERALD : item;
    }

    /**
     * @param villagerSells true: price when a villager sells the item, false: when a villager buys it
     * @return the amounts as text, e.g. "20" items for "1" currency, or null if no villager trades the item this way
     */
    @Nullable
    public static Rate getRate(@Nullable Entry entry, boolean villagerSells) {
        if (entry == null) return null;
        double min = villagerSells ? entry.sellMin : entry.buyMin;
        double max = villagerSells ? entry.sellMax : entry.buyMax;
        if (min <= 0) return null;

        if (max >= 1) return new Rate("1", format(Math.max(1, Math.round(min)), Math.round(max)));
        return new Rate(format(Math.round(1 / max), Math.round(1 / min)), "1");
    }

    private static String format(long a, long b) {
        return a == b ? String.valueOf(a) : a + "-" + b;
    }

    private record Sample(MerchantOffer offer, String trader) {}

    /** Amount of items and of currency for one trade, as text because random trades are a range. */
    public record Rate(String items, String price) {}

    /** One trade between merchants: this many items for this much currency. */
    public record Lot(int items, int price) {}

    public static class Entry {
        /** Currency per item when a villager buys the item, -1 = no villager buys it. */
        public double buyMin = -1;
        public double buyMax = -1;
        public String buyTrader = "";
        /** Currency per item when a villager sells the item, -1 = no villager sells it. */
        public double sellMin = -1;
        public double sellMax = -1;
        public String sellTrader = "";
        /** Extra item the villager wants besides the currency, e.g. a book for an enchanted book. */
        public ItemStack sellExtra = ItemStack.EMPTY;

        /** Mean of what villagers pay and ask, or the one that exists. -1 if no villager trades the item. */
        public double getMeanValue() {
            double buy = buyMin > 0 ? (buyMin + buyMax) / 2D : -1;
            double sell = sellMin > 0 ? (sellMin + sellMax) / 2D : -1;
            if (buy > 0 && sell > 0) return (buy + sell) / 2D;
            return Math.max(buy, sell);
        }

        private void addBuy(double rate, String trader) {
            if (rate > buyMax) buyTrader = trader; //the villager who pays the most
            buyMin = buyMin < 0 ? rate : Math.min(buyMin, rate);
            buyMax = Math.max(buyMax, rate);
        }

        private void addSell(double rate, ItemStack extra, String trader) {
            if (sellMin < 0 || rate < sellMin) { //the villager who asks the least
                sellTrader = trader;
                sellExtra = extra.copy();
            }
            sellMin = sellMin < 0 ? rate : Math.min(sellMin, rate);
            sellMax = Math.max(sellMax, rate);
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeDouble(buyMin);
            buf.writeDouble(buyMax);
            buf.writeUtf(buyTrader);
            buf.writeDouble(sellMin);
            buf.writeDouble(sellMax);
            buf.writeUtf(sellTrader);
            buf.writeItem(sellExtra);
        }

        public static Entry read(FriendlyByteBuf buf) {
            Entry entry = new Entry();
            entry.buyMin = buf.readDouble();
            entry.buyMax = buf.readDouble();
            entry.buyTrader = buf.readUtf();
            entry.sellMin = buf.readDouble();
            entry.sellMax = buf.readDouble();
            entry.sellTrader = buf.readUtf();
            entry.sellExtra = buf.readItem();
            return entry;
        }
    }
}
