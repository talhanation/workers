package com.talhanation.workers.inventory;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.MerchantEntity;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;

public class MerchantTradeContainer extends ContainerBase {

    private final IInventory workerInventory;
    private final MerchantEntity merchant;

    public MerchantTradeContainer(int id, MerchantEntity merchant, PlayerInventory playerInventory) {
        super(Main.WORKER_CONTAINER_TYPE, id, playerInventory, merchant.getInventory());
        this.merchant = merchant;
        this.workerInventory = merchant.getInventory();

        addWorkerInventorySlots();
        addPlayerInventorySlots();
    }

    @Override
    public int getInvOffset() {
        return 0;
    }
    
    public void addWorkerInventorySlots() {
        for (int k = 0; k < 2; ++k) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(workerInventory, l + k * 9, 8 + l * 18,  18 + k * 18));
            }
        }
    }

    public AbstractWorkerEntity getWorker() {
        return merchant;
    }

    @Override
    public boolean stillValid(PlayerEntity playerIn) {
        return this.workerInventory.stillValid(playerIn) && this.merchant.isAlive() && this.merchant.distanceTo(playerIn) < 8.0F;
    }

    @Override
    public void removed(PlayerEntity playerIn) {
        super.removed(playerIn);
    }
}
