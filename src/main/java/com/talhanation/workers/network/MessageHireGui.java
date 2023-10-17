package com.talhanation.workers.network;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageHireGui implements Message<MessageHireGui> {

    private UUID uuid;
    private UUID worker_uuid;


    public MessageHireGui() {
        this.uuid = new UUID(0, 0);
    }

    public MessageHireGui(Player player, UUID recruit) {
        this.uuid = player.getUUID();
        this.worker_uuid = recruit;
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
        player.getCommandSenderWorld().getEntitiesOfClass(AbstractWorkerEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.worker_uuid))
                .stream()
                .filter(AbstractWorkerEntity::isAlive)
                .findAny()
                .ifPresent(worker -> worker.openHireGUI(player));
    }

    @Override
    public MessageHireGui fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.worker_uuid = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(worker_uuid);
    }

}