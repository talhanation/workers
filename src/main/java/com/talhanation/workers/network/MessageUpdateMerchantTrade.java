package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.world.VillagerPriceTable;
import com.talhanation.workers.world.WorkersMerchantTrade;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageUpdateMerchantTrade implements Message<MessageUpdateMerchantTrade> {

    public UUID merchantUuid;
    public CompoundTag nbt;
    public boolean remove;
    public MessageUpdateMerchantTrade() {}
    public MessageUpdateMerchantTrade(UUID merchantUuid, WorkersMerchantTrade trade, boolean remove) {
        this.merchantUuid = merchantUuid;
        this.nbt = trade.toNbt();
        this.remove = remove;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
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
            merchant.removeTrade(WorkersMerchantTrade.fromNbt(nbt));
        }
        else{
            WorkersMerchantTrade trade = WorkersMerchantTrade.fromNbt(nbt);
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
