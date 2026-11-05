package com.talhanation.workers.network;

import com.talhanation.workers.inventory.WorkshopEditOrderContainer;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Fills the ghost grid of the open workshop order with a recipe picked in the item search. */
public class MessageSetWorkshopRecipe implements Message<MessageSetWorkshopRecipe> {

    private ResourceLocation recipe;

    public MessageSetWorkshopRecipe() {}

    public MessageSetWorkshopRecipe(ResourceLocation recipe) {
        this.recipe = recipe;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());
        if (player == null) return;
        if (!(player.containerMenu instanceof WorkshopEditOrderContainer menu)) return;

        player.getServer().getRecipeManager().byKey(recipe).ifPresent(found -> {
            menu.applyRecipe(found.value(), player);
            menu.broadcastChanges();
        });
    }

    @Override
    public MessageSetWorkshopRecipe fromBytes(RegistryFriendlyByteBuf buf) {
        this.recipe = buf.readResourceLocation();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(recipe);
    }

    public static final CustomPacketPayload.Type<MessageSetWorkshopRecipe> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("workers", "message_set_workshop_recipe"));

    @Override
    public CustomPacketPayload.Type<MessageSetWorkshopRecipe> type() {
        return TYPE;
    }
}
