package com.talhanation.workers;

import com.talhanation.workers.entities.workarea.WorkshopArea;
import com.talhanation.workers.world.CraftOrder;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

import java.util.List;

/**
 * Workshop events on the {@code MinecraftForge.EVENT_BUS}, server-side only.
 * Meant for the Governor compat: he can react when one of his orders is done or blocked.
 *
 * <pre>
 *   {@code @SubscribeEvent}
 *   public void onOrderBlocked(WorkshopEvent.OrderBlocked event) {
 *       // e.g. notify the owner which items are missing
 *   }
 * </pre>
 */
public abstract class WorkshopEvent extends Event {

    private final WorkshopArea workshop;
    private final CraftOrder order;

    protected WorkshopEvent(WorkshopArea workshop, CraftOrder order) {
        this.workshop = workshop;
        this.order = order;
    }

    public WorkshopArea getWorkshop() {
        return workshop;
    }

    public CraftOrder getOrder() {
        return order;
    }

    /** Fired when an order reached its amount (ONCE) or its amount of the day (DAILY). */
    public static class OrderCompleted extends WorkshopEvent {
        public OrderCompleted(WorkshopArea workshop, CraftOrder order) {
            super(workshop, order);
        }
    }

    /** Fired once when an order can not be produced anymore because items are missing. */
    public static class OrderBlocked extends WorkshopEvent {
        private final List<ItemStack> missing;

        public OrderBlocked(WorkshopArea workshop, CraftOrder order, List<ItemStack> missing) {
            super(workshop, order);
            this.missing = missing;
        }

        public List<ItemStack> getMissing() {
            return missing;
        }
    }
}
