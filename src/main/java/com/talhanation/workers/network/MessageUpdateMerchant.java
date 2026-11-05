package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.world.WorkersMerchantTrade;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class MessageUpdateMerchant implements Message<MessageUpdateMerchant> {

    public UUID merchantUuid;
    public boolean isCreative;
    public boolean isTrading;
    public boolean dailyRefresh;
    public MessageUpdateMerchant() {}
    public MessageUpdateMerchant(UUID merchantUuid, boolean isCreative, boolean isTrading) {
        this.merchantUuid = merchantUuid;
        this.isCreative = isCreative;
        this.isTrading = isTrading;
    }
    public MessageUpdateMerchant(UUID merchantUuid, boolean isCreative, boolean isTrading, boolean dailyRefresh) {
        this.merchantUuid  = merchantUuid;
        this.isCreative    = isCreative;
        this.isTrading     = isTrading;
        this.dailyRefresh  = dailyRefresh;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context){
        ServerPlayer player = ((ServerPlayer) context.player());
        if(player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(MerchantEntity.class, player.getBoundingBox()
                        .inflate(32.0D), v -> v
                        .getUUID()
                        .equals(this.merchantUuid))
                .stream()
                .findAny()
                .ifPresent(this::update);

    }

    private void update(MerchantEntity merchant){
        merchant.setCreative(this.isCreative);
        merchant.setTrading(this.isTrading);
        merchant.setDailyRefresh(this.dailyRefresh);
    }
    public MessageUpdateMerchant fromBytes(RegistryFriendlyByteBuf buf) {
        this.merchantUuid  = buf.readUUID();
        this.isCreative    = buf.readBoolean();
        this.isTrading     = buf.readBoolean();
        this.dailyRefresh  = buf.readBoolean();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(merchantUuid);
        buf.writeBoolean(this.isCreative);
        buf.writeBoolean(this.isTrading);
        buf.writeBoolean(this.dailyRefresh);
    }

    public static final CustomPacketPayload.Type<MessageUpdateMerchant> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_update_merchant"));

    @Override
    public CustomPacketPayload.Type<MessageUpdateMerchant> type() {
        return TYPE;
    }
}
