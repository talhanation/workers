package com.talhanation.workers.network;

import com.talhanation.workers.entities.CourierEntity;
import com.talhanation.workers.world.CourierRoute;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

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

    public MessageCourierSetRoute(UUID courierUuid, CourierRoute routeNBT, boolean useVehicleInventory, boolean shouldCycle, boolean start) {
        this.courierUuid = courierUuid;
        this.hasRoute = routeNBT != null;
        if(hasRoute){
            this.routeNBT = routeNBT.toNBT();
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
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;

        player.getCommandSenderWorld()
                .getEntitiesOfClass(CourierEntity.class,
                        player.getBoundingBox().inflate(32.0D),
                        c -> c.getUUID().equals(this.courierUuid) && c.isAlive())
                .forEach(courier -> {
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
                    CourierRoute route = CourierRoute.fromNBT(routeNBT);
                    if (this.start) {

                        courier.loadRouteFromNearestWaypoint(route);
                        courier.setFollowState(6);
                    }
                    else {

                        courier.loadRoute(route);
                    }
                });
    }

    @Override
    public MessageCourierSetRoute fromBytes(FriendlyByteBuf buf) {
        this.courierUuid         = buf.readUUID();
        this.useVehicleInventory = buf.readBoolean();
        this.shouldCycle         = buf.readBoolean();
        this.hasRoute            = buf.readBoolean();
        if (this.hasRoute) {
            byte[] compressed = buf.readByteArray();
            try {
                this.routeNBT = NbtIo.readCompressed(new ByteArrayInputStream(compressed));
            } catch (IOException e) {
                e.printStackTrace();
                this.routeNBT = new CompoundTag();
            }
        }
        this.start = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
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
}