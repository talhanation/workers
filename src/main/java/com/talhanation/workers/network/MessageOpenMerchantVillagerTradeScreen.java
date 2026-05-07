package com.talhanation.workers.network;

import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.world.WorkersMerchantTrade;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageOpenMerchantVillagerTradeScreen implements Message<MessageOpenMerchantVillagerTradeScreen> {

    private UUID player;
    private UUID merchantUuid;
    private CompoundTag nbt;

    public MessageOpenMerchantVillagerTradeScreen() {
        this.player = new UUID(0L, 0L);
    }

    public MessageOpenMerchantVillagerTradeScreen(Player player, UUID merchantUuid, WorkersMerchantTrade trade) {
        this.player      = player.getUUID();
        this.merchantUuid = merchantUuid;
        this.nbt         = trade.toNbt();
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        if (!context.getSender().getUUID().equals(player)) {
            return;
        }
        ServerPlayer player = context.getSender();
        player.getCommandSenderWorld()
                .getEntitiesOfClass(MerchantEntity.class, player.getBoundingBox().inflate(32.0D),
                        v -> v.getUUID().equals(this.merchantUuid))
                .stream()
                .findAny()
                .ifPresent(merchant -> merchant.openVillagerTradeGUI(player, WorkersMerchantTrade.fromNbt(nbt)));
    }

    @Override
    public MessageOpenMerchantVillagerTradeScreen fromBytes(FriendlyByteBuf buf) {
        this.player       = buf.readUUID();
        this.merchantUuid = buf.readUUID();
        this.nbt          = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeUUID(this.merchantUuid);
        buf.writeNbt(this.nbt);
    }
}
