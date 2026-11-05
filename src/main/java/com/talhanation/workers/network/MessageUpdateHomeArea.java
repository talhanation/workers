package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.HomeArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

import static com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity.DONE_TIME;

public class MessageUpdateHomeArea implements Message<MessageUpdateHomeArea> {

    public UUID uuid;
    public boolean teamAccess;
    public boolean evict;

    public MessageUpdateHomeArea() {

    }

    public MessageUpdateHomeArea(UUID uuid, boolean teamAccess, boolean evict) {
        this.uuid = uuid;
        this.teamAccess = teamAccess;
        this.evict = evict;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());
        if (player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(HomeArea.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.uuid))
                .stream()
                .findAny()
                .ifPresent(this::update);
    }

    public void update(HomeArea homeArea) {
        if (evict) {
            homeArea.clearResident();
        }
        else {
            homeArea.setTeamAccess(this.teamAccess);
            homeArea.time += DONE_TIME;
        }
    }

    public MessageUpdateHomeArea fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid       = buf.readUUID();
        this.teamAccess = buf.readBoolean();
        this.evict      = buf.readBoolean();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeBoolean(teamAccess);
        buf.writeBoolean(evict);
    }

    public static final CustomPacketPayload.Type<MessageUpdateHomeArea> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_update_home_area"));

    @Override
    public CustomPacketPayload.Type<MessageUpdateHomeArea> type() {
        return TYPE;
    }
}
