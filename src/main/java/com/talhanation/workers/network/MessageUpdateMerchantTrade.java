package com.talhanation.workers.network;

import net.minecraft.core.HolderLookup;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.world.VillagerPriceTable;
import com.talhanation.workers.world.WorkersMerchantTrade;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class MessageUpdateMerchantTrade implements Message<MessageUpdateMerchantTrade> {

    public UUID merchantUuid;
    public CompoundTag nbt;
    public boolean remove;
    public MessageUpdateMerchantTrade() {}
    public MessageUpdateMerchantTrade(UUID merchantUuid, WorkersMerchantTrade trade, boolean remove, HolderLookup.Provider provider) {
        this.merchantUuid = merchantUuid;
        this.nbt = trade.toNbt(provider);
        this.remove = remove;
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
                .ifPresent(merchant -> this.update(player, merchant));

    }

    public void update(ServerPlayer player, MerchantEntity merchant){
        WorkersMerchantTrade incoming = WorkersMerchantTrade.fromNbt(nbt, player.registryAccess());
        WorkersMerchantTrade existing = null;
        for(WorkersMerchantTrade t : merchant.getTrades()){
            if(t.uuid.equals(incoming.uuid)) existing = t;
        }
        // Governor trades can only be switched on and off by players
        if(existing != null && existing.isGovernorTrade()){
            if(remove || existing.enabled == incoming.enabled){
                player.sendSystemMessage(Component.translatable("chat.workers.merchant.governorTradeLocked"));
                return;
            }
            existing.enabled = incoming.enabled;
            merchant.addOrUpdateTrade(existing);
            return;
        }

        if(remove){
            merchant.removeTrade(WorkersMerchantTrade.fromNbt(nbt, player.registryAccess()));
        }
        else{
            WorkersMerchantTrade trade = incoming;
            // never trust the client: trades from the GUI are always player trades
            trade.source = WorkersMerchantTrade.Source.PLAYER;
            trade.sourceKey = null;
            if(trade.isVillagerTrade) {
                // the villager currency can not be the trade item, villager trades always pay with it
                if(trade.tradeItem.is(VillagerPriceTable.getCurrency())) return;
                trade.currencyItem = VillagerPriceTable.getCurrencyStack();
                trade.allowDamagedCurrency = false;
                trade.useStorage = true;
                if(!trade.tradeItem.isEmpty()) trade.tradeItem = trade.tradeItem.copyWithCount(1);
            }
            merchant.addOrUpdateTrade(trade);
        }
    }

    public MessageUpdateMerchantTrade fromBytes(RegistryFriendlyByteBuf buf) {
        this.merchantUuid = buf.readUUID();
        this.nbt = buf.readNbt();
        this.remove = buf.readBoolean();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(merchantUuid);
        buf.writeNbt(nbt);
        buf.writeBoolean(remove);
    }

    public static final CustomPacketPayload.Type<MessageUpdateMerchantTrade> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_update_merchant_trade"));

    @Override
    public CustomPacketPayload.Type<MessageUpdateMerchantTrade> type() {
        return TYPE;
    }
}
