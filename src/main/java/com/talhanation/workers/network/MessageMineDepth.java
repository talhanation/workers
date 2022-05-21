package com.talhanation.workers.network;

import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageMineDepth implements Message<MessageMineDepth> {

    private int mineDepth;
    private UUID uuid;

    public MessageMineDepth(){
    }

    public MessageMineDepth(int mineDepth, UUID uuid) {
        this.mineDepth = mineDepth;
        this.uuid = uuid;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<MinerEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(MinerEntity.class, context.getSender().getBoundingBox().inflate(16.0D));
        for (MinerEntity recruits : list){

            if (recruits.getUUID().equals(this.uuid))
                recruits.setMineDepth(this.mineDepth);
        }

    }
    public MessageMineDepth fromBytes(FriendlyByteBuf buf) {
        this.mineDepth = buf.readInt();
        this.uuid = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(mineDepth);
        buf.writeUUID(uuid);
    }

}