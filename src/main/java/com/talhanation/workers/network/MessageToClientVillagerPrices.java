package com.talhanation.workers.network;

import com.talhanation.workers.client.WorkersClientManager;
import com.talhanation.workers.world.VillagerPriceTable;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;

public class MessageToClientVillagerPrices implements Message<MessageToClientVillagerPrices> {
    private Map<Item, VillagerPriceTable.Entry> prices = new HashMap<>();
    private Item currency = Items.EMERALD;

    public MessageToClientVillagerPrices() {
    }

    public MessageToClientVillagerPrices(Map<Item, VillagerPriceTable.Entry> prices, Item currency) {
        this.prices = prices;
        this.currency = currency;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(NetworkEvent.Context context) {
        WorkersClientManager.villagerPrices = this.prices;
        VillagerPriceTable.setCurrency(this.currency);
    }

    @Override
    public MessageToClientVillagerPrices fromBytes(FriendlyByteBuf buf) {
        this.prices = new HashMap<>();
        this.currency = BuiltInRegistries.ITEM.get(buf.readResourceLocation());
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buf.readResourceLocation();
            VillagerPriceTable.Entry entry = VillagerPriceTable.Entry.read(buf);
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != Items.AIR) this.prices.put(item, entry);
        }
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(this.currency));
        buf.writeVarInt(this.prices.size());
        for (Map.Entry<Item, VillagerPriceTable.Entry> entry : this.prices.entrySet()) {
            buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(entry.getKey()));
            entry.getValue().write(buf);
        }
    }
}
