package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.world.MerchantMemory;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Favorite / avoid a known market. */
public class MessageUpdateMerchantMemory implements Message<MessageUpdateMerchantMemory> {

    private UUID merchantUuid;
    private UUID marketUuid;
    private boolean favorite;
    private boolean avoid;

    public MessageUpdateMerchantMemory() {}

    public MessageUpdateMerchantMemory(UUID merchantUuid, UUID marketUuid, boolean favorite, boolean avoid) {
        this.merchantUuid = merchantUuid;
        this.marketUuid = marketUuid;
        this.favorite = favorite;
        this.avoid = avoid;
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
                .ifPresent(merchant -> {
                    MerchantMemory.Entry entry = merchant.getMemory().get(marketUuid);
                    if (entry == null) return;
                    // a market is either a favorite or avoided
                    entry.favorite = favorite && !avoid;
                    entry.avoid = avoid;
                });
    }

    @Override
    public MessageUpdateMerchantMemory fromBytes(RegistryFriendlyByteBuf buf) {
        this.merchantUuid = buf.readUUID();
        this.marketUuid = buf.readUUID();
        this.favorite = buf.readBoolean();
        this.avoid = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(merchantUuid);
        buf.writeUUID(marketUuid);
        buf.writeBoolean(favorite);
        buf.writeBoolean(avoid);
    }

    public static final CustomPacketPayload.Type<MessageUpdateMerchantMemory> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_update_merchant_memory"));

    @Override
    public CustomPacketPayload.Type<MessageUpdateMerchantMemory> type() {
        return TYPE;
    }
}
