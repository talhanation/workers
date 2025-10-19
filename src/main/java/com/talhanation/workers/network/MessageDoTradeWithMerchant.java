package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageDoTradeWithMerchant implements Message<MessageDoTradeWithMerchant> {

    public UUID merchantUuid;
    public UUID trade;
    public MessageDoTradeWithMerchant() {}
    public MessageDoTradeWithMerchant(UUID merchantUuid, UUID trade) {
        this.merchantUuid = merchantUuid;
        this.trade = trade;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
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
