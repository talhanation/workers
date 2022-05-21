package com.talhanation.workers.network;

import com.talhanation.workers.CommandEvents;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageStartPos implements Message<MessageStartPos> {

    private UUID player;
    private BlockPos startPos;

    public MessageStartPos(){
    }

    public MessageStartPos(UUID player, BlockPos startPos) {
        this.player = player;
        this.startPos = startPos;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        List<AbstractWorkerEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AbstractWorkerEntity.class, context.getSender().getBoundingBox().inflate(8.0D));
        for (AbstractWorkerEntity workers : list) {
                CommandEvents.setStartPosWorker(this.player, workers, this.startPos);
        }
    }
    public MessageStartPos fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.startPos= buf.readBlockPos();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeBlockPos(this.startPos);
    }

}