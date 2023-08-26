package com.talhanation.workers.network;

import com.talhanation.workers.client.gui.MerchantOwnerScreen;
import com.talhanation.workers.client.gui.MerchantWaypointScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;


public class MessageToClientUpdateMerchantScreen implements Message<MessageToClientUpdateMerchantScreen> {
    public List<BlockPos> waypoints;
    public List<Integer> currentTrades;
    public List<Integer> limits;
    public MessageToClientUpdateMerchantScreen() {
    }

    public MessageToClientUpdateMerchantScreen(List<BlockPos> waypoints, List<Integer> currentTrades, List<Integer> limits) {
        this.waypoints = waypoints;
        this.currentTrades = currentTrades;
        this.limits = limits;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        MerchantWaypointScreen.waypoints = this.waypoints;
        MerchantOwnerScreen.currentTrades = this.currentTrades;
        MerchantOwnerScreen.limits = this.limits;
    }

    @Override
    public MessageToClientUpdateMerchantScreen fromBytes(FriendlyByteBuf buf) {
        this.waypoints = buf.readList(FriendlyByteBuf::readBlockPos);
        this.currentTrades = buf.readList(FriendlyByteBuf::readInt);
        this.limits = buf.readList(FriendlyByteBuf::readInt);
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeCollection(waypoints, FriendlyByteBuf::writeBlockPos);
        buf.writeCollection(currentTrades, FriendlyByteBuf::writeInt);
        buf.writeCollection(limits, FriendlyByteBuf::writeInt);
    }
}
