package com.talhanation.workers.network;

import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.entities.MerchantEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.UUID;

/** The owner opens the list of markets the merchant knows. */
public class MessageRequestMerchantMemory implements Message<MessageRequestMerchantMemory> {

    private UUID merchantUuid;

    public MessageRequestMerchantMemory() {}

    public MessageRequestMerchantMemory(UUID merchantUuid) {
        this.merchantUuid = merchantUuid;
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
                .ifPresent(merchant -> PacketDistributor.sendToPlayer(player,
                        new MessageToClientMerchantMemory(merchant.getUUID(), new ArrayList<>(merchant.getMemory().getEntries()))));
    }

    @Override
    public MessageRequestMerchantMemory fromBytes(RegistryFriendlyByteBuf buf) {
        this.merchantUuid = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(merchantUuid);
    }

    public static final CustomPacketPayload.Type<MessageRequestMerchantMemory> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_request_merchant_memory"));

    @Override
    public CustomPacketPayload.Type<MessageRequestMerchantMemory> type() {
        return TYPE;
    }
}
