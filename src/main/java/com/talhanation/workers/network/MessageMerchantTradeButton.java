package com.talhanation.workers.network;

import com.talhanation.workers.CommandEvents;
import com.talhanation.workers.entities.MerchantEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageMerchantTradeButton implements Message<MessageMerchantTradeButton> {

    private UUID uuid;
    private UUID merchant;
    private int trade;

    public MessageMerchantTradeButton() {
        this.uuid = new UUID(0, 0);
    }

    public MessageMerchantTradeButton(UUID merchant, UUID player, int trade) {
        this.trade = trade;
        this.uuid = player;
        this.merchant = merchant;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        if (!context.getSender().getUUID().equals(uuid)) {
            return;
        }
        List<MerchantEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(MerchantEntity.class, context.getSender().getBoundingBox().inflate(16.0D));
        for (MerchantEntity recruits : list){

            if (recruits.getUUID().equals(this.merchant)) {
                CommandEvents.handleMerchantTrade(context.getSender(), recruits, this.trade);
            }
        }
    }

    @Override
    public MessageMerchantTradeButton fromBytes(FriendlyByteBuf buf) {
        this.trade = buf.readInt();
        this.uuid = buf.readUUID();
        this.merchant = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(trade);
        buf.writeUUID(uuid);
        buf.writeUUID(merchant);
    }
}
