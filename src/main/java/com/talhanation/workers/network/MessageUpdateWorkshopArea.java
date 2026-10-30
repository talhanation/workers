package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.WorkshopArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageUpdateWorkshopArea implements Message<MessageUpdateWorkshopArea> {

    public UUID uuid;
    public boolean useStorage;

    public MessageUpdateWorkshopArea() {}

    public MessageUpdateWorkshopArea(UUID uuid, boolean useStorage) {
        this.uuid       = uuid;
        this.useStorage = useStorage;
    }

    @Override
    public Dist getExecutingSide() { return Dist.DEDICATED_SERVER; }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;

        player.getCommandSenderWorld()
                .getEntitiesOfClass(WorkshopArea.class, player.getBoundingBox().inflate(64),
                        v -> v.getUUID().equals(this.uuid))
                .stream().findAny()
                .ifPresent(workshop -> {
                    if (!workshop.canPlayerSee(player)) return;
                    workshop.setUseStorage(useStorage);
                    workshop.requestCheck();
                });
    }

    @Override
    public MessageUpdateWorkshopArea fromBytes(FriendlyByteBuf buf) {
        this.uuid       = buf.readUUID();
        this.useStorage = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeBoolean(useStorage);
    }
}
