package com.talhanation.workers.network;

import com.talhanation.workers.CommandEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageOpenCommandScreen implements Message<MessageOpenCommandScreen> {
    private UUID uuid;

    public MessageOpenCommandScreen() {
        this.uuid = new UUID(0, 0);
    }

    public MessageOpenCommandScreen(Player player) {
        this.uuid = player.getUUID();
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (!player.getUUID().equals(uuid)) {
            return;
        }
        CommandEvents.updateCommandScreen(player, (ServerLevel) player.getCommandSenderWorld());
        CommandEvents.openCommandScreen(player);
    }

    @Override
    public MessageOpenCommandScreen fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
    }

}

