package com.talhanation.workers.inventory;

import com.talhanation.workers.entities.workarea.WorkshopArea;
import com.talhanation.workers.init.ModMenuTypes;
import com.talhanation.workers.world.CraftOrder;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Edit screen of a workshop order: a 3x3 ghost grid for crafting and a single ghost slot for smelting.
 * Ghost slots only copy the carried item (count 1), nothing is taken from the player.
 */
public class WorkshopEditOrderContainer extends ContainerBase {

    private final WorkshopArea workshopArea;
    private final CraftOrder order;
    private final SimpleContainer gridContainer;
    private final SimpleContainer smeltContainer;
    public boolean smeltMode;

    public WorkshopEditOrderContainer(int id, WorkshopArea workshopArea, Inventory playerInventory, CraftOrder order) {
        super(ModMenuTypes.WORKSHOP_EDIT_ORDER_CONTAINER_TYPE.get(), id, playerInventory, new SimpleContainer(0));
        this.workshopArea = workshopArea;
        this.order = order;
        this.smeltMode = order.type == CraftOrder.OrderType.SMELTING;
        this.gridContainer = new SimpleContainer(CraftOrder.GRID_SIZE);
        this.smeltContainer = new SimpleContainer(1);

        if (smeltMode) {
            this.smeltContainer.setItem(0, order.grid[0].copy());
        }
        else {
            for (int i = 0; i < CraftOrder.GRID_SIZE; i++) {
                this.gridContainer.setItem(i, order.grid[i].copy());
            }
        }

        addPlayerInventorySlots();
        setUpSlots();
    }

    public void setUpSlots() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addGhostSlot(gridContainer, row * 3 + col, 30 + col * 18, 17 + row * 18, false);
            }
        }
        addGhostSlot(smeltContainer, 0, 56, 17, true);
    }

    public WorkshopArea getWorkshopArea() {
        return workshopArea;
    }

    public CraftOrder getOrder() {
        return order;
    }

    /** The grid of the active mode, smelting uses only index 0. */
    public ItemStack[] getGrid() {
        ItemStack[] grid = new ItemStack[CraftOrder.GRID_SIZE];
        for (int i = 0; i < CraftOrder.GRID_SIZE; i++) {
            grid[i] = smeltMode ? ItemStack.EMPTY : gridContainer.getItem(i).copy();
        }
        if (smeltMode) grid[0] = smeltContainer.getItem(0).copy();
        return grid;
    }

    /** Switches between crafting (0) and smelting (1), sent by the screen via handleInventoryButtonClick so both sides match. */
    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id != 0 && id != 1) return false;
        this.smeltMode = id == 1;
        return true;
    }

    @Override
    public int getInvOffset() {
        return 0;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    public void addGhostSlot(SimpleContainer slotContainer, int index, int x, int y, boolean smeltSlot) {
        this.addSlot(new Slot(slotContainer, index, x, y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack itemStack) {
                if (isActive()) slotContainer.setItem(this.getSlotIndex(), itemStack.copyWithCount(1));
                return false;
            }

            @Override
            public boolean mayPickup(@NotNull Player player) {
                if (!isActive()) return false;
                ItemStack carried = WorkshopEditOrderContainer.this.getCarried();
                slotContainer.setItem(this.getSlotIndex(), carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
                return false;
            }

            @Override
            public boolean isActive() {
                return smeltSlot == WorkshopEditOrderContainer.this.smeltMode;
            }
        });
    }
}
