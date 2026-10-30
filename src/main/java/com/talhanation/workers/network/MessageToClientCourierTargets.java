package com.talhanation.workers.network;

import com.talhanation.workers.client.gui.CourierScreen;
import com.talhanation.workers.world.CourierAction;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server → Client: the work areas the courier can use at a waypoint (answer to {@link MessageRequestCourierTargets}). */
public class MessageToClientCourierTargets implements Message<MessageToClientCourierTargets> {

    private BlockPos     waypointPos;
    private boolean      loaded;
    private List<Target> targets;

    public MessageToClientCourierTargets() {
        this.targets = new ArrayList<>();
    }

    public MessageToClientCourierTargets(BlockPos waypointPos, boolean loaded, List<Target> targets) {
        this.waypointPos = waypointPos;
        this.loaded      = loaded;
        this.targets     = targets;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(NetworkEvent.Context context) {
        if (Minecraft.getInstance().screen instanceof CourierScreen courierScreen) {
            courierScreen.onTargetsReceived(waypointPos, loaded, targets);
        }
    }

    @Override
    public MessageToClientCourierTargets fromBytes(FriendlyByteBuf buf) {
        this.waypointPos = buf.readBlockPos();
        this.loaded      = buf.readBoolean();
        int size = buf.readVarInt();
        this.targets = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UUID id                       = buf.readUUID();
            CourierAction.SourceType type = buf.readEnum(CourierAction.SourceType.class);
            String name                   = buf.readUtf();
            BlockPos pos                  = buf.readBlockPos();
            int slots                     = buf.readVarInt();
            targets.add(new Target(id, type, name, pos, slots));
        }
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(waypointPos);
        buf.writeBoolean(loaded);
        buf.writeVarInt(targets.size());
        for (Target target : targets) {
            buf.writeUUID(target.id());
            buf.writeEnum(target.type());
            buf.writeUtf(target.name());
            buf.writeBlockPos(target.pos());
            buf.writeVarInt(target.slots());
        }
    }

    /** A work area at the waypoint. {@code name} is empty when the area has no custom name. */
    public record Target(UUID id, CourierAction.SourceType type, String name, BlockPos pos, int slots) {}
}
