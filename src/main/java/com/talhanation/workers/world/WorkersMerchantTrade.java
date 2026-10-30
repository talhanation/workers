package com.talhanation.workers.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorkersMerchantTrade {
    /** Sell trades sell everything by default, keeping something is set on purpose. */
    public static final int DEFAULT_KEEP_AMOUNT = 0;
    /** Villager buy trades buy one stack by default. */
    public static final int DEFAULT_BUY_AMOUNT = 64;
    public int maxTrades;
    public int currentTrades;
    public ItemStack currencyItem;
    public boolean allowDamagedCurrency;
    public boolean enabled;
    public boolean isVillagerTrade;
    public ItemStack tradeItem;
    public UUID uuid;
    /** Villager trades only: sell the trade item to villagers or buy it from them. */
    public VillagerTradeMode villagerMode;
    /** Player trades: refill the market from a nearby storage. Villager trades always use the storage of the market. */
    public boolean useStorage;
    /**
     * Player trades: amount that always stays in the storages.
     * Villager sell: sell only while market and storages hold more. Villager buy: buy only until market and storages hold this amount.
     */
    public int storageAmount;

    public WorkersMerchantTrade copy(){
        WorkersMerchantTrade t = new WorkersMerchantTrade(this.currencyItem, this.tradeItem, this.maxTrades, this.allowDamagedCurrency);
        t.enabled = this.enabled;
        t.isVillagerTrade = this.isVillagerTrade;
        t.villagerMode = this.villagerMode;
        t.useStorage = this.useStorage;
        t.storageAmount = this.storageAmount;
        return t;
    }

    public WorkersMerchantTrade(){
        this(ItemStack.EMPTY, ItemStack.EMPTY, -1, false);
    }
    public WorkersMerchantTrade(ItemStack currencyItem, ItemStack tradeItem, int maxTrades, boolean allowDamagedCurrency){
        this(UUID.randomUUID(), currencyItem, tradeItem, maxTrades, allowDamagedCurrency);
    }
    private WorkersMerchantTrade(UUID uuid, ItemStack currencyItem, ItemStack tradeItem, int maxTrades, boolean allowDamagedCurrency) {
        this.uuid = uuid;
        this.currencyItem = currencyItem;
        this.tradeItem = tradeItem;
        this.maxTrades = maxTrades;
        this.allowDamagedCurrency = allowDamagedCurrency;
        this.enabled = true;
        this.isVillagerTrade = false;
        this.villagerMode = VillagerTradeMode.SELL;
        this.useStorage = true;
        this.storageAmount = DEFAULT_KEEP_AMOUNT;
    }

    public boolean isVillagerBuy() {
        return isVillagerTrade && villagerMode == VillagerTradeMode.BUY;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("uuid", this.uuid);
        tag.put("currencyItem", this.currencyItem.serializeNBT());
        tag.put("tradeItem", this.tradeItem.serializeNBT());
        tag.putInt("maxTrades", maxTrades);
        tag.putInt("currentTrades", currentTrades);
        tag.putBoolean("allowDamagedCurrency", this.allowDamagedCurrency);
        tag.putBoolean("enabled", this.enabled);
        tag.putBoolean("isVillagerTrade", this.isVillagerTrade);
        tag.putString("villagerMode", this.villagerMode.name());
        tag.putBoolean("useStorage", this.useStorage);
        tag.putInt("storageAmount", this.storageAmount);
        return tag;
    }

    public static WorkersMerchantTrade fromNbt(CompoundTag tag) {
        ItemStack currencyItem = ItemStack.of(tag.getCompound("currencyItem"));
        ItemStack tradeItem = ItemStack.of(tag.getCompound("tradeItem"));
        UUID uuid = tag.getUUID("uuid");
        int maxTrades = tag.getInt("maxTrades");
        int currentTrades = tag.getInt("currentTrades");
        boolean allowDamaged = tag.getBoolean("allowDamagedCurrency");

        boolean enabled = !tag.contains("enabled") || tag.getBoolean("enabled");

        WorkersMerchantTrade trade = new WorkersMerchantTrade(uuid, currencyItem, tradeItem, maxTrades, allowDamaged);
        trade.currentTrades = currentTrades;
        trade.enabled = enabled;
        trade.isVillagerTrade = tag.getBoolean("isVillagerTrade");
        trade.villagerMode = VillagerTradeMode.fromName(tag.getString("villagerMode"));
        // villager trades are limited by the item amount, not by the number of trades
        if (trade.isVillagerTrade) trade.maxTrades = -1;
        if (tag.contains("useStorage")) trade.useStorage = tag.getBoolean("useStorage");
        if (tag.contains("storageAmount")) trade.storageAmount = tag.getInt("storageAmount");
        return trade;
    }
    public static CompoundTag listToNbt(List<WorkersMerchantTrade> trades) {
        CompoundTag compound = new CompoundTag();
        if (trades == null) return compound;

        ListTag list = new ListTag();
        for (WorkersMerchantTrade t : trades) {
            list.add(t.toNbt());
        }
        compound.put("Trades", list);
        return compound;
    }


    public static List<WorkersMerchantTrade> listFromNbt(CompoundTag compound) {
        List<WorkersMerchantTrade> out = new ArrayList<>();
        if (compound == null || !compound.contains("Trades", Tag.TAG_LIST)) {
            return out;
        }

        ListTag list = compound.getList("Trades", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            out.add(WorkersMerchantTrade.fromNbt(entry));
        }
        return out;
    }

    public enum VillagerTradeMode {
        SELL,
        BUY;

        public static VillagerTradeMode fromName(String name) {
            for (VillagerTradeMode mode : values()) {
                if (mode.name().equals(name)) return mode;
            }
            return SELL;
        }
    }
}
