package com.talhanation.workers.network;

import com.talhanation.workers.entities.MinerEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageMineType implements Message<MessageMineType> {

    private int mineType;
    private UUID uuid;

    public MessageMineType(){
    }

    public MessageMineType(int mineType, UUID uuid) {
        this.mineType = mineType;
        this.uuid = uuid;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<MinerEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(MinerEntity.class, context.getSender().getBoundingBox().inflate(16.0D));
        for (MinerEntity recruits : list){

            if (recruits.getUUID().equals(this.uuid)) {
                recruits.setMineType(this.mineType);
                recruits.resetWorkerParameters();
            }
        }

    }
    public MessageMineType fromBytes(FriendlyByteBuf buf) {
        this.mineType = buf.readInt();
        this.uuid = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(mineType);
        buf.writeUUID(uuid);
    }

}