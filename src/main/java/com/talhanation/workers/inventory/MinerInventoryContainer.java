package com.talhanation.workers.inventory;

import com.talhanation.workers.entities.MinerEntity;
import com.talhanation.workers.init.ModMenuTypes;

import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class MinerInventoryContainer extends ContainerBase {

    private final Container workerInventory;
    private final MinerEntity worker;

    public MinerInventoryContainer(int id, MinerEntity worker, Inventory playerInventory) {
        super(ModMenuTypes.MINER_CONTAINER_TYPE.get(), id, playerInventory, worker.getInventory());
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
                this.addSlot(new Slot(workerInventory, l + k * 9, 8 + l * 18, 18 + k * 18));
            }
        }
    }

    public MinerEntity getMiner() {
        return worker;
    }

    @Override
    public boolean stillValid(Player playerIn) {
        return this.workerInventory.stillValid(playerIn) && this.worker.isAlive()
                && this.worker.distanceTo(playerIn) < 8.0F;
    }

    @Override
    public void removed(Player playerIn) {
        super.removed(playerIn);
    }

    public void broadcastChanges() {
        super.broadcastChanges();
        worker.upgradeTool();
        worker.updateNeedsTool();
        worker.upgradeArmor();
    }
}
