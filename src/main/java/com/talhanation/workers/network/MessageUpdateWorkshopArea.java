package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.WorkshopArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class MessageUpdateWorkshopArea implements Message<MessageUpdateWorkshopArea> {

    public UUID uuid;
    public boolean useStorage;

    public MessageUpdateWorkshopArea() {}

    public MessageUpdateWorkshopArea(UUID uuid, boolean useStorage) {
        this.uuid       = uuid;
        this.useStorage = useStorage;
    }

    @Override
    public PacketFlow getExecutingSide() { return PacketFlow.SERVERBOUND; }

    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());
        if (player == null) return;

        player.getCommandSenderWorld()
                .getEntitiesOfClass(WorkshopArea.class, player.getBoundingBox().inflate(64),
                        v -> v.getUUID().equals(this.uuid))
                .stream().findAny()
                .ifPresent(workshop -> {
                    if (!workshop.canPlayerSee(player)) return;
                    workshop.setUseStorage(useStorage);
                    workshop.requestCheck();
                });
    }

    @Override
    public MessageUpdateWorkshopArea fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid       = buf.readUUID();
        this.useStorage = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeBoolean(useStorage);
    }

    public static final CustomPacketPayload.Type<MessageUpdateWorkshopArea> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_update_workshop_area"));

    @Override
    public CustomPacketPayload.Type<MessageUpdateWorkshopArea> type() {
        return TYPE;
    }
}
