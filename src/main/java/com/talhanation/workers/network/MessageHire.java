package com.talhanation.workers.network;

import com.talhanation.workers.CommandEvents;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageHire implements Message<MessageHire> {

    private UUID player;
    private UUID worker;
    private String name;

    public MessageHire() {
    }

    public MessageHire(UUID player, UUID recruit, String name) {
        this.name = name;
        this.player = player;
        this.worker = recruit;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {

        ServerPlayer player = context.getSender();
        player.level.getEntitiesOfClass(AbstractWorkerEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.worker))
                .stream()
                .filter(AbstractWorkerEntity::isAlive)
                .findAny()
                .ifPresent(abstractRecruitEntity -> CommandEvents.handleRecruiting(player, abstractRecruitEntity, name));

    }

    public MessageHire fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.worker = buf.readUUID();
        this.name = buf.readUtf();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeUUID(this.worker);
        buf.writeUtf(this.name);
    }

}