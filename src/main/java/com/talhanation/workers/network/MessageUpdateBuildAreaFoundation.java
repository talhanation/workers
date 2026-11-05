package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.BuildArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Foundation mode of a build area: on/off and the fill item (empty = chosen from the ground). */
public class MessageUpdateBuildAreaFoundation implements Message<MessageUpdateBuildAreaFoundation> {

    public UUID uuid;
    public boolean foundation;
    public ItemStack fillItem = ItemStack.EMPTY;

    public MessageUpdateBuildAreaFoundation() {}

    public MessageUpdateBuildAreaFoundation(UUID uuid, boolean foundation, ItemStack fillItem) {
        this.uuid = uuid;
        this.foundation = foundation;
        this.fillItem = fillItem == null ? ItemStack.EMPTY : fillItem.copy(); // NeoForge encodes payloads later on the netty thread: snapshot mutable data here
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());
        if (player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(BuildArea.class,
                        player.getBoundingBox().inflate(32.0D),
                        v -> v.getUUID().equals(this.uuid))
                .stream()
                .findAny()
                .filter(area -> area.canPlayerSee(player))
                .ifPresent(area -> {
                    area.setFoundation(this.foundation);
                    // only auto or a block of the palette
                    area.setFillItem(BuildArea.isValidFillItem(this.fillItem) ? this.fillItem : ItemStack.EMPTY);
                });
    }

    @Override
    public MessageUpdateBuildAreaFoundation fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.foundation = buf.readBoolean();
        this.fillItem = net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeBoolean(foundation);
        net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, fillItem);
    }

    public static final CustomPacketPayload.Type<MessageUpdateBuildAreaFoundation> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_update_build_area_foundation"));

    @Override
    public CustomPacketPayload.Type<MessageUpdateBuildAreaFoundation> type() {
        return TYPE;
    }
}
