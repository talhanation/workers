package com.talhanation.workers.inventory;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.init.ModMenuTypes;

import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class MerchantInventoryContainer extends ContainerBase {
    private final Container workerInventory;
    private final MerchantEntity merchant;

    public MerchantInventoryContainer(int id, MerchantEntity merchant, Inventory playerInventory) {
        super(Main.MERCHANT_INVENTORY_CONTAINER_TYPE, id, playerInventory, merchant.getInventory());
        this.merchant = merchant;
        this.workerInventory = merchant.getInventory();
        addPlayerInventorySlots();
        addWorkerInventorySlots();
    }

    @Override
    public int getInvOffset() {
        return 18;
    }

    public void addWorkerInventorySlots() {
        for (int k = 0; k < 4; ++k) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(workerInventory, l + k * 9, 8 + l * 18, 18 + k * 18));
            }
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

    public void broadcastChanges() {
        super.broadcastChanges();
        merchant.upgradeTool();
        merchant.upgradeArmor();
    }

}
