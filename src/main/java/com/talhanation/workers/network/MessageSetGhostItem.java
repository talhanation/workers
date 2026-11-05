package com.talhanation.workers.network;

import com.talhanation.workers.inventory.MerchantAddEditTradeContainer;
import com.talhanation.workers.inventory.MerchantAddEditVillagerTradeContainer;
import com.talhanation.workers.world.VillagerPriceTable;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sets a ghost slot of a trade edit screen to an item picked in the item search. Only item and count, no NBT. */
public class MessageSetGhostItem implements Message<MessageSetGhostItem> {

    private int slotIndex;
    private ResourceLocation item;
    private int count;

    public MessageSetGhostItem() {}

    public MessageSetGhostItem(int slotIndex, Item item, int count) {
        this.slotIndex = slotIndex;
        this.item = BuiltInRegistries.ITEM.getKey(item);
        this.count = count;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());
        if (player == null) return;

        AbstractContainerMenu menu = player.containerMenu;
        boolean villager = menu instanceof MerchantAddEditVillagerTradeContainer;
        if (!villager && !(menu instanceof MerchantAddEditTradeContainer)) return;
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) return;
        Slot slot = menu.getSlot(slotIndex);
        // only ghost slots, never the player inventory
        if (slot.container == player.getInventory()) return;

        Item picked = BuiltInRegistries.ITEM.get(item);
        if (picked == Items.AIR) return;
        if (villager && picked == VillagerPriceTable.getCurrency()) return;
        int amount = villager ? 1 : Mth.clamp(count, 1, new ItemStack(picked).getMaxStackSize());

        slot.container.setItem(slot.getContainerSlot(), new ItemStack(picked, amount));
        menu.broadcastChanges();
    }

    @Override
    public MessageSetGhostItem fromBytes(RegistryFriendlyByteBuf buf) {
        this.slotIndex = buf.readVarInt();
        this.item = buf.readResourceLocation();
        this.count = buf.readVarInt();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(slotIndex);
        buf.writeResourceLocation(item);
        buf.writeVarInt(count);
    }

    public static final CustomPacketPayload.Type<MessageSetGhostItem> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_set_ghost_item"));

    @Override
    public CustomPacketPayload.Type<MessageSetGhostItem> type() {
        return TYPE;
    }
}
