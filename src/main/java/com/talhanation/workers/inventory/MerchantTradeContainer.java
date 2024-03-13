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

public class MerchantTradeContainer extends ContainerBase {
    private final Container workerInventory;
    private final MerchantEntity merchant;
    private final int[] PRICE_ID = new int[] {
            0, 2, 4, 6
    };

    private final int[] TRADE_ID = new int[] {
            1, 3, 5, 7
    };

    public MerchantTradeContainer(int id, MerchantEntity merchant, Inventory playerInventory) {
        super(ModMenuTypes.MERCHANT_CONTAINER_TYPE.get(), id, playerInventory, merchant.getInventory());
        this.merchant = merchant;
        this.workerInventory = merchant.getTradeInventory();
        addWorkerTradeSlots();
        addWorkerPriceSlots();
        addPlayerInventorySlots();
    }

    @Override
    public int getInvOffset() {
        return 25;
    }

    public void addWorkerPriceSlots() {
        for (int k = 0; k < 4; ++k) {
            this.addSlot(new Slot(workerInventory, PRICE_ID[k], 8 + 18, 18 + k * 18) {
                @Override
                public boolean mayPlace(ItemStack itemStack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
    }

    public void addWorkerTradeSlots() {
        for (int k = 0; k < 4; ++k) {
            this.addSlot(new Slot(workerInventory, TRADE_ID[k], 8 + 18 * 4, 18 + k * 18) {
                @Override
                public boolean mayPlace(ItemStack itemStack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
    }

    public AbstractWorkerEntity getWorker() {
        return merchant;
    }

    public ItemStack quickMoveStack(Player playerIn, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player playerIn) {
        if(this.workerInventory.stillValid(playerIn) && this.merchant.isAlive() && this.merchant.distanceTo(playerIn) < 8.0F){
            this.merchant.isTrading = true;
            return true;
        }
        return false;
    }

    @Override
    public void removed(Player playerIn) {
        this.merchant.isTrading = false;
        super.removed(playerIn);
    }
}
