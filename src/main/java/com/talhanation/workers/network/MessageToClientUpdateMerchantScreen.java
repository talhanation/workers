package com.talhanation.workers.network;

import com.talhanation.workers.client.gui.MerchantOwnerScreen;
import com.talhanation.workers.client.gui.MerchantWaypointScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;


public class MessageToClientUpdateMerchantScreen implements Message<MessageToClientUpdateMerchantScreen> {
    public List<BlockPos> waypoints;
    public List<ItemStack> waypointItems;
    public List<Integer> currentTrades;
    public List<Integer> limits;
    public boolean isStarted;
    public boolean isReturning;
    public boolean isStopped;
    public MessageToClientUpdateMerchantScreen() {
    }

    public MessageToClientUpdateMerchantScreen(List<BlockPos> waypoints, List<ItemStack> waypointItems, List<Integer> currentTrades, List<Integer> limits, boolean isStarted, boolean isReturning) {
        this.waypoints = waypoints;
        this.waypointItems = waypointItems;
        this.currentTrades = currentTrades;
        this.limits = limits;
        this.isStarted = isStarted;
        this.isStopped = !isReturning && !isStarted;
        this.isReturning = isReturning;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        MerchantWaypointScreen.waypoints = this.waypoints;
        MerchantWaypointScreen.waypointItems = this.waypointItems;
        MerchantOwnerScreen.currentTrades = this.currentTrades;
        MerchantOwnerScreen.limits = this.limits;
        MerchantWaypointScreen.isReturning = this.isReturning;
        MerchantWaypointScreen.isStarted = this.isStarted;
        MerchantWaypointScreen.isStopped = this.isStopped;
    }

    @Override
    public MessageToClientUpdateMerchantScreen fromBytes(FriendlyByteBuf buf) {
        this.waypoints = buf.readList(FriendlyByteBuf::readBlockPos);
        this.waypointItems = buf.readList(FriendlyByteBuf::readItem);
        this.currentTrades = buf.readList(FriendlyByteBuf::readInt);
        this.limits = buf.readList(FriendlyByteBuf::readInt);
        this.isReturning = buf.readBoolean();
        this.isStarted = buf.readBoolean();
        this.isStopped = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeCollection(waypoints, FriendlyByteBuf::writeBlockPos);
        buf.writeCollection(waypointItems, FriendlyByteBuf::writeItem);
        buf.writeCollection(currentTrades, FriendlyByteBuf::writeInt);
        buf.writeCollection(limits, FriendlyByteBuf::writeInt);
        buf.writeBoolean(this.isStarted);
        buf.writeBoolean(this.isStopped);
        buf.writeBoolean(this.isReturning);
    }
}
