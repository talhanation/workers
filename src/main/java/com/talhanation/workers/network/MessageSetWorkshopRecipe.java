package com.talhanation.workers.network;

import com.talhanation.workers.inventory.WorkshopEditOrderContainer;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

/** Fills the ghost grid of the open workshop order with a recipe picked in the item search. */
public class MessageSetWorkshopRecipe implements Message<MessageSetWorkshopRecipe> {

    private ResourceLocation recipe;

    public MessageSetWorkshopRecipe() {}

    public MessageSetWorkshopRecipe(ResourceLocation recipe) {
        this.recipe = recipe;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        if (!(player.containerMenu instanceof WorkshopEditOrderContainer menu)) return;

        player.getServer().getRecipeManager().byKey(recipe).ifPresent(found -> {
            menu.applyRecipe(found, player);
            menu.broadcastChanges();
        });
    }

    @Override
    public MessageSetWorkshopRecipe fromBytes(FriendlyByteBuf buf) {
        this.recipe = buf.readResourceLocation();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(recipe);
    }
}
