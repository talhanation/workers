package com.talhanation.workers.network;

import com.talhanation.workers.entities.workarea.WorkshopArea;
import com.talhanation.workers.world.CraftOrder;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageUpdateWorkshopOrder implements Message<MessageUpdateWorkshopOrder> {

    private UUID workshopUuid;
    private CompoundTag nbt;
    private int action;

    public MessageUpdateWorkshopOrder() {}

    public MessageUpdateWorkshopOrder(UUID workshopUuid, CraftOrder order, Action action) {
        this.workshopUuid = workshopUuid;
        this.nbt = order.toNbt();
        this.action = action.ordinal();
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || nbt == null) return;

        player.getCommandSenderWorld().getEntitiesOfClass(WorkshopArea.class, player.getBoundingBox().inflate(64.0D),
                        area -> area.getUUID().equals(this.workshopUuid))
                .stream()
                .findAny()
                .ifPresent(workshop -> {
                    if (!workshop.canPlayerSee(player)) return;
                    update(workshop, player);
                });
    }

    private void update(WorkshopArea workshop, ServerPlayer player) {
        CraftOrder order = CraftOrder.fromNbt(nbt);
        CraftOrder existing = workshop.getOrder(order.uuid);
        Action type = Action.fromIndex(action);

        // Governor orders can only be switched on and off by players
        if (existing != null && existing.isGovernorOrder() && type != Action.TOGGLE_ENABLED) {
            player.sendSystemMessage(Component.translatable("chat.workers.workshop.governorOrderLocked"));
            return;
        }

        switch (type) {
            case ADD_OR_UPDATE -> {
                order.clampValues();
                order.source = CraftOrder.Source.PLAYER;
                order.sourceKey = null;
                if (!order.resolveRecipe(player.getCommandSenderWorld())) {
                    player.sendSystemMessage(Component.translatable("chat.workers.workshop.invalidRecipe"));
                    return;
                }
                if (existing != null) {
                    boolean sameRecipe = existing.recipeId != null && existing.recipeId.equals(order.recipeId);
                    order.produced = sameRecipe ? existing.produced : 0;
                    order.stock = sameRecipe ? existing.stock : 0;
                }
                order.status = CraftOrder.Status.OK;
                order.missing.clear();
                if (!workshop.addOrUpdateOrder(order)) {
                    player.sendSystemMessage(Component.translatable("chat.workers.workshop.maxOrders", WorkshopArea.MAX_ORDERS));
                }
            }
            case REMOVE -> {
                if (existing != null) workshop.removeOrder(existing.uuid);
            }
            case TOGGLE_ENABLED -> {
                if (existing == null) return;
                existing.enabled = !existing.enabled;
                existing.status = existing.enabled ? CraftOrder.Status.OK : CraftOrder.Status.DISABLED;
                workshop.onOrdersChanged(true);
            }
            case RESET_PROGRESS -> {
                if (existing == null) return;
                existing.produced = 0;
                if (existing.status == CraftOrder.Status.DONE) existing.status = CraftOrder.Status.OK;
                workshop.onOrdersChanged(true);
            }
        }
    }

    @Override
    public MessageUpdateWorkshopOrder fromBytes(FriendlyByteBuf buf) {
        this.workshopUuid = buf.readUUID();
        this.nbt = buf.readNbt();
        this.action = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.workshopUuid);
        buf.writeNbt(this.nbt);
        buf.writeInt(this.action);
    }

    public enum Action {
        ADD_OR_UPDATE,
        REMOVE,
        TOGGLE_ENABLED,
        RESET_PROGRESS;

        public static Action fromIndex(int index) {
            Action[] values = values();
            return index >= 0 && index < values.length ? values[index] : ADD_OR_UPDATE;
        }
    }
}
