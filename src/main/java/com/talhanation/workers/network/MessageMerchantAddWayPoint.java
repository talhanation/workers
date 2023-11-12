package com.talhanation.workers.network;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.MerchantEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

public class MessageMerchantAddWayPoint implements Message<MessageMerchantAddWayPoint> {
    private UUID worker;

    public MessageMerchantAddWayPoint() {
    }

    public MessageMerchantAddWayPoint(UUID recruit) {
        this.worker = recruit;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {

        ServerPlayer player = context.getSender();
        player.level.getEntitiesOfClass(MerchantEntity.class, player.getBoundingBox()
                        .inflate(100.0D), v -> v
                        .getUUID()
                        .equals(this.worker))
                .stream()
                .filter(MerchantEntity::isAlive)
                .findAny()
                .ifPresent(merchant -> this.addWayPoint(player, merchant));
    }

    private void addWayPoint(ServerPlayer player, MerchantEntity merchant){
        BlockPos pos;
        if(merchant.isInWater()) pos = merchant.getOnPos().above();
        else pos = merchant.getOnPos();
        //merchant.tellPlayer(player, Component.literal("Pos: " + pos + " was added."));
        BlockState state = merchant.getCommandSenderWorld().getBlockState(pos);
        if(state.isAir()) pos = pos.below();

        merchant.setStartPos(pos); // adds waypoint without starting work

        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToClientUpdateMerchantScreen(merchant.WAYPOINTS, merchant.WAYPOINT_ITEMS, merchant.getCurrentTrades(), merchant.getTradeLimits(), merchant.getTraveling(), merchant.getReturning()));
    }

    public MessageMerchantAddWayPoint fromBytes(FriendlyByteBuf buf) {
        this.worker = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.worker);
    }

}