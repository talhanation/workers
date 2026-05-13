package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.KitchenArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageUpdateKitchenArea implements Message<MessageUpdateKitchenArea> {

    public UUID uuid;
    public boolean sellToVillagers;

    public MessageUpdateKitchenArea() {}

    public MessageUpdateKitchenArea(UUID uuid, boolean sellToVillagers) {
        this.uuid            = uuid;
        this.sellToVillagers = sellToVillagers;
    }

    @Override
    public Dist getExecutingSide() { return Dist.DEDICATED_SERVER; }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;

        player.getCommandSenderWorld()
                .getEntitiesOfClass(KitchenArea.class, player.getBoundingBox().inflate(64),
                        v -> v.getUUID().equals(this.uuid))
                .stream().findAny()
                .ifPresent(kitchen -> {
                    kitchen.setSellToVillagers(sellToVillagers);
                    kitchen.scanArea();
                });
    }

    @Override
    public MessageUpdateKitchenArea fromBytes(FriendlyByteBuf buf) {
        this.uuid            = buf.readUUID();
        this.sellToVillagers = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeBoolean(sellToVillagers);
    }
}
