package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.StorageArea;
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

public class MessageUpdateStorageArea implements Message<MessageUpdateStorageArea> {

    public UUID uuid;
    public int mask;
    public String name;
    public MessageUpdateStorageArea() {

    }

    public MessageUpdateStorageArea(UUID uuid, int mask, String name) {
        this.uuid = uuid;
        this.mask = mask;
        this.name = name;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context){
        ServerPlayer player = ((ServerPlayer) context.player());
        if(player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(StorageArea.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.uuid))
                .stream()
                .findAny()
                .ifPresent(this::update);

    }

    public void update(StorageArea storageArea){
        storageArea.setStorageTypes(mask);
        storageArea.setCustomName(Component.literal(name));
    }

    public MessageUpdateStorageArea fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.mask = buf.readInt();
        this.name = buf.readUtf();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeInt(mask);
        buf.writeUtf(name);
    }

    public static final CustomPacketPayload.Type<MessageUpdateStorageArea> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_update_storage_area"));

    @Override
    public CustomPacketPayload.Type<MessageUpdateStorageArea> type() {
        return TYPE;
    }
}
