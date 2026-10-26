package com.talhanation.workers.world;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.config.WorkersServerConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Server-side price calculation for buildings in {@link com.talhanation.workers.config.BuildPaymentMode#CURRENCY}.
 * price = BuildingBasePrice + placed blocks / 64 * BuildingSizePrice + sum(base items / 64 * material price)
 * Materials are the BASE items from {@link BuildBlockParse} (1 block = 1 base item), same as the builder consumes.
 */
public class BuildingPrice {
    private static List<String> lastPriceConfig;
    private static final Map<Item, Double> ITEM_PRICES = new HashMap<>();
    private static final Map<TagKey<Item>, Double> TAG_PRICES = new LinkedHashMap<>();
    private static List<String> lastBlockedConfig;
    private static final Set<Item> BLOCKED_ITEMS = new HashSet<>();
    private static final Set<TagKey<Item>> BLOCKED_TAGS = new HashSet<>();

    /**
     * @return the price in currency, or -1 if the materials contain a blocked item.
     */
    public static int calculate(Map<Item, Integer> materials) {
        if (findBlocked(materials) != null) return -1;

        int blocks = 0;
        double materialPrice = 0;
        for (Map.Entry<Item, Integer> entry : materials.entrySet()) {
            blocks += entry.getValue();
            materialPrice += entry.getValue() * getPricePerStack(entry.getKey()) / 64D;
        }
        if (blocks == 0) return 0;

        double sizePrice = blocks * WorkersServerConfig.BuildingSizePrice.get() / 64D;
        return WorkersServerConfig.BuildingBasePrice.get() + (int) Math.ceil(sizePrice + materialPrice);
    }

    @Nullable
    public static Item findBlocked(Map<Item, Integer> materials) {
        for (Item item : materials.keySet()) {
            if (isBlocked(item)) return item;
        }
        return null;
    }

    public static boolean isBlocked(Item item) {
        reloadBlocked();
        if (BLOCKED_ITEMS.contains(item)) return true;

        ItemStack stack = new ItemStack(item);
        for (TagKey<Item> tag : BLOCKED_TAGS) {
            if (stack.is(tag)) return true;
        }
        return false;
    }

    public static double getPricePerStack(Item item) {
        reloadPrices();
        Double price = ITEM_PRICES.get(item);

        if (price == null) {
            ItemStack stack = new ItemStack(item);
            for (Map.Entry<TagKey<Item>, Double> entry : TAG_PRICES.entrySet()) {
                if (stack.is(entry.getKey())) {
                    price = entry.getValue();
                    break;
                }
            }
        }

        if (price == null) price = WorkersServerConfig.BuildingDefaultMaterialPrice.get();

        //The currency itself must never be cheaper than itself, otherwise build -> break would print money
        if (item == FactionEvents.getCurrency().getItem()) price = Math.max(price, 64D);

        return price;
    }

    private static void reloadPrices() {
        List<String> config = WorkersServerConfig.BuildingMaterialPrices.get();
        if (config.equals(lastPriceConfig)) return;
        lastPriceConfig = new ArrayList<>(config);

        ITEM_PRICES.clear();
        TAG_PRICES.clear();
        for (String entry : config) {
            String[] split = entry.split("=");
            if (split.length != 2) {
                WorkersMain.LOGGER.warn("Workers: invalid BuildingMaterialPrices entry: {}", entry);
                continue;
            }

            double price;
            try {
                price = Double.parseDouble(split[1].trim());
            } catch (NumberFormatException e) {
                WorkersMain.LOGGER.warn("Workers: invalid price in BuildingMaterialPrices entry: {}", entry);
                continue;
            }

            String id = split[0].trim();
            if (id.startsWith("#")) {
                TagKey<Item> tag = parseTag(id);
                if (tag != null) TAG_PRICES.putIfAbsent(tag, price);
            } else {
                Item item = parseItem(id);
                if (item != null) ITEM_PRICES.putIfAbsent(item, price);
            }
        }
    }

    private static void reloadBlocked() {
        List<String> config = WorkersServerConfig.BuildingBlockedMaterials.get();
        if (config.equals(lastBlockedConfig)) return;
        lastBlockedConfig = new ArrayList<>(config);

        BLOCKED_ITEMS.clear();
        BLOCKED_TAGS.clear();
        for (String entry : config) {
            String id = entry.trim();
            if (id.startsWith("#")) {
                TagKey<Item> tag = parseTag(id);
                if (tag != null) BLOCKED_TAGS.add(tag);
            } else {
                Item item = parseItem(id);
                if (item != null) BLOCKED_ITEMS.add(item);
            }
        }
    }

    @Nullable
    private static TagKey<Item> parseTag(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id.substring(1));
        if (location == null) {
            WorkersMain.LOGGER.warn("Workers: invalid item tag in building config: {}", id);
            return null;
        }
        return TagKey.create(Registries.ITEM, location);
    }

    @Nullable
    private static Item parseItem(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null || !BuiltInRegistries.ITEM.containsKey(location)) return null; // modded item not installed
        Item item = BuiltInRegistries.ITEM.get(location);
        return item == Items.AIR ? null : item;
    }
}
