package com.talhanation.workers.network;

import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class MessageToClientPresetContent implements Message<MessageToClientPresetContent> {

    public String presetName;
    public CompoundTag nbt;

    public static java.util.function.Consumer<MessageToClientPresetContent> pendingCallback = null;

    public MessageToClientPresetContent() {}
    public MessageToClientPresetContent(String presetName, CompoundTag nbt) {
        this.presetName = presetName;
        this.nbt = nbt;
    }

    @Override
    public PacketFlow getExecutingSide() { return PacketFlow.CLIENTBOUND; }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(IPayloadContext context) {
        if (pendingCallback != null) {
            pendingCallback.accept(this);
            pendingCallback = null;
        }
    }

    @Override
    public MessageToClientPresetContent fromBytes(RegistryFriendlyByteBuf buf) {
        this.presetName = buf.readUtf();
        this.nbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(presetName);
        buf.writeNbt(nbt);
    }

    public static final CustomPacketPayload.Type<MessageToClientPresetContent> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_to_client_preset_content"));

    @Override
    public CustomPacketPayload.Type<MessageToClientPresetContent> type() {
        return TYPE;
    }
}
