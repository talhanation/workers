package com.talhanation.workers.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.*;

/**
 * A trading trip of a travelling merchant. Stored in the merchant NBT, so it survives being taken out of
 * the world by {@link WorkersRemoteTravel} and server restarts.
 */
public class MerchantTrip {
    public Phase phase = Phase.PACK;
    public UUID homeMarket;
    public BlockPos homePos;
    @Nullable public UUID targetMarket;
    public BlockPos targetPos = BlockPos.ZERO;
    public String targetName = "";
    /** Game time when walking towards the target started, 0 = not yet. After one minute he is teleported in. */
    public long walkStart;
    /** Game time until the merchant stays at the market. */
    public long stayUntil;
    public final Set<UUID> visited = new HashSet<>();
    /** Villager buy trades: how many items may still be bought on this trip. */
    public final Map<UUID, Integer> buyLeft = new HashMap<>();
    // report
    public int currencyPacked;
    public final Map<String, Integer> sold = new LinkedHashMap<>();
    public final Map<String, Integer> bought = new LinkedHashMap<>();

    public MerchantTrip(UUID homeMarket, BlockPos homePos) {
        this.homeMarket = homeMarket;
        this.homePos = homePos;
    }

    public void setTarget(UUID market, BlockPos pos, String name) {
        this.targetMarket = market;
        this.targetPos = pos;
        this.targetName = name;
        this.walkStart = 0;
        this.stayUntil = 0;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Phase", phase.name());
        tag.putUUID("HomeMarket", homeMarket);
        tag.putLong("HomePos", homePos.asLong());
        if (targetMarket != null) tag.putUUID("TargetMarket", targetMarket);
        tag.putLong("TargetPos", targetPos.asLong());
        tag.putString("TargetName", targetName);
        tag.putLong("WalkStart", walkStart);
        tag.putLong("StayUntil", stayUntil);

        ListTag visitedList = new ListTag();
        for (UUID uuid : visited) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("UUID", uuid);
            visitedList.add(entry);
        }
        tag.put("Visited", visitedList);

        ListTag buyList = new ListTag();
        for (Map.Entry<UUID, Integer> entry : buyLeft.entrySet()) {
            CompoundTag buy = new CompoundTag();
            buy.putUUID("Trade", entry.getKey());
            buy.putInt("Left", entry.getValue());
            buyList.add(buy);
        }
        tag.put("BuyLeft", buyList);

        tag.putInt("CurrencyPacked", currencyPacked);
        tag.put("Sold", countsToNbt(sold));
        tag.put("Bought", countsToNbt(bought));
        return tag;
    }

    @Nullable
    public static MerchantTrip fromNbt(CompoundTag tag) {
        if (!tag.hasUUID("HomeMarket")) return null;
        MerchantTrip trip = new MerchantTrip(tag.getUUID("HomeMarket"), BlockPos.of(tag.getLong("HomePos")));
        trip.phase = Phase.fromName(tag.getString("Phase"));
        trip.targetMarket = tag.hasUUID("TargetMarket") ? tag.getUUID("TargetMarket") : null;
        trip.targetPos = BlockPos.of(tag.getLong("TargetPos"));
        trip.targetName = tag.getString("TargetName");
        trip.walkStart = tag.getLong("WalkStart");
        trip.stayUntil = tag.getLong("StayUntil");

        ListTag visitedList = tag.getList("Visited", Tag.TAG_COMPOUND);
        for (int i = 0; i < visitedList.size(); i++) trip.visited.add(visitedList.getCompound(i).getUUID("UUID"));

        ListTag buyList = tag.getList("BuyLeft", Tag.TAG_COMPOUND);
        for (int i = 0; i < buyList.size(); i++) {
            CompoundTag buy = buyList.getCompound(i);
            trip.buyLeft.put(buy.getUUID("Trade"), buy.getInt("Left"));
        }

        trip.currencyPacked = tag.getInt("CurrencyPacked");
        countsFromNbt(tag.getCompound("Sold"), trip.sold);
        countsFromNbt(tag.getCompound("Bought"), trip.bought);
        return trip;
    }

    private static CompoundTag countsToNbt(Map<String, Integer> counts) {
        CompoundTag tag = new CompoundTag();
        counts.forEach(tag::putInt);
        return tag;
    }

    private static void countsFromNbt(CompoundTag tag, Map<String, Integer> out) {
        for (String key : tag.getAllKeys()) out.put(key, tag.getInt(key));
    }

    public enum Phase {
        /** Takes goods and currency from the home market. */
        PACK,
        /** On the way to the target market (walking or travelling). */
        TO_MARKET,
        /** Trades at the target market. */
        AT_MARKET,
        /** On the way home. */
        RETURN,
        /** Puts everything back into the home market. */
        UNPACK;

        public static Phase fromName(String name) {
            for (Phase phase : values()) {
                if (phase.name().equals(name)) return phase;
            }
            return RETURN;
        }
    }
}
