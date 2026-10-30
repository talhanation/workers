package com.talhanation.workers.world;

import com.talhanation.recruits.entities.AbstractChunkLoaderEntity;
import com.talhanation.workers.WorkersMain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Takes entities out of the world for a journey and puts them back near the target after the travel time.
 * Works for any entity (a leader and its companions, mounts with their riders). What the entities want to do
 * after arriving is stored in their own NBT, this class only moves them.
 * Forerunner of a travel system that will move to recruits later.
 */
public class WorkersRemoteTravel extends SavedData {
    private static final String DATA_NAME = "workers_remote_travel";
    /** Recruits forces the 3x3 chunks around a chunk loader entity with this mod id. */
    private static final String RECRUITS_MOD_ID = "recruits";
    /** Blocks per second while travelling. */
    public static final double TRAVEL_SPEED = 7.0D;

    private final List<Trip> trips = new ArrayList<>();

    public static WorkersRemoteTravel get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(WorkersRemoteTravel::load, WorkersRemoteTravel::new, DATA_NAME);
    }

    /** Travel time in ticks for the distance in blocks. */
    public static long getTravelTicks(double distance) {
        return Math.max(20L, (long) Math.ceil(distance / TRAVEL_SPEED * 20D));
    }

    /**
     * Takes the leader and its companions (with mounts and riders) out of the world.
     * They are put back near the target after travelTicks.
     * @return false if nothing could be saved, then nobody was removed
     */
    public static boolean depart(ServerLevel level, Entity leader, List<? extends Entity> companions, BlockPos target, int spread, long travelTicks) {
        List<Entity> roots = new ArrayList<>();
        roots.add(leader.getRootVehicle());
        for (Entity companion : companions) {
            Entity root = companion.getRootVehicle();
            if (!roots.contains(root) && root.isAlive()) roots.add(root);
        }

        List<CompoundTag> saved = new ArrayList<>();
        List<Entity> savedRoots = new ArrayList<>();
        for (Entity root : roots) {
            CompoundTag tag = new CompoundTag();
            if (root.saveAsPassenger(tag)) {
                saved.add(tag);
                savedRoots.add(root);
            }
        }
        if (saved.isEmpty() || savedRoots.get(0) != leader.getRootVehicle()) return false;

        for (Entity root : savedRoots) {
            List<Entity> all = new ArrayList<>();
            root.getIndirectPassengers().forEach(all::add);
            all.add(root);
            for (Entity entity : all) {
                releaseForcedChunks(level, entity);
                entity.remove(Entity.RemovalReason.CHANGED_DIMENSION);
            }
        }

        Trip trip = new Trip(leader.getUUID(), level.dimension().location(), target, spread,
                level.getServer().overworld().getGameTime() + travelTicks, saved);
        WorkersRemoteTravel data = get(level.getServer());
        data.trips.add(trip);
        data.setDirty();
        return true;
    }

    public static boolean isTravelling(MinecraftServer server, UUID leader) {
        return get(server).findTrip(leader) != null;
    }

    /** Game time (overworld) of the arrival, or -1 if the entity is not travelling. */
    public static long getArrival(MinecraftServer server, UUID leader) {
        Trip trip = get(server).findTrip(leader);
        return trip == null ? -1 : trip.arrival;
    }

    @Nullable
    private Trip findTrip(UUID leader) {
        for (Trip trip : trips) {
            if (trip.leader.equals(leader)) return trip;
        }
        return null;
    }

    /** Recruits keeps the chunks of a chunk loader forced, they are released while it is away. */
    private static void releaseForcedChunks(ServerLevel level, Entity entity) {
        if (!(entity instanceof AbstractChunkLoaderEntity)) return;
        ChunkPos center = entity.chunkPosition();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                ForgeChunkManager.forceChunk(level, RECRUITS_MOD_ID, entity, center.x + x, center.z + z, false, false);
            }
        }
    }

    // ── Arrival ───────────────────────────────────────────────────────────────

    private void tick(MinecraftServer server) {
        if (trips.isEmpty()) return;
        long now = server.overworld().getGameTime();
        Iterator<Trip> iterator = trips.iterator();
        while (iterator.hasNext()) {
            Trip trip = iterator.next();
            if (trip.arrival > now) continue;
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, trip.dimension));
            if (level == null) continue; // dimension not loaded, try again later
            arrive(level, trip);
            iterator.remove();
            setDirty();
        }
    }

    private static void arrive(ServerLevel level, Trip trip) {
        RandomSource random = level.getRandom();
        BlockPos spawn = findSpawnPos(level, trip.target, trip.spread, random);

        // the chunk has to tick until the chunk loader of the leader takes over
        ChunkPos chunk = new ChunkPos(spawn);
        level.getChunkSource().addRegionTicket(TicketType.PORTAL, chunk, 3, spawn);
        level.getChunk(chunk.x, chunk.z);

        for (int i = 0; i < trip.entities.size(); i++) {
            BlockPos pos = i == 0 ? spawn : findSpawnPos(level, spawn, 3, random);
            Entity entity = EntityType.loadEntityRecursive(trip.entities.get(i), level, e -> {
                e.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, e.getYRot(), e.getXRot());
                return e;
            });
            // an entity that already exists (e.g. after a crash) is not added twice
            if (entity == null || !level.tryAddFreshEntityWithPassengers(entity)) {
                WorkersMain.LOGGER.warn("Workers: could not put travelling entity back into the world at {}", pos);
            }
        }
    }

    /** A free spot on the ground around the center, the center itself if none is found. */
    public static BlockPos findSpawnPos(ServerLevel level, BlockPos center, int spread, RandomSource random) {
        for (int i = 0; i < 10 && spread > 0; i++) {
            int x = center.getX() + random.nextInt(spread * 2 + 1) - spread;
            int z = center.getZ() + random.nextInt(spread * 2 + 1) - spread;
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (isFree(level, pos)) return pos;
        }
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center);
    }

    private static boolean isFree(Level level, BlockPos pos) {
        if (!level.getBlockState(pos.below()).isSolid()) return false;
        if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.below()).isEmpty()) return false;
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }

    // ── Save data ─────────────────────────────────────────────────────────────

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Trip trip : trips) list.add(trip.toNbt());
        tag.put("Trips", list);
        return tag;
    }

    public static WorkersRemoteTravel load(CompoundTag tag) {
        WorkersRemoteTravel data = new WorkersRemoteTravel();
        ListTag list = tag.getList("Trips", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Trip trip = Trip.fromNbt(list.getCompound(i));
            if (trip != null) data.trips.add(trip);
        }
        return data;
    }

    private static class Trip {
        final UUID leader;
        final ResourceLocation dimension;
        final BlockPos target;
        final int spread;
        final long arrival;
        /** The leader first, then the companions. Each tag is a root vehicle with its passengers. */
        final List<CompoundTag> entities;

        Trip(UUID leader, ResourceLocation dimension, BlockPos target, int spread, long arrival, List<CompoundTag> entities) {
            this.leader = leader;
            this.dimension = dimension;
            this.target = target;
            this.spread = spread;
            this.arrival = arrival;
            this.entities = entities;
        }

        CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Leader", leader);
            tag.putString("Dimension", dimension.toString());
            tag.putLong("Target", target.asLong());
            tag.putInt("Spread", spread);
            tag.putLong("Arrival", arrival);
            ListTag list = new ListTag();
            list.addAll(entities);
            tag.put("Entities", list);
            return tag;
        }

        @Nullable
        static Trip fromNbt(CompoundTag tag) {
            ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
            if (!tag.hasUUID("Leader") || dimension == null) return null;
            List<CompoundTag> entities = new ArrayList<>();
            ListTag list = tag.getList("Entities", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) entities.add(list.getCompound(i));
            return new Trip(tag.getUUID("Leader"), dimension, BlockPos.of(tag.getLong("Target")), tag.getInt("Spread"), tag.getLong("Arrival"), entities);
        }
    }

    /** Puts arrived travellers back into the world. Registered on the forge event bus. */
    public static class Events {
        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            MinecraftServer server = event.getServer();
            if (server == null || server.getTickCount() % 20 != 0) return;
            get(server).tick(server);
        }
    }
}
