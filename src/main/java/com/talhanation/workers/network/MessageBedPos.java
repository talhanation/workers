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

public class MessageBedPos implements Message<MessageBedPos> {

    private UUID player;
    private UUID worker;
    private BlockPos bedPos;

    public MessageBedPos(){
    }

    public MessageBedPos(UUID player, BlockPos bedPos, UUID worker) {
        this.worker = worker;
        this.player = player;
        this.bedPos = bedPos;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        List<AbstractWorkerEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AbstractWorkerEntity.class, context.getSender().getBoundingBox().inflate(5.5D));
        for (AbstractWorkerEntity worker : list) {
            if(worker.getUUID().equals(this.worker))
                CommandEvents.setBedPosWorker(this.player, worker, this.bedPos);
        }
    }
    public MessageBedPos fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.worker = buf.readUUID();
        this.bedPos= buf.readBlockPos();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeUUID(this.worker);
        buf.writeBlockPos(this.bedPos);
    }

}