package com.talhanation.workers.network;

import com.talhanation.workers.entities.MinerEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageOpenGuiMiner implements Message<MessageOpenGuiMiner> {

    private UUID uuid;
    private UUID worker;


    public MessageOpenGuiMiner() {
        this.uuid = new UUID(0, 0);
    }

    public MessageOpenGuiMiner(Player player, UUID worker) {
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
        player.level.getEntitiesOfClass(MinerEntity.class, player.getBoundingBox()
                .inflate(16.0D), v -> v
                .getUUID()
                .equals(this.worker))
                .stream()
                .filter(Entity::isAlive)
                .findAny()
                .ifPresent(worker -> worker.openGUI(player));
    }

    @Override
    public MessageOpenGuiMiner fromBytes(FriendlyByteBuf buf) {
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