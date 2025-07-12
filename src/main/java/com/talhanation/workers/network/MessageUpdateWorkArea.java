package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

import static com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity.DONE_TIME;

public class MessageUpdateWorkArea implements Message<MessageUpdateWorkArea> {
    public float x;
    public float y;
    public float z;
    public UUID uuid;
    public String name;
    public boolean destroy;
    public MessageUpdateWorkArea() {

    }

    public MessageUpdateWorkArea(UUID uuid, String name, Vec3 vec3, boolean destroy) {
        this.x = (float) vec3.x;
        this.y = (float) vec3.y;
        this.z = (float) vec3.z;
        this.uuid = uuid;
        this.name = name;
        this.destroy = destroy;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        ServerPlayer player = context.getSender();
        if(player == null) return;
        String teamStringID = "";
        if(player.getTeam() != null){
            teamStringID = player.getTeam().getName();
        }

        player.getCommandSenderWorld().getEntitiesOfClass(AbstractWorkAreaEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.uuid))
                .stream()
                .findAny()
                .ifPresent(this::updateWorkArea);

    }

    public void updateWorkArea(AbstractWorkAreaEntity workArea){
        if(destroy) workArea.remove(Entity.RemovalReason.DISCARDED);

        workArea.setCustomName(Component.literal(name));

        workArea.moveTo(this.x, this.y, this.z);

        workArea.timeSinceLastVisit += DONE_TIME;
    }

    public MessageUpdateWorkArea fromBytes(FriendlyByteBuf buf) {
        this.x = buf.readFloat();
        this.y = buf.readFloat();
        this.z = buf.readFloat();
        this.uuid = buf.readUUID();
        this.name = buf.readUtf();
        this.destroy = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
        buf.writeUUID(uuid);
        buf.writeUtf(name);
        buf.writeBoolean(destroy);
    }

}
