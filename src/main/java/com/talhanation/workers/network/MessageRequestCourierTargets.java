package com.talhanation.workers.network;

import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.CourierEntity;
import com.talhanation.workers.entities.ai.CourierWorkGoal;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.MarketArea;
import com.talhanation.workers.world.CourierAction;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Client → Server: asks which work areas the courier could use at a waypoint. */
public class MessageRequestCourierTargets implements Message<MessageRequestCourierTargets> {

    private static final CourierAction.SourceType[] AREA_TYPES = {
            CourierAction.SourceType.STORAGE,
            CourierAction.SourceType.MARKET,
            CourierAction.SourceType.KITCHEN,
            CourierAction.SourceType.WORKSHOP
    };

    private UUID     courierUuid;
    private BlockPos waypointPos;

    public MessageRequestCourierTargets() {}

    public MessageRequestCourierTargets(UUID courierUuid, BlockPos waypointPos) {
        this.courierUuid = courierUuid;
        this.waypointPos = waypointPos;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        ServerLevel level = player.serverLevel();

        player.getCommandSenderWorld()
                .getEntitiesOfClass(CourierEntity.class,
                        player.getBoundingBox().inflate(32.0D),
                        c -> c.getUUID().equals(this.courierUuid) && c.isAlive())
                .stream()
                .findAny()
                .ifPresent(courier -> {
                    if (!courier.isOwned() || !player.getUUID().equals(courier.getOwnerUUID())) return;

                    // Work areas are entities: they only exist on the server while the chunk is loaded.
                    boolean loaded = level.isLoaded(waypointPos) && level.areEntitiesLoaded(ChunkPos.asLong(waypointPos));
                    List<MessageToClientCourierTargets.Target> targets = new ArrayList<>();

                    if (loaded) {
                        for (CourierAction.SourceType type : AREA_TYPES) {
                            for (AbstractWorkAreaEntity area : CourierWorkGoal.findTargetAreas(courier, level, waypointPos, type)) {
                                targets.add(new MessageToClientCourierTargets.Target(
                                        area.getUUID(), type, getAreaName(area), area.blockPosition(), countSlots(area)));
                            }
                        }
                    }

                    WorkersMain.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new MessageToClientCourierTargets(waypointPos, loaded, targets));
                });
    }

    private static String getAreaName(AbstractWorkAreaEntity area) {
        String name = area.getCustomName() != null ? area.getCustomName().getString() : "";
        if (name.isEmpty() && area instanceof MarketArea market) name = market.getMarketName();
        return name != null ? name : "";
    }

    private static int countSlots(AbstractWorkAreaEntity area) {
        int slots = 0;
        for (Container container : CourierWorkGoal.getTargetContainersOf(area)) {
            slots += container.getContainerSize();
        }
        return slots;
    }

    @Override
    public MessageRequestCourierTargets fromBytes(FriendlyByteBuf buf) {
        this.courierUuid = buf.readUUID();
        this.waypointPos = buf.readBlockPos();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(courierUuid);
        buf.writeBlockPos(waypointPos);
    }
}
