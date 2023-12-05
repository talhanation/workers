package com.talhanation.workers.network;

import com.talhanation.workers.entities.LumberjackEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageLumberjackReplant implements Message<MessageLumberjackReplant> {
    private UUID worker;
    private boolean replantSaplings;
    public MessageLumberjackReplant() {
    }

    public MessageLumberjackReplant(UUID recruit, boolean replantSaplings) {
        this.worker = recruit;
        this.replantSaplings = replantSaplings;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {

        ServerPlayer player = context.getSender();
        player.getCommandSenderWorld().getEntitiesOfClass(LumberjackEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.worker))
                .stream()
                .filter(LumberjackEntity::isAlive)
                .findAny()
                .ifPresent(farmer -> farmer.setReplantSaplings(replantSaplings));

    }

    public MessageLumberjackReplant fromBytes(FriendlyByteBuf buf) {
        this.worker = buf.readUUID();
        this.replantSaplings = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.worker);
        buf.writeBoolean(this.replantSaplings);
    }

}