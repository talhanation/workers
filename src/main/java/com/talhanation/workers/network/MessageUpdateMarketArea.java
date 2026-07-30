package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.MarketArea;
import com.talhanation.workers.network.compat.WorkersMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.workers.network.compat.WorkersNetworkContext;

import java.util.UUID;

public class MessageUpdateMarketArea implements WorkersMessage<MessageUpdateMarketArea> {

    public UUID uuid;
    public boolean isOpen;
    public String marketName;

    public MessageUpdateMarketArea() {}

    public MessageUpdateMarketArea(UUID uuid, boolean isOpen, String marketName) {
        this.uuid = uuid;
        this.isOpen = isOpen;
        this.marketName = marketName;
    }

    @Override
    public PacketFlow getExecutingSide() { return PacketFlow.SERVERBOUND; }

    @Override
    public void executeServerSide(WorkersNetworkContext context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;

        player.getCommandSenderWorld()
                .getEntitiesOfClass(MarketArea.class, player.getBoundingBox().inflate(64),
                        v -> v.getUUID().equals(this.uuid))
                .stream().findAny()
                .ifPresent(market -> {
                    market.setOpen(isOpen);
                    market.setMarketName(marketName);
                });
    }

    @Override
    public MessageUpdateMarketArea fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.isOpen = buf.readBoolean();
        this.marketName = buf.readUtf();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeBoolean(isOpen);
        buf.writeUtf(marketName);
    }
}
