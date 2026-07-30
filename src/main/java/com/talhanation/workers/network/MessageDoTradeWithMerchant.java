package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.network.compat.WorkersMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.workers.network.compat.WorkersNetworkContext;

import java.util.UUID;

public class MessageDoTradeWithMerchant implements WorkersMessage<MessageDoTradeWithMerchant> {

    public UUID merchantUuid;
    public UUID trade;
    public MessageDoTradeWithMerchant() {}
    public MessageDoTradeWithMerchant(UUID merchantUuid, UUID trade) {
        this.merchantUuid = merchantUuid;
        this.trade = trade;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(WorkersNetworkContext context){
        ServerPlayer player = context.getSender();
        if(player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(MerchantEntity.class, player.getBoundingBox()
                        .inflate(32.0D), v -> v
                        .getUUID()
                        .equals(this.merchantUuid))
                .stream()
                .findAny()
                .ifPresent(merchant -> merchant.doTrade(trade, player));

    }

    public MessageDoTradeWithMerchant fromBytes(FriendlyByteBuf buf) {
        this.merchantUuid = buf.readUUID();
        this.trade = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(merchantUuid);
        buf.writeUUID(this.trade);
    }
}
