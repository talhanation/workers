package com.talhanation.workers.world;

import com.talhanation.workers.entities.workarea.MarketArea;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Where the markets are, also in unloaded chunks. Only positions and settings, never the trades:
 * what a market buys or sells every merchant has to find out himself ({@link MerchantMemory}).
 */
public class MarketDirectory extends SavedData {
    private static final String DATA_NAME = "workers_market_directory";

    private final Map<UUID, Entry> markets = new HashMap<>();

    public static MarketDirectory get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(MarketDirectory::load, MarketDirectory::new, DATA_NAME);
    }

    /** Called by the market regularly and on changes. */
    public static void update(ServerLevel level, MarketArea market) {
        MarketDirectory data = get(level.getServer());
        Entry entry = new Entry(market.getUUID(), market.getMarketName(), market.blockPosition(), level.dimension().location().toString(),
                market.getPlayerUUID(), market.getPlayerName(), market.getTeamStringID(), market.getAcceptForeignMerchants(), market.assignedMerchantUUID);
        Entry old = data.markets.put(entry.uuid, entry);
        if (!entry.equals(old)) data.setDirty();
    }

    public static void remove(ServerLevel level, UUID market) {
        MarketDirectory data = get(level.getServer());
        if (data.markets.remove(market) != null) data.setDirty();
    }

    public static Collection<Entry> getMarkets(MinecraftServer server) {
        return get(server).markets.values();
    }

    @Nullable
    public static Entry getMarket(MinecraftServer server, UUID market) {
        return get(server).markets.get(market);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry entry : markets.values()) list.add(entry.toNbt());
        tag.put("Markets", list);
        return tag;
    }

    public static MarketDirectory load(CompoundTag tag) {
        MarketDirectory data = new MarketDirectory();
        ListTag list = tag.getList("Markets", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Entry entry = Entry.fromNbt(list.getCompound(i));
            if (entry != null) data.markets.put(entry.uuid, entry);
        }
        return data;
    }

    public record Entry(UUID uuid, String name, BlockPos pos, String dimension, @Nullable UUID owner, String ownerName,
                        String team, boolean acceptForeign, @Nullable UUID merchant) {

        CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("UUID", uuid);
            tag.putString("Name", name);
            tag.putLong("Pos", pos.asLong());
            tag.putString("Dimension", dimension);
            if (owner != null) tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName == null ? "" : ownerName);
            tag.putString("Team", team == null ? "" : team);
            tag.putBoolean("AcceptForeign", acceptForeign);
            if (merchant != null) tag.putUUID("Merchant", merchant);
            return tag;
        }

        @Nullable
        static Entry fromNbt(CompoundTag tag) {
            if (!tag.hasUUID("UUID")) return null;
            return new Entry(tag.getUUID("UUID"), tag.getString("Name"), BlockPos.of(tag.getLong("Pos")), tag.getString("Dimension"),
                    tag.hasUUID("Owner") ? tag.getUUID("Owner") : null, tag.getString("OwnerName"), tag.getString("Team"),
                    tag.getBoolean("AcceptForeign"), tag.hasUUID("Merchant") ? tag.getUUID("Merchant") : null);
        }
    }
}
