package com.talhanation.workers.network;

import net.minecraft.core.HolderLookup;
import com.talhanation.workers.entities.workarea.WorkshopArea;
import com.talhanation.workers.world.CraftOrder;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class MessageOpenWorkshopEditOrderScreen implements Message<MessageOpenWorkshopEditOrderScreen> {

    private UUID workshopUuid;
    private CompoundTag nbt;

    public MessageOpenWorkshopEditOrderScreen() {}

    public MessageOpenWorkshopEditOrderScreen(UUID workshopUuid, CraftOrder order, HolderLookup.Provider provider) {
        this.workshopUuid = workshopUuid;
        this.nbt = order.toNbt(provider);
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());
        if (player == null || nbt == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(WorkshopArea.class, player.getBoundingBox().inflate(64.0D),
                        area -> area.getUUID().equals(this.workshopUuid))
                .stream()
                .findAny()
                .ifPresent(workshop -> {
                    if (!workshop.canPlayerSee(player)) return;
                    workshop.openEditOrderGUI(player, CraftOrder.fromNbt(nbt, player.registryAccess()));
                });
    }

    @Override
    public MessageOpenWorkshopEditOrderScreen fromBytes(RegistryFriendlyByteBuf buf) {
        this.workshopUuid = buf.readUUID();
        this.nbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.workshopUuid);
        buf.writeNbt(this.nbt);
    }

    public static final CustomPacketPayload.Type<MessageOpenWorkshopEditOrderScreen> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_open_workshop_edit_order_screen"));

    @Override
    public CustomPacketPayload.Type<MessageOpenWorkshopEditOrderScreen> type() {
        return TYPE;
    }
}
