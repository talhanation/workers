package com.talhanation.workers.network;

import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;


public class MessageUpdateOwner implements Message<MessageUpdateOwner> {

    public UUID uuid;
    public UUID playerUUID;
    public String playerName;
    public MessageUpdateOwner() {

    }

    public MessageUpdateOwner(UUID uuid, RecruitsPlayerInfo playerInfo) {
        this.uuid = uuid;
        this.playerUUID = playerInfo.getUUID();
        this.playerName = playerInfo.getName();
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        ServerPlayer player = context.getSender();
        if(player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(AbstractWorkAreaEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.uuid))
                .stream()
                .findAny()
                .ifPresent(this::updateWorkArea);

    }

    public void updateWorkArea(AbstractWorkAreaEntity workArea){
        workArea.setPlayerUUID(this.playerUUID);
        workArea.setPlayerName(this.playerName);

        Player player = workArea.level().getPlayerByUUID(playerUUID);

        if(player == null || player.getTeam() == null) return;

        workArea.setTeamStringID(player.getTeam().getName());
    }

    public MessageUpdateOwner fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.playerUUID = buf.readUUID();
        this.playerName = buf.readUtf();
        return this;
    }
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(playerUUID);
        buf.writeUtf(playerName);
    }
}
