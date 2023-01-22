package com.talhanation.workers.network;

import com.talhanation.workers.entities.AbstractAnimalFarmerEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageAnimalCount implements Message<MessageAnimalCount> {

    private int animalCount;
    private UUID uuid;

    public MessageAnimalCount() {
    }

    public MessageAnimalCount(int mineDepth, UUID uuid) {
        this.animalCount = mineDepth;
        this.uuid = uuid;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        List<AbstractAnimalFarmerEntity> list = Objects.requireNonNull(context.getSender()).level.getEntitiesOfClass(
                AbstractAnimalFarmerEntity.class, context.getSender().getBoundingBox().inflate(16.0D));
        for (AbstractAnimalFarmerEntity recruits : list) {

            if (recruits.getUUID().equals(this.uuid))
                recruits.setMaxAnimalCount(this.animalCount);
        }

    }

    public MessageAnimalCount fromBytes(FriendlyByteBuf buf) {
        this.animalCount = buf.readInt();
        this.uuid = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(animalCount);
        buf.writeUUID(uuid);
    }

}