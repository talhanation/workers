package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.world.WorkersMerchantTrade;
import com.talhanation.workers.network.compat.WorkersMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.workers.network.compat.WorkersNetworkContext;

import java.util.UUID;

public class MessageUpdateMerchant implements WorkersMessage<MessageUpdateMerchant> {

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

    public void executeServerSide(WorkersNetworkContext context){
        ServerPlayer player = context.getSender();
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
    public MessageUpdateMerchant fromBytes(FriendlyByteBuf buf) {
        this.merchantUuid  = buf.readUUID();
        this.isCreative    = buf.readBoolean();
        this.isTrading     = buf.readBoolean();
        this.dailyRefresh  = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(merchantUuid);
        buf.writeBoolean(this.isCreative);
        buf.writeBoolean(this.isTrading);
        buf.writeBoolean(this.dailyRefresh);
    }
}
