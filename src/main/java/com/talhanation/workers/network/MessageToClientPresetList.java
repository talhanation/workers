package com.talhanation.workers.network;

import com.talhanation.workers.client.WorkersClientManager;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;

/** Server → Client: delivers the list of available preset file names. */
public class MessageToClientPresetList implements Message<MessageToClientPresetList> {

    public List<String> names;

    public MessageToClientPresetList() { this.names = new ArrayList<>(); }
    public MessageToClientPresetList(List<String> names) { this.names = names; }

    @Override
    public Dist getExecutingSide() { return Dist.CLIENT; }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(NetworkEvent.Context context) {
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
