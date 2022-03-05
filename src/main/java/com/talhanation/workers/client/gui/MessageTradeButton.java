package com.talhanation.workers.client.gui;

import com.talhanation.workers.CommandEvents;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.network.Message;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageTradeButton implements Message<MessageTradeButton> {

    private UUID uuid;
    private UUID merchant;
    private int trade;

    public MessageTradeButton() {
        this.uuid = new UUID(0, 0);
    }

    public MessageTradeButton(UUID merchant, UUID player, int trade) {
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
    public MessageTradeButton fromBytes(PacketBuffer buf) {
        this.trade = buf.readInt();
        this.uuid = buf.readUUID();
        this.merchant = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(PacketBuffer buf) {
        buf.writeInt(trade);
        buf.writeUUID(uuid);
        buf.writeUUID(merchant);
    }
}
