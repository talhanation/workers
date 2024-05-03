package com.talhanation.workers.network;

import com.talhanation.workers.CommandEvents;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageFollow implements Message<MessageFollow> {

    private UUID player;
    private UUID worker;

    public MessageFollow(){
    }

    public MessageFollow(UUID player, UUID worker) {
        this.player = player;
        this.worker = worker;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        List<AbstractWorkerEntity> workers = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AbstractWorkerEntity.class, context.getSender().getBoundingBox().inflate(64D));
        for (AbstractWorkerEntity worker : workers) {
            if(Objects.equals(worker.getUUID(), this.worker)) {
                if(worker.getStatus() == AbstractWorkerEntity.Status.FOLLOW) worker.setStatus(worker.prevStatus);
                else worker.setStatus(AbstractWorkerEntity.Status.FOLLOW, true);
            }
        }
    }
    public MessageFollow fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.worker = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeUUID(this.worker);
    }

}