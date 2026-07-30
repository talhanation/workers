package com.talhanation.workers.network;

import com.talhanation.workers.network.compat.WorkersMessage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;
import com.talhanation.workers.network.compat.WorkersNetworkContext;

public class MessageToClientPresetContent implements WorkersMessage<MessageToClientPresetContent> {

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
    public void executeClientSide(WorkersNetworkContext context) {
        if (pendingCallback != null) {
            pendingCallback.accept(this);
            pendingCallback = null;
        }
    }

    @Override
    public MessageToClientPresetContent fromBytes(FriendlyByteBuf buf) {
        this.presetName = buf.readUtf();
        this.nbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(presetName);
        buf.writeNbt(nbt);
    }
}
