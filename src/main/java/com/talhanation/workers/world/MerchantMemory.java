package com.talhanation.workers.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import java.util.*;

/**
 * What a travelling merchant remembers about the markets he visited: which items they buy and sell.
 * Lives on the merchant (NBT), dies with him. Overwritten on every visit, old knowledge expires.
 */
public class MerchantMemory {
    /** Knowledge older than this is treated like an unknown market. */
    public static final long STALE_TICKS = 24000L * 3;
    /** Entries without favorite that were not visited for this long are forgotten. */
    public static final long FORGET_TICKS = 24000L * 10;

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();

    @Nullable
    public Entry get(UUID market) {
        return entries.get(market);
    }

    public Collection<Entry> getEntries() {
        return entries.values();
    }

    /** Remembers what the market buys and sells right now. */
    public Entry learn(UUID market, String name, BlockPos pos, String dimension, String ownerName, Collection<String> buys, Collection<String> sells, long gameTime) {
        Entry entry = entries.computeIfAbsent(market, Entry::new);
        entry.name = name;
        entry.pos = pos;
        entry.dimension = dimension;
        entry.ownerName = ownerName;
        entry.buys.clear();
        entry.buys.addAll(buys);
        entry.sells.clear();
        entry.sells.addAll(sells);
        entry.lastVisit = gameTime;
        return entry;
    }

    public void forgetOld(long gameTime) {
        entries.values().removeIf(entry -> !entry.favorite && !entry.avoid && gameTime - entry.lastVisit > FORGET_TICKS);
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Entry entry : entries.values()) list.add(entry.toNbt());
        tag.put("Markets", list);
        return tag;
    }

    public static MerchantMemory fromNbt(CompoundTag tag) {
        MerchantMemory memory = new MerchantMemory();
        ListTag list = tag.getList("Markets", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Entry entry = Entry.fromNbt(list.getCompound(i));
            if (entry != null) memory.entries.put(entry.market, entry);
        }
        return memory;
    }

    public static class Entry {
        public final UUID market;
        public String name = "";
        public BlockPos pos = BlockPos.ZERO;
        public String dimension = "";
        public String ownerName = "";
        /** Item ids the market buys / sells. */
        public final List<String> buys = new ArrayList<>();
        public final List<String> sells = new ArrayList<>();
        public long lastVisit;
        public int trades;
        public int emptyVisits;
        public boolean favorite;
        public boolean avoid;

        public Entry(UUID market) {
            this.market = market;
        }

        public boolean isStale(long gameTime) {
            return gameTime - lastVisit > STALE_TICKS;
        }

        CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Market", market);
            tag.putString("Name", name);
            tag.putLong("Pos", pos.asLong());
            tag.putString("Dimension", dimension);
            tag.putString("Owner", ownerName);
            tag.put("Buys", toList(buys));
            tag.put("Sells", toList(sells));
            tag.putLong("LastVisit", lastVisit);
            tag.putInt("Trades", trades);
            tag.putInt("EmptyVisits", emptyVisits);
            tag.putBoolean("Favorite", favorite);
            tag.putBoolean("Avoid", avoid);
            return tag;
        }

        @Nullable
        static Entry fromNbt(CompoundTag tag) {
            if (!tag.hasUUID("Market")) return null;
            Entry entry = new Entry(tag.getUUID("Market"));
            entry.name = tag.getString("Name");
            entry.pos = BlockPos.of(tag.getLong("Pos"));
            entry.dimension = tag.getString("Dimension");
            entry.ownerName = tag.getString("Owner");
            fromList(tag.getList("Buys", Tag.TAG_STRING), entry.buys);
            fromList(tag.getList("Sells", Tag.TAG_STRING), entry.sells);
            entry.lastVisit = tag.getLong("LastVisit");
            entry.trades = tag.getInt("Trades");
            entry.emptyVisits = tag.getInt("EmptyVisits");
            entry.favorite = tag.getBoolean("Favorite");
            entry.avoid = tag.getBoolean("Avoid");
            return entry;
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeUUID(market);
            buf.writeUtf(name);
            buf.writeBlockPos(pos);
            buf.writeUtf(dimension);
            buf.writeUtf(ownerName);
            buf.writeCollection(buys, FriendlyByteBuf::writeUtf);
            buf.writeCollection(sells, FriendlyByteBuf::writeUtf);
            buf.writeLong(lastVisit);
            buf.writeBoolean(favorite);
            buf.writeBoolean(avoid);
        }

        public static Entry read(FriendlyByteBuf buf) {
            Entry entry = new Entry(buf.readUUID());
            entry.name = buf.readUtf();
            entry.pos = buf.readBlockPos();
            entry.dimension = buf.readUtf();
            entry.ownerName = buf.readUtf();
            entry.buys.addAll(buf.readList(FriendlyByteBuf::readUtf));
            entry.sells.addAll(buf.readList(FriendlyByteBuf::readUtf));
            entry.lastVisit = buf.readLong();
            entry.favorite = buf.readBoolean();
            entry.avoid = buf.readBoolean();
            return entry;
        }

        private static ListTag toList(List<String> values) {
            ListTag list = new ListTag();
            for (String value : values) list.add(StringTag.valueOf(value));
            return list;
        }

        private static void fromList(ListTag list, List<String> out) {
            for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        }
    }
}
