package com.talhanation.workers.network;

import com.talhanation.workers.client.gui.MerchantMemoryScreen;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.world.MerchantMemory;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MessageToClientMerchantMemory implements Message<MessageToClientMerchantMemory> {

    private UUID merchantUuid;
    private List<MerchantMemory.Entry> entries = new ArrayList<>();

    public MessageToClientMerchantMemory() {}

    public MessageToClientMerchantMemory(UUID merchantUuid, List<MerchantMemory.Entry> entries) {
        this.merchantUuid = merchantUuid;
        // NeoForge encodes payloads later on the netty thread: snapshot the (live) entries here
        net.minecraft.network.FriendlyByteBuf copy = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        copy.writeCollection(entries, (b, entry) -> entry.write(b));
        this.entries = copy.readList(MerchantMemory.Entry::read);
        copy.release();
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(IPayloadContext context) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        player.getCommandSenderWorld().getEntitiesOfClass(MerchantEntity.class, player.getBoundingBox().inflate(32.0D),
                        merchant -> merchant.getUUID().equals(this.merchantUuid))
                .stream()
                .findAny()
                .ifPresent(merchant -> {
                    player.closeContainer();
                    Minecraft.getInstance().setScreen(new MerchantMemoryScreen(merchant, player, entries));
                });
    }

    @Override
    public MessageToClientMerchantMemory fromBytes(RegistryFriendlyByteBuf buf) {
        this.merchantUuid = buf.readUUID();
        this.entries = buf.readList(MerchantMemory.Entry::read);
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(merchantUuid);
        buf.writeCollection(entries, (b, entry) -> entry.write(b));
    }

    public static final CustomPacketPayload.Type<MessageToClientMerchantMemory> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_to_client_merchant_memory"));

    @Override
    public CustomPacketPayload.Type<MessageToClientMerchantMemory> type() {
        return TYPE;
    }
}
