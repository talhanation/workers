package com.talhanation.workers.network;

import com.talhanation.workers.client.WorkersClientManager;
import com.talhanation.workers.config.BuildMode;
import com.talhanation.workers.config.BuildPaymentMode;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

public class MessageToClientUpdateConfig implements Message<MessageToClientUpdateConfig> {
    private boolean allowWorkAreaOnlyInFactionClaim;
    private boolean allowOnlyBuildings;
    private BuildMode buildMode;
    private BuildPaymentMode buildPaymentMode;
    private ItemStack currency;
    public MessageToClientUpdateConfig() {
    }

    public MessageToClientUpdateConfig(boolean allowWorkAreaOnlyInFactionClaim, boolean allowOnlyBuildings, BuildMode buildMode, BuildPaymentMode buildPaymentMode, ItemStack currency) {
        this.allowWorkAreaOnlyInFactionClaim = allowWorkAreaOnlyInFactionClaim;
        this.allowOnlyBuildings = allowOnlyBuildings;
        this.buildMode = buildMode;
        this.buildPaymentMode = buildPaymentMode;
        this.currency = currency;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(NetworkEvent.Context context) {
        WorkersClientManager.configValueWorkAreaOnlyInFactionClaim = this.allowWorkAreaOnlyInFactionClaim;
        WorkersClientManager.configValueOnlyBuildings = this.allowOnlyBuildings;
        WorkersClientManager.buildMode = this.buildMode;
        WorkersClientManager.buildPaymentMode = this.buildPaymentMode;
        WorkersClientManager.currency = this.currency;
    }

    @Override
    public MessageToClientUpdateConfig fromBytes(FriendlyByteBuf buf) {
        this.allowWorkAreaOnlyInFactionClaim = buf.readBoolean();
        this.allowOnlyBuildings = buf.readBoolean();
        this.buildMode = buf.readEnum(BuildMode.class);
        this.buildPaymentMode = buf.readEnum(BuildPaymentMode.class);
        this.currency = buf.readItem();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.allowWorkAreaOnlyInFactionClaim);
        buf.writeBoolean(this.allowOnlyBuildings);
        buf.writeEnum(this.buildMode);
        buf.writeEnum(this.buildPaymentMode);
        buf.writeItem(this.currency);
    }

}
