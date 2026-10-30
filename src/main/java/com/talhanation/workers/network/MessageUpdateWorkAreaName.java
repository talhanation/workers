package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

/** Client → Server: renames a work area (custom name), e.g. so the courier can list it by name. */
public class MessageUpdateWorkAreaName implements Message<MessageUpdateWorkAreaName> {

    private UUID   uuid;
    private String name;

    public MessageUpdateWorkAreaName() {}

    public MessageUpdateWorkAreaName(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;

        player.getCommandSenderWorld()
                .getEntitiesOfClass(AbstractWorkAreaEntity.class, player.getBoundingBox().inflate(64),
                        v -> v.getUUID().equals(this.uuid))
                .stream().findAny()
                .ifPresent(area -> {
                    if (!area.canPlayerSee(player)) return;
                    area.setCustomName(Component.literal(name));
                });
    }

    @Override
    public MessageUpdateWorkAreaName fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.name = buf.readUtf(32);
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUtf(name, 32);
    }
}
