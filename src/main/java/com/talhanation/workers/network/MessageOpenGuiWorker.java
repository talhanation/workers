package com.talhanation.workers.network;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageOpenGuiWorker implements Message<MessageOpenGuiWorker> {

    private UUID uuid;
    private UUID worker;


    public MessageOpenGuiWorker() {
        this.uuid = new UUID(0, 0);
    }

    public MessageOpenGuiWorker(Player player, UUID worker) {
        this.uuid = player.getUUID();
        this.worker = worker;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        if (!context.getSender().getUUID().equals(uuid)) {
            return;
        }

        ServerPlayer player = context.getSender();
        player.level.getEntitiesOfClass(AbstractWorkerEntity.class, player.getBoundingBox()
                .inflate(16.0D), v -> v
                .getUUID()
                .equals(this.worker))
                .stream()
                .filter(Entity::isAlive)
                .findAny()
                .ifPresent(worker -> worker.openGUI(player));
    }

    @Override
    public MessageOpenGuiWorker fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.worker = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(worker);
    }

}