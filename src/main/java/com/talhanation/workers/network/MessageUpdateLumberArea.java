package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.LumberArea;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

import static com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity.DONE_TIME;

public class MessageUpdateLumberArea implements Message<MessageUpdateLumberArea> {

    public UUID uuid;
    public ItemStack saplingItem;
    public boolean shearLeaves;
    public boolean stripLogs;
    public boolean replant;
    public MessageUpdateLumberArea() {

    }

    public MessageUpdateLumberArea(UUID uuid, ItemStack saplingItem, boolean shearLeaves, boolean stripLogs, boolean replant) {
        this.uuid = uuid;
        this.saplingItem = saplingItem.copy();
        this.shearLeaves = shearLeaves;
        this.stripLogs = stripLogs;
        this.replant = replant;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context){
        ServerPlayer player = ((ServerPlayer) context.player());
        if(player == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(LumberArea.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v
                        .getUUID()
                        .equals(this.uuid))
                .stream()
                .findAny()
                .ifPresent(this::update);

    }

    public void update(LumberArea lumberArea){
        ItemStack itemStack = this.saplingItem;
        lumberArea.setSaplingStack(itemStack);
        lumberArea.setShearLeaves(this.shearLeaves);
        lumberArea.setStripLogs(this.stripLogs);
        lumberArea.setReplant(this.replant);

        lumberArea.time += DONE_TIME;
    }

    public MessageUpdateLumberArea fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.saplingItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        this.shearLeaves = buf.readBoolean();
        this.stripLogs = buf.readBoolean();
        this.replant = buf.readBoolean();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, saplingItem);
        buf.writeBoolean(shearLeaves);
        buf.writeBoolean(stripLogs);
        buf.writeBoolean(replant);
    }

    public static final CustomPacketPayload.Type<MessageUpdateLumberArea> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_update_lumber_area"));

    @Override
    public CustomPacketPayload.Type<MessageUpdateLumberArea> type() {
        return TYPE;
    }
}
