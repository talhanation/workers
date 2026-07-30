package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.network.compat.WorkersMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.workers.network.compat.WorkersNetworkContext;

import java.util.UUID;

public class MessageMoveMerchantTrade implements WorkersMessage<MessageMoveMerchantTrade> {

    public UUID merchantUuid;
    public UUID tradeUuid;
    public boolean moveUp;

    public MessageMoveMerchantTrade() {}
    public MessageMoveMerchantTrade(UUID merchantUuid, UUID tradeUuid, boolean moveUp) {
        this.merchantUuid = merchantUuid;
        this.tradeUuid    = tradeUuid;
        this.moveUp       = moveUp;
    }

    @Override
    public PacketFlow getExecutingSide() { return PacketFlow.SERVERBOUND; }

    @Override
    public void executeServerSide(WorkersNetworkContext context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;

        player.getCommandSenderWorld()
                .getEntitiesOfClass(MerchantEntity.class,
                        player.getBoundingBox().inflate(32.0D),
                        v -> v.getUUID().equals(this.merchantUuid))
                .stream().findAny()
                .ifPresent(merchant -> {
                    if (!player.getUUID().equals(merchant.getOwnerUUID()) && !player.hasPermissions(2)) return;
                    if (moveUp) merchant.moveTradeUp(tradeUuid);
                    else        merchant.moveTradeDown(tradeUuid);
                });
    }

    @Override
    public MessageMoveMerchantTrade fromBytes(FriendlyByteBuf buf) {
        this.merchantUuid = buf.readUUID();
        this.tradeUuid    = buf.readUUID();
        this.moveUp       = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(merchantUuid);
        buf.writeUUID(tradeUuid);
        buf.writeBoolean(moveUp);
    }
}
