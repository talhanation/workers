package com.talhanation.workers.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorkersMerchantTrade {
    public int maxTrades;
    public int currentTrades;
    public ItemStack currencyItem;
    public boolean allowDamagedCurrency;
    public boolean enabled;
    public ItemStack tradeItem;
    public UUID uuid;

    public WorkersMerchantTrade copy(){
        WorkersMerchantTrade t = new WorkersMerchantTrade(this.currencyItem, this.tradeItem, this.maxTrades, this.allowDamagedCurrency);
        t.enabled = this.enabled;
        return t;
    }

    public WorkersMerchantTrade(){
        this(ItemStack.EMPTY, ItemStack.EMPTY, 16, false);
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
}
