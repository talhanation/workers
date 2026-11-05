package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Client → Server: renames a work area (custom name), e.g. so the courier can list it by name. */
public class MessageUpdateWorkAreaName implements Message<MessageUpdateWorkAreaName> {

    private UUID   uuid;
    private String name;

    public MessageUpdateWorkAreaName() {}

    public MessageUpdateWorkAreaName(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
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
                .getEntitiesOfClass(AbstractWorkAreaEntity.class, player.getBoundingBox().inflate(64),
                        v -> v.getUUID().equals(this.uuid))
                .stream().findAny()
                .ifPresent(area -> {
                    if (!area.canPlayerSee(player)) return;
                    area.setCustomName(Component.literal(name));
                });
    }

    @Override
    public MessageUpdateWorkAreaName fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.name = buf.readUtf(32);
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUtf(name, 32);
    }

    public static final CustomPacketPayload.Type<MessageUpdateWorkAreaName> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_update_work_area_name"));

    @Override
    public CustomPacketPayload.Type<MessageUpdateWorkAreaName> type() {
        return TYPE;
    }
}
