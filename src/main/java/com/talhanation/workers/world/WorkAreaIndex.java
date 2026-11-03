package com.talhanation.workers.world;

import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Server-side index of all loaded work areas, by chunk and by UUID.
 * <p>
 * Workers and the Governor ask "which work areas are around here / in this claim" very often. A
 * {@code getEntitiesOfClass} over 64 blocks walks every entity section in that box (items, mobs, ...),
 * the index only holds work areas and answers by chunk. Work areas register themselves when they are
 * added to or removed from the level, and move their entry when they change chunk.
 */
public final class WorkAreaIndex {

    private static final Map<ResourceKey<Level>, WorkAreaIndex> INDICES = new HashMap<>();

    private final Long2ObjectOpenHashMap<List<AbstractWorkAreaEntity>> byChunk = new Long2ObjectOpenHashMap<>();
    private final Map<UUID, AbstractWorkAreaEntity> byId = new HashMap<>();

    private WorkAreaIndex() {
    }

    public static WorkAreaIndex get(ServerLevel level) {
        return INDICES.computeIfAbsent(level.dimension(), key -> new WorkAreaIndex());
    }

    /** Server stopped: forget everything, the next server builds the index again from the loaded areas. */
    public static void clearAll() {
        INDICES.clear();
    }

    // ------------------------------------------------------------------ registration (called by the work areas)

    public static void add(AbstractWorkAreaEntity area, long chunk) {
        if (!(area.level() instanceof ServerLevel level)) return;
        WorkAreaIndex index = get(level);
        index.byId.put(area.getUUID(), area);
        List<AbstractWorkAreaEntity> list = index.byChunk.computeIfAbsent(chunk, c -> new ArrayList<>(2));
        if (!list.contains(area)) list.add(area);
    }

    public static void remove(AbstractWorkAreaEntity area, long chunk) {
        if (!(area.level() instanceof ServerLevel level)) return;
        WorkAreaIndex index = get(level);
        index.byId.remove(area.getUUID(), area);
        List<AbstractWorkAreaEntity> list = index.byChunk.get(chunk);
        if (list != null) {
            list.remove(area);
            if (list.isEmpty()) index.byChunk.remove(chunk);
        }
    }

    public static void move(AbstractWorkAreaEntity area, long from, long to) {
        remove(area, from);
        add(area, to);
    }

    // ------------------------------------------------------------------ queries

    @Nullable
    public AbstractWorkAreaEntity get(UUID uuid) {
        AbstractWorkAreaEntity area = byId.get(uuid);
        return area == null || area.isRemoved() ? null : area;
    }

    @Nullable
    public <T extends AbstractWorkAreaEntity> T get(UUID uuid, Class<T> type) {
        AbstractWorkAreaEntity area = get(uuid);
        return type.isInstance(area) ? type.cast(area) : null;
    }

    /** Work areas of the type whose position is within the range (square, like an inflated bounding box). */
    public <T extends AbstractWorkAreaEntity> List<T> inRange(Class<T> type, BlockPos center, int range) {
        List<T> result = new ArrayList<>();
        int minX = (center.getX() - range) >> 4, maxX = (center.getX() + range) >> 4;
        int minZ = (center.getZ() - range) >> 4, maxZ = (center.getZ() + range) >> 4;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                List<AbstractWorkAreaEntity> list = byChunk.get(ChunkPos.asLong(cx, cz));
                if (list == null) continue;
                for (AbstractWorkAreaEntity area : list) {
                    if (!type.isInstance(area) || area.isRemoved()) continue;
                    BlockPos pos = area.blockPosition();
                    if (Math.abs(pos.getX() - center.getX()) > range || Math.abs(pos.getZ() - center.getZ()) > range
                            || Math.abs(pos.getY() - center.getY()) > range) continue;
                    result.add(type.cast(area));
                }
            }
        }
        return result;
    }

    /** Work areas of the type in the given chunks, e.g. all chunks of a claim. */
    public <T extends AbstractWorkAreaEntity> List<T> inChunks(Class<T> type, Collection<ChunkPos> chunks) {
        List<T> result = new ArrayList<>();
        for (ChunkPos chunk : chunks) {
            List<AbstractWorkAreaEntity> list = byChunk.get(chunk.toLong());
            if (list == null) continue;
            for (AbstractWorkAreaEntity area : list) {
                if (type.isInstance(area) && !area.isRemoved()) result.add(type.cast(area));
            }
        }
        return result;
    }
}
