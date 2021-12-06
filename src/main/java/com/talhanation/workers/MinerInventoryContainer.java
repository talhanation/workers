package com.talhanation.workers;

import com.talhanation.workers.entities.MinerEntity;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;

public class MinerInventoryContainer extends ContainerBase {

    private final IInventory workerInventory;
    private final MinerEntity worker;

    public MinerInventoryContainer(int id, MinerEntity worker, PlayerInventory playerInventory) {
        super(Main.MINER_CONTAINER_TYPE, id, playerInventory, worker.getInventory());
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

    public MinerEntity getMiner() {
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
