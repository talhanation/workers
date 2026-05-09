package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.HomeArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

import static com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity.DONE_TIME;

public class MessageUpdateHomeArea implements Message<MessageUpdateHomeArea> {

    public UUID uuid;
    public boolean teamAccess;
    public boolean evict;

    public MessageUpdateHomeArea() {

    }

    public MessageUpdateHomeArea(UUID uuid, boolean teamAccess, boolean evict) {
        this.uuid = uuid;
        this.teamAccess = teamAccess;
        this.evict = evict;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(HomeArea.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.uuid))
                .stream()
                .findAny()
                .ifPresent(this::update);
    }

    public void update(HomeArea homeArea) {
        if (evict) {
            homeArea.clearResident();
        }
        else {
            homeArea.setTeamAccess(this.teamAccess);
            homeArea.time += DONE_TIME;
        }
    }

    public MessageUpdateHomeArea fromBytes(FriendlyByteBuf buf) {
        this.uuid       = buf.readUUID();
        this.teamAccess = buf.readBoolean();
        this.evict      = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeBoolean(teamAccess);
        buf.writeBoolean(evict);
    }
}
