package com.talhanation.workers.network;

import net.minecraft.core.HolderLookup;
import com.talhanation.workers.entities.CourierEntity;
import com.talhanation.workers.world.CourierRoute;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

public class MessageCourierSetRoute implements Message<MessageCourierSetRoute> {

    private UUID        courierUuid;
    private boolean     hasRoute;
    private CompoundTag routeNBT;
    private boolean     useVehicleInventory;
    private boolean     shouldCycle;
    private boolean     start;
    public MessageCourierSetRoute() {}

    public MessageCourierSetRoute(UUID courierUuid, CourierRoute routeNBT, boolean useVehicleInventory, boolean shouldCycle, boolean start, HolderLookup.Provider provider) {
        this.courierUuid = courierUuid;
        this.hasRoute = routeNBT != null;
        if(hasRoute){
            this.routeNBT = routeNBT.toNBT(provider);
        }

        this.useVehicleInventory  = useVehicleInventory;
        this.shouldCycle  = shouldCycle;
        this.start  = start;
    }

    public MessageCourierSetRoute(UUID courierUuid) {
        this.courierUuid = courierUuid;
        this.hasRoute    = false;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());
        if (player == null) return;

        player.getCommandSenderWorld()
                .getEntitiesOfClass(CourierEntity.class,
                        player.getBoundingBox().inflate(32.0D),
                        c -> c.getUUID().equals(this.courierUuid) && c.isAlive())
                .forEach(courier -> {
                    // Only the owner may reprogram the courier.
                    if (!courier.isOwned() || !player.getUUID().equals(courier.getOwnerUUID())) return;

                    courier.useVehicleInventory = this.useVehicleInventory;

                    // Always buffer the requested value. If the courier is safely at
                    // the start of its route (waypoint 0, not travelling) apply it
                    // right away; otherwise it takes effect when it next reaches
                    // waypoint 0 (see CourierWorkGoal.tickExecute). This keeps a
                    // mid-travel change safe WITHOUT the old bug where turning cycle
                    // OFF left pendingShouldCycle=true and flipped it back on at wp 0.
                    courier.pendingShouldCycle = this.shouldCycle;
                    boolean safeAtStart = courier.currentWaypointIndex == 0 && !courier.returning;
                    if (safeAtStart){
                        courier.shouldCycle = this.shouldCycle;
                    }

                    if (!hasRoute) {
                        courier.clearRoute();
                        return;
                    }
                    if (routeNBT == null) {
                        courier.clearRoute();
                        return;
                    }
                    CourierRoute route = CourierRoute.fromNBT(routeNBT, player.registryAccess());
                    if (this.start) {

                        courier.loadRouteFromNearestWaypoint(route);
                        courier.setFollowState(6);
                    }
                    else if (!courier.updateRouteKeepProgress(route)) {
                        // Waypoints changed (or a different route) → restart from the first waypoint.
                        // Same waypoints → only the actions changed, the courier keeps its progress.
                        courier.loadRoute(route);
                    }
                });
    }

    @Override
    public MessageCourierSetRoute fromBytes(RegistryFriendlyByteBuf buf) {
        this.courierUuid         = buf.readUUID();
        this.useVehicleInventory = buf.readBoolean();
        this.shouldCycle         = buf.readBoolean();
        this.hasRoute            = buf.readBoolean();
        if (this.hasRoute) {
            byte[] compressed = buf.readByteArray();
            try {
                this.routeNBT = NbtIo.readCompressed(new ByteArrayInputStream(compressed), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            } catch (IOException e) {
                e.printStackTrace();
                this.routeNBT = new CompoundTag();
            }
        }
        this.start = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(courierUuid);
        buf.writeBoolean(useVehicleInventory);
        buf.writeBoolean(shouldCycle);
        buf.writeBoolean(hasRoute);

        if(hasRoute && routeNBT != null){
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                NbtIo.writeCompressed(routeNBT, out);
                byte[] compressed = out.toByteArray();
                buf.writeByteArray(compressed);
            } catch (IOException e) {
                e.printStackTrace();
                buf.writeByteArray(new byte[0]);
            }
        }

        buf.writeBoolean(start);
    }

    public static final CustomPacketPayload.Type<MessageCourierSetRoute> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_courier_set_route"));

    @Override
    public CustomPacketPayload.Type<MessageCourierSetRoute> type() {
        return TYPE;
    }
}