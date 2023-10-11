package com.talhanation.workers.network;

import com.talhanation.workers.entities.ChickenFarmerEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageChickenFarmerUseEggs implements Message<MessageChickenFarmerUseEggs> {
    private UUID worker;
    private boolean useEggs;
    public MessageChickenFarmerUseEggs() {
    }

    public MessageChickenFarmerUseEggs(UUID recruit, boolean useEggs) {
        this.worker = recruit;
        this.useEggs = useEggs;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {

        ServerPlayer player = context.getSender();
        player.level.getEntitiesOfClass(ChickenFarmerEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.worker))
                .stream()
                .filter(ChickenFarmerEntity::isAlive)
                .findAny()
                .ifPresent(farmer -> farmer.setUseEggs(useEggs));

    }

    public MessageChickenFarmerUseEggs fromBytes(FriendlyByteBuf buf) {
        this.worker = buf.readUUID();
        this.useEggs = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.worker);
        buf.writeBoolean(this.useEggs);
    }

}