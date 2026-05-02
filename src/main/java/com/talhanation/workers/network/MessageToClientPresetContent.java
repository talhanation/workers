package com.talhanation.workers.network;

import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

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
    public Dist getExecutingSide() { return Dist.CLIENT; }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(NetworkEvent.Context context) {
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
