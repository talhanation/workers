package com.talhanation.workers.network;

import com.talhanation.workers.client.gui.WorkerHireScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageToClientUpdateHireScreen implements Message<MessageToClientUpdateHireScreen> {
    public ItemStack currency;
    public int amount;

    public MessageToClientUpdateHireScreen() {

    }

    public MessageToClientUpdateHireScreen(ItemStack currency, int amount) {
        this.currency = currency;
        this.amount = amount;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        WorkerHireScreen.currency = this.currency;
        WorkerHireScreen.amount = this.amount;
    }

    @Override
    public MessageToClientUpdateHireScreen fromBytes(FriendlyByteBuf buf) {
        this.currency = buf.readItem();
        this.amount = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeItemStack(currency, false);
        buf.writeInt(amount);
    }

}