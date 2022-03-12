package com.talhanation.workers.network;

import com.talhanation.workers.entities.ShepherdEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageSheepCount implements Message<MessageSheepCount> {

    private int sheepCount;
    private UUID uuid;

    public MessageSheepCount(){
    }

    public MessageSheepCount(int mineDepth, UUID uuid) {
        this.sheepCount = mineDepth;
        this.uuid = uuid;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        List<ShepherdEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(ShepherdEntity.class, context.getSender().getBoundingBox().inflate(16.0D));
        for (ShepherdEntity recruits : list){

            if (recruits.getUUID().equals(this.uuid))
                recruits.setMaxSheepCount(this.sheepCount);
        }

    }
    public MessageSheepCount fromBytes(PacketBuffer buf) {
        this.sheepCount = buf.readInt();
        this.uuid = buf.readUUID();
        return this;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeInt(sheepCount);
        buf.writeUUID(uuid);
    }

}