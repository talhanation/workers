package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.KitchenArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class MessageUpdateKitchenArea implements Message<MessageUpdateKitchenArea> {

    public UUID uuid;
    public boolean sellToVillagers;
    public boolean useStorage;

    public MessageUpdateKitchenArea() {}

    public MessageUpdateKitchenArea(UUID uuid, boolean sellToVillagers, boolean useStorage) {
        this.uuid            = uuid;
        this.sellToVillagers = sellToVillagers;
        this.useStorage      = useStorage;
    }

    @Override
    public PacketFlow getExecutingSide() { return PacketFlow.SERVERBOUND; }

    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());
        if (player == null) return;

        player.getCommandSenderWorld()
                .getEntitiesOfClass(KitchenArea.class, player.getBoundingBox().inflate(64),
                        v -> v.getUUID().equals(this.uuid))
                .stream().findAny()
                .ifPresent(kitchen -> {
                    kitchen.setFeedVillagers(sellToVillagers);
                    kitchen.setUseStorage(useStorage);
                    kitchen.scanArea();
                });
    }

    @Override
    public MessageUpdateKitchenArea fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid            = buf.readUUID();
        this.sellToVillagers = buf.readBoolean();
        this.useStorage      = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeBoolean(sellToVillagers);
        buf.writeBoolean(useStorage);
    }

    public static final CustomPacketPayload.Type<MessageUpdateKitchenArea> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_update_kitchen_area"));

    @Override
    public CustomPacketPayload.Type<MessageUpdateKitchenArea> type() {
        return TYPE;
    }
}
