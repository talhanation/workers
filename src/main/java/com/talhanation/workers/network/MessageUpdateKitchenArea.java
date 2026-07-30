package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.KitchenArea;
import com.talhanation.workers.network.compat.WorkersMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.workers.network.compat.WorkersNetworkContext;

import java.util.UUID;

public class MessageUpdateKitchenArea implements WorkersMessage<MessageUpdateKitchenArea> {

    public UUID uuid;
    public boolean sellToVillagers;

    public MessageUpdateKitchenArea() {}

    public MessageUpdateKitchenArea(UUID uuid, boolean sellToVillagers) {
        this.uuid            = uuid;
        this.sellToVillagers = sellToVillagers;
    }

    @Override
    public PacketFlow getExecutingSide() { return PacketFlow.SERVERBOUND; }

    @Override
    public void executeServerSide(WorkersNetworkContext context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;

        player.getCommandSenderWorld()
                .getEntitiesOfClass(KitchenArea.class, player.getBoundingBox().inflate(64),
                        v -> v.getUUID().equals(this.uuid))
                .stream().findAny()
                .ifPresent(kitchen -> {
                    kitchen.setFeedVillagers(sellToVillagers);
                    kitchen.scanArea();
                });
    }

    @Override
    public MessageUpdateKitchenArea fromBytes(FriendlyByteBuf buf) {
        this.uuid            = buf.readUUID();
        this.sellToVillagers = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeBoolean(sellToVillagers);
    }
}
