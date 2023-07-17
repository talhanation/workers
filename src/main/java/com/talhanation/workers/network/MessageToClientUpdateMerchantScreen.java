package com.talhanation.workers.network;

import com.talhanation.workers.client.gui.MerchantOwnerScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;


public class MessageToClientUpdateMerchantScreen implements Message<MessageToClientUpdateMerchantScreen> {
    public List<BlockPos> waypoints;

    public MessageToClientUpdateMerchantScreen() {
    }

    public MessageToClientUpdateMerchantScreen(List<BlockPos> waypoints) {
        this.waypoints = waypoints;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        MerchantOwnerScreen.waypoints = this.waypoints;
    }

    @Override
    public MessageToClientUpdateMerchantScreen fromBytes(FriendlyByteBuf buf) {
        this.waypoints = buf.readList(FriendlyByteBuf::readBlockPos);
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeCollection(waypoints, FriendlyByteBuf::writeBlockPos);
    }
}
