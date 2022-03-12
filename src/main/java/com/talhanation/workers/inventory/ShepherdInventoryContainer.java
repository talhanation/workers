package com.talhanation.workers.inventory;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.ShepherdEntity;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;

public class ShepherdInventoryContainer extends ContainerBase {

    private final IInventory workerInventory;
    private final ShepherdEntity worker;

    public ShepherdInventoryContainer(int id, ShepherdEntity worker, PlayerInventory playerInventory) {
        super(Main.SHEPHERD_CONTAINER_TYPE, id, playerInventory, worker.getInventory());
        this.worker = worker;
        this.workerInventory = worker.getInventory();

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

    public ShepherdEntity getShepherd() {
        return worker;
    }

    @Override
    public boolean stillValid(PlayerEntity playerIn) {
        return this.workerInventory.stillValid(playerIn) && this.worker.isAlive() && this.worker.distanceTo(playerIn) < 8.0F;
    }

    @Override
    public void removed(PlayerEntity playerIn) {
        super.removed(playerIn);
    }
}
