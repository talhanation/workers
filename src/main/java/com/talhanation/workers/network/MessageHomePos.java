package com.talhanation.workers.network;

import com.talhanation.workers.CommandEvents;
import com.talhanation.workers.Main;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageHomePos implements Message<MessageHomePos> {

    private BlockPos homePos;
    private UUID worker_uuid;
    private UUID player_uuid;

    public MessageHomePos(){
    }

    public MessageHomePos(UUID player_uuid, UUID worker_uuid, BlockPos homePos) {
        this.player_uuid = player_uuid;
        this.worker_uuid = worker_uuid;
        this.homePos = homePos;

    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        List<AbstractWorkerEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AbstractWorkerEntity.class, context.getSender().getBoundingBox().inflate(16.0D));
        for (AbstractWorkerEntity workers : list) {
            Main.LOGGER.debug("Message: " + homePos.toShortString());
            if(this.worker_uuid.equals(workers.getUUID())) CommandEvents.setHomePosWorker(player_uuid, workers, homePos);
        }
    }
    public MessageHomePos fromBytes(FriendlyByteBuf buf) {
        this.player_uuid = buf.readUUID();
        this.worker_uuid = buf.readUUID();
        this.homePos = buf.readBlockPos();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player_uuid);
        buf.writeUUID(this.worker_uuid);
        buf.writeBlockPos(this.homePos);
    }

}