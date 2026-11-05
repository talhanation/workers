package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class MessageSetFarTrader implements Message<MessageSetFarTrader> {

    private UUID merchantUuid;
    private boolean farTrader;

    public MessageSetFarTrader() {}

    public MessageSetFarTrader(UUID merchantUuid, boolean farTrader) {
        this.merchantUuid = merchantUuid;
        this.farTrader = farTrader;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());
        if (player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(MerchantEntity.class, player.getBoundingBox().inflate(32.0D),
                        merchant -> merchant.getUUID().equals(this.merchantUuid) && player.getUUID().equals(merchant.getOwnerUUID()))
                .stream()
                .findAny()
                .ifPresent(merchant -> merchant.setFarTrader(farTrader));
    }

    @Override
    public MessageSetFarTrader fromBytes(RegistryFriendlyByteBuf buf) {
        this.merchantUuid = buf.readUUID();
        this.farTrader = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(merchantUuid);
        buf.writeBoolean(farTrader);
    }

    public static final CustomPacketPayload.Type<MessageSetFarTrader> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_set_far_trader"));

    @Override
    public CustomPacketPayload.Type<MessageSetFarTrader> type() {
        return TYPE;
    }
}
