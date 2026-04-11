package com.talhanation.workers.world;

import com.talhanation.recruits.world.RecruitsRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Courier-specific route data. Wraps a {@link RecruitsRoute} reference (by UUID)
 * with per-waypoint display names and an ordered list of up to 8 {@link CourierAction}s.
 * <p>
 * This is the single source of truth for all route information, both on the server
 * (stored in the entity) and on the client (decoded from synced entity data).
 */
public class CourierRoute {

    @Nullable private final UUID       routeId;
    private final List<CourierWaypoint> waypoints;

    private CourierRoute(@Nullable UUID routeId, List<CourierWaypoint> waypoints) {
        this.routeId   = routeId;
        this.waypoints = waypoints;
    }

    // ── Factories ──────────────────────────────────────────────────────────────

    /**
     * Creates a fresh {@code CourierRoute} from a {@link RecruitsRoute}, taking the
     * waypoint positions and the route's own names, with empty action lists.
     * Used by the GUI when the player selects a new route.
     */
    public static CourierRoute fromRecruitsRoute(RecruitsRoute route) {
        List<CourierWaypoint> wps = new ArrayList<>();
        for (RecruitsRoute.Waypoint wp : route.getWaypoints()) {
            wps.add(new CourierWaypoint(wp.getPosition(), wp.getName(), new ArrayList<>()));
        }
        return new CourierRoute(route.getId(), wps);
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    @Nullable
    public UUID getRouteId() {
        return routeId;
    }

    public List<CourierWaypoint> getWaypoints() {
        return waypoints;
    }

    public boolean isEmpty() {
        return waypoints.isEmpty();
    }

    public int size() {
        return waypoints.size();
    }

    // ── NBT ────────────────────────────────────────────────────────────────────

    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        if (routeId != null) nbt.putUUID("RouteId", routeId);

        ListTag waypointList = new ListTag();
        for (CourierWaypoint wp : waypoints) {
            waypointList.add(wp.toNBT());
        }
        nbt.put("Waypoints", waypointList);
        return nbt;
    }

    @Nullable
    public static CourierRoute fromNBT(CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty()) return null;

        UUID routeId = nbt.hasUUID("RouteId") ? nbt.getUUID("RouteId") : null;

        List<CourierWaypoint> wps = new ArrayList<>();
        ListTag waypointList = nbt.getList("Waypoints", Tag.TAG_COMPOUND);
        for (int i = 0; i < waypointList.size(); i++) {
            CourierWaypoint wp = CourierWaypoint.fromNBT(waypointList.getCompound(i));
            if (wp != null) wps.add(wp);
        }

        if (wps.isEmpty() && routeId == null) return null;
        return new CourierRoute(routeId, wps);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Nested class
    // ═════════════════════════════════════════════════════════════════════════

    public static class CourierWaypoint {

        private final BlockPos             position;
        public        String               displayName;
        public final  List<CourierAction>  actions;      // max 8, never null

        public CourierWaypoint(BlockPos position, String displayName, List<CourierAction> actions) {
            this.position    = position;
            this.displayName = displayName;
            this.actions     = new ArrayList<>(actions);
        }

        public BlockPos getPosition() {
            return position;
        }

        public CompoundTag toNBT() {
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("X", position.getX());
            nbt.putInt("Y", position.getY());
            nbt.putInt("Z", position.getZ());
            nbt.putString("DisplayName", displayName);

            ListTag actionList = new ListTag();
            for (CourierAction action : actions) {
                actionList.add(action.toNBT());
            }
            nbt.put("Actions", actionList);
            return nbt;
        }

        @Nullable
        public static CourierWaypoint fromNBT(CompoundTag nbt) {
            if (nbt == null || nbt.isEmpty()) return null;

            BlockPos pos  = new BlockPos(nbt.getInt("X"), nbt.getInt("Y"), nbt.getInt("Z"));
            String   name = nbt.getString("DisplayName");

            List<CourierAction> actions = new ArrayList<>();
            ListTag actionList = nbt.getList("Actions", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(actionList.size(), 8); i++) {
                CourierAction action = CourierAction.fromNBT(actionList.getCompound(i));
                if (action != null) actions.add(action);
            }

            return new CourierWaypoint(pos, name, actions);
        }
    }
}
