package com.talhanation.workers.network;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MessageCampPos implements Message<MessageCampPos> {

    private BlockPos campPos;
    private UUID worker;

    public MessageCampPos(){
    }

    public MessageCampPos(UUID worker, BlockPos campPos) {
        this.worker = worker;
        this.campPos = campPos;

    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        List<AbstractWorkerEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(AbstractWorkerEntity.class, context.getSender().getBoundingBox().inflate(8.0D));
        for (AbstractWorkerEntity workers : list) {

            //(this.worker == workers.getUUID())
                workers.setCampPos(Optional.of(campPos));
        }
    }
    public MessageCampPos fromBytes(FriendlyByteBuf buf) {
        this.worker = buf.readUUID();
        this.campPos= buf.readBlockPos();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.worker);
        buf.writeBlockPos(this.campPos);
    }

}