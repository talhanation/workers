package com.talhanation.workers.inventory;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.init.ModMenuTypes;

import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MerchantInventoryContainer extends ContainerBase {

    private final Container workerInventory;
    private final Container workerTradeInventory;
    private final MerchantEntity merchant;
    private final int[] PRICE_ID = new int[] {
            0, 2, 4, 6
    };

    private final int[] TRADE_ID = new int[] {
            1, 3, 5, 7
    };

    public MerchantInventoryContainer(int id, MerchantEntity merchant, Inventory playerInventory) {
        super(ModMenuTypes.MERCHANT_OWNER_CONTAINER_TYPE.get(), id, playerInventory, merchant.getInventory());
        this.merchant = merchant;
        this.workerTradeInventory = merchant.getTradeInventory();
        this.workerInventory = merchant.getInventory();
        addWorkerTradeSlots();
        addWorkerPriceSlots();
        addPlayerInventorySlots();
        addWorkerInventorySlots();
    }

    @Override
    public int getInvOffset() {
        return 56;
    }

    public void addWorkerInventorySlots() {
        for (int k = 0; k < 2; ++k) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(workerInventory, l + k * 9, 8 + l * 18, 3 + 18 * 5 + k * 18));
            }
        }
    }

    public void addWorkerPriceSlots() {
        for (int k = 0; k < 4; ++k) {
            this.addSlot(new Slot(workerTradeInventory, PRICE_ID[k], 27 + 8 + 18, 18 - 2 + k * 18) {
                @Override
                public boolean mayPlace(ItemStack itemStack) {
                    return true;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return true;
                }
            });
        }
    }

    public void addWorkerTradeSlots() {
        for (int k = 0; k < 4; ++k) {
            this.addSlot(new Slot(workerTradeInventory, TRADE_ID[k], 27 + 8 + 18 * 4, 18 - 2 + k * 18) {
                @Override
                public boolean mayPlace(ItemStack itemStack) {
                    return true;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return true;
                }
            });
        }
    }

    public AbstractWorkerEntity getWorker() {
        return merchant;
    }

    @Override
    public boolean stillValid(Player playerIn) {
        return this.workerInventory.stillValid(playerIn) && this.merchant.isAlive()
                && this.merchant.distanceTo(playerIn) < 8.0F;
    }

    @Override
    public void removed(Player playerIn) {
        super.removed(playerIn);
    }
}
