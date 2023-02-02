package com.talhanation.workers.network;

import com.talhanation.workers.CommandEvents;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageBed implements Message<MessageBed> {

    private UUID player;
    private BlockPos bedPos;

    public MessageBed(){
    }

    public MessageBed(UUID player, BlockPos bedPos) {
        this.player = player;
        this.bedPos = bedPos;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        List<AbstractWorkerEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AbstractWorkerEntity.class, context.getSender().getBoundingBox().inflate(5.5D));
        for (AbstractWorkerEntity workers : list) {
                CommandEvents.setBedPosWorker(this.player, workers, this.bedPos);
        }
    }
    public MessageBed fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.bedPos= buf.readBlockPos();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeBlockPos(this.bedPos);
    }

}