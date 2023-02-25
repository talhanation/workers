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

public class MessageStartPos implements Message<MessageStartPos> {

    private UUID player;
    private BlockPos startPos;
    private UUID worker;

    public MessageStartPos(){
    }

    public MessageStartPos(UUID player, BlockPos startPos, UUID worker) {
        this.player = player;
        this.startPos = startPos;
        this.worker = worker;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        List<AbstractWorkerEntity> workers = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AbstractWorkerEntity.class, context.getSender().getBoundingBox().inflate(64D));
        for (AbstractWorkerEntity worker : workers) {
            if(Objects.equals(worker.getOwnerUUID(), player))
                CommandEvents.setStartPosWorker(this.player, worker, this.startPos);
        }
    }
    public MessageStartPos fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.startPos= buf.readBlockPos();
        this.worker = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeBlockPos(this.startPos);
        buf.writeUUID(this.worker);
    }

}