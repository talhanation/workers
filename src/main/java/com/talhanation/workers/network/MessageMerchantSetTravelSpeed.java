package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageMerchantSetTravelSpeed implements Message<MessageMerchantSetTravelSpeed> {
    private UUID worker;
    private int state;
    public MessageMerchantSetTravelSpeed() {
    }

    public MessageMerchantSetTravelSpeed(UUID recruit, int state) {
        this.worker = recruit;
        this.state = state;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {

        ServerPlayer player = context.getSender();
        player.level.getEntitiesOfClass(MerchantEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.worker))
                .stream()
                .filter(MerchantEntity::isAlive)
                .findAny()
                .ifPresent(merchant -> merchant.setTravelSpeedState(state));

    }

    public MessageMerchantSetTravelSpeed fromBytes(FriendlyByteBuf buf) {
        this.worker = buf.readUUID();
        this.state = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.worker);
        buf.writeInt(this.state);
    }

}