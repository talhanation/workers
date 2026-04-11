package com.talhanation.workers.network;

import com.talhanation.workers.entities.CourierEntity;
import com.talhanation.workers.world.CourierRoute;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Client → Server: assigns or clears a route on a {@link CourierEntity}.
 * <p>
 * Routes are stored client-side as {@link com.talhanation.recruits.world.RecruitsRoute}
 * files, so the full {@link CourierRoute} (positions, names, actions) must be transmitted
 * — the server cannot read the client's file system.
 */
public class MessageCourierSetRoute implements Message<MessageCourierSetRoute> {

    private UUID         courierUuid;
    private boolean      hasRoute;
    private CompoundTag  routeNbt;   // CourierRoute.toNBT(), or empty if clearing

    public MessageCourierSetRoute() {}

    /** Assign a route. */
    public MessageCourierSetRoute(UUID courierUuid, CourierRoute route) {
        this.courierUuid = courierUuid;
        this.hasRoute    = route != null;
        this.routeNbt    = route != null ? route.toNBT() : new CompoundTag();
    }

    /** Clear the route. */
    public MessageCourierSetRoute(UUID courierUuid) {
        this.courierUuid = courierUuid;
        this.hasRoute    = false;
        this.routeNbt    = new CompoundTag();
    }

    // ── Message contract ───────────────────────────────────────────────────────

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;

        // Widen the search box somewhat — the player may have a cursor lag offset.
        AABB searchBox = player.getBoundingBox().inflate(32.0D);

        player.getCommandSenderWorld()
                .getEntitiesOfClass(CourierEntity.class, searchBox,
                        c -> c.getUUID().equals(this.courierUuid) && c.isAlive())
                .forEach(courier -> {
                    if (!hasRoute) {
                        courier.clearRoute();
                        return;
                    }

                    @Nullable CourierRoute route = CourierRoute.fromNBT(routeNbt);
                    if (route == null) {
                        courier.clearRoute();
                        return;
                    }

                    courier.loadRoute(route);
                    courier.setFollowState(6); // put the courier into working mode
                });
    }

    // ── Serialisation ──────────────────────────────────────────────────────────

    @Override
    public MessageCourierSetRoute fromBytes(FriendlyByteBuf buf) {
        this.courierUuid = buf.readUUID();
        this.hasRoute    = buf.readBoolean();
        this.routeNbt    = buf.readBoolean() ? buf.readNbt() : new CompoundTag();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(courierUuid);
        buf.writeBoolean(hasRoute);
        boolean hasNbt = routeNbt != null && !routeNbt.isEmpty();
        buf.writeBoolean(hasNbt);
        if (hasNbt) buf.writeNbt(routeNbt);
    }
}
