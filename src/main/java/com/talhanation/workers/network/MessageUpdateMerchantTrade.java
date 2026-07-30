package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.world.WorkersMerchantTrade;
import net.minecraft.core.HolderLookup;
import com.talhanation.workers.network.compat.WorkersMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.workers.network.compat.WorkersNetworkContext;

import java.util.UUID;

public class MessageUpdateMerchantTrade implements WorkersMessage<MessageUpdateMerchantTrade> {

    public UUID merchantUuid;
    public CompoundTag nbt;
    public boolean remove;
    public MessageUpdateMerchantTrade() {}
    public MessageUpdateMerchantTrade(UUID merchantUuid, WorkersMerchantTrade trade, HolderLookup.Provider registries, boolean remove) {
        this.merchantUuid = merchantUuid;
        this.nbt = trade.toNbt(registries);
        this.remove = remove;
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

    public void update(MerchantEntity merchant){
        if(remove){
            merchant.removeTrade(WorkersMerchantTrade.fromNbt(merchant.registryAccess(), nbt));
        }
        else{
            merchant.addOrUpdateTrade(WorkersMerchantTrade.fromNbt(merchant.registryAccess(), nbt));
        }
    }

    public MessageUpdateMerchantTrade fromBytes(FriendlyByteBuf buf) {
        this.merchantUuid = buf.readUUID();
        this.nbt = buf.readNbt();
        this.remove = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(merchantUuid);
        buf.writeNbt(nbt);
        buf.writeBoolean(remove);
    }
}
