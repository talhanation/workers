package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.StorageArea;
import com.talhanation.workers.network.compat.WorkersMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.workers.network.compat.WorkersNetworkContext;

import java.util.UUID;

public class MessageUpdateStorageArea implements WorkersMessage<MessageUpdateStorageArea> {

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

    public void executeServerSide(WorkersNetworkContext context){
        ServerPlayer player = context.getSender();
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

    public MessageUpdateStorageArea fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.mask = buf.readInt();
        this.name = buf.readUtf();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeInt(mask);
        buf.writeUtf(name);
    }

}
