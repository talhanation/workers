package com.talhanation.workers.network;

import com.talhanation.workers.client.WorkersClientManager;
import com.talhanation.workers.network.compat.WorkersMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;
import com.talhanation.workers.network.compat.WorkersNetworkContext;

import java.util.ArrayList;
import java.util.List;

/** Server → Client: delivers the list of available preset file names. */
public class MessageToClientPresetList implements WorkersMessage<MessageToClientPresetList> {

    public List<String> names;

    public MessageToClientPresetList() { this.names = new ArrayList<>(); }
    public MessageToClientPresetList(List<String> names) { this.names = names; }

    @Override
    public PacketFlow getExecutingSide() { return PacketFlow.CLIENTBOUND; }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(WorkersNetworkContext context) {
        WorkersClientManager.serverBuildingPresetNames = new ArrayList<>(names);
    }

    @Override
    public MessageToClientPresetList fromBytes(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.names = new ArrayList<>(size);
        for (int i = 0; i < size; i++) names.add(buf.readUtf());
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(names.size());
        names.forEach(buf::writeUtf);
    }
}
