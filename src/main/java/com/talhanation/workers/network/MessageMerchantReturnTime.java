package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.entities.MinerEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageMerchantReturnTime implements Message<MessageMerchantReturnTime> {

    private int time;
    private UUID uuid;

    public MessageMerchantReturnTime(){
    }

    public MessageMerchantReturnTime(int time, UUID uuid) {
        this.time = time;
        this.uuid = uuid;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<MerchantEntity> list = Objects.requireNonNull(context.getSender()).getCommandSenderWorld().getEntitiesOfClass(MerchantEntity.class, context.getSender().getBoundingBox().inflate(16.0D));
        for (MerchantEntity merchant : list){

            if (merchant.getUUID().equals(this.uuid))
                merchant.setReturningTime(this.time);
        }

    }
    public MessageMerchantReturnTime fromBytes(FriendlyByteBuf buf) {
        this.time = buf.readInt();
        this.uuid = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(time);
        buf.writeUUID(uuid);
    }

}