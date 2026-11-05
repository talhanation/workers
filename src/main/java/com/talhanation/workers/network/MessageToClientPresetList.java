package com.talhanation.workers.network;

import com.talhanation.workers.client.WorkersClientManager;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Server → Client: delivers the list of available preset file names. */
public class MessageToClientPresetList implements Message<MessageToClientPresetList> {

    public List<String> names;

    public MessageToClientPresetList() { this.names = new ArrayList<>(); }
    public MessageToClientPresetList(List<String> names) { this.names = names; }

    @Override
    public PacketFlow getExecutingSide() { return PacketFlow.CLIENTBOUND; }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(IPayloadContext context) {
        WorkersClientManager.serverBuildingPresetNames = new ArrayList<>(names);
    }

    @Override
    public MessageToClientPresetList fromBytes(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.names = new ArrayList<>(size);
        for (int i = 0; i < size; i++) names.add(buf.readUtf());
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(names.size());
        names.forEach(buf::writeUtf);
    }

    public static final CustomPacketPayload.Type<MessageToClientPresetList> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_to_client_preset_list"));

    @Override
    public CustomPacketPayload.Type<MessageToClientPresetList> type() {
        return TYPE;
    }
}
