package com.talhanation.workers.network;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.MerchantEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageMerchantTradeLimitButton implements Message<MessageMerchantTradeLimitButton> {

    private int limit;
    private int index;
    private UUID uuid;

    public MessageMerchantTradeLimitButton(){
    }

    public MessageMerchantTradeLimitButton(int index, int limit, UUID uuid) {
        this.index = index;
        this.limit = limit;
        this.uuid = uuid;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<MerchantEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(MerchantEntity.class, context.getSender().getBoundingBox().inflate(16.0D));
        for (MerchantEntity merchant : list){

            if (merchant.getUUID().equals(this.uuid)) {
                merchant.setTradeLimit(this.index, this.limit);
                Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(context::getSender), new MessageToClientUpdateMerchantScreen(merchant.WAYPOINTS, merchant.getCurrentTrades(), merchant.getTradeLimits()));
            }
        }

    }
    public MessageMerchantTradeLimitButton fromBytes(FriendlyByteBuf buf) {
        this.limit = buf.readInt();
        this.uuid = buf.readUUID();
        this.index = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(limit);
        buf.writeUUID(uuid);
        buf.writeInt(index);
    }

}