package com.talhanation.workers.network;

import com.talhanation.workers.client.gui.MerchantMemoryScreen;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.world.MerchantMemory;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MessageToClientMerchantMemory implements Message<MessageToClientMerchantMemory> {

    private UUID merchantUuid;
    private List<MerchantMemory.Entry> entries = new ArrayList<>();

    public MessageToClientMerchantMemory() {}

    public MessageToClientMerchantMemory(UUID merchantUuid, List<MerchantMemory.Entry> entries) {
        this.merchantUuid = merchantUuid;
        this.entries = entries;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(NetworkEvent.Context context) {
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
    public MessageToClientMerchantMemory fromBytes(FriendlyByteBuf buf) {
        this.merchantUuid = buf.readUUID();
        this.entries = buf.readList(MerchantMemory.Entry::read);
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(merchantUuid);
        buf.writeCollection(entries, (b, entry) -> entry.write(b));
    }
}
