package com.talhanation.workers.inventory;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.AbstractWorkerEntity;

import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class WorkerHireContainer extends ContainerBase {
    private final AbstractWorkerEntity worker;

    public WorkerHireContainer(int id, Player playerEntity, AbstractWorkerEntity recruit, Inventory playerInventory) {
        super(Main.HIRE_CONTAINER_TYPE, id, null, new SimpleContainer(0));
        String playerName = playerEntity.getDisplayName().getString();
        String workerName = recruit.getDisplayName().getString();
        Main.LOGGER.info(String.format("Player %s is hiring %s.", playerName, workerName));
        this.worker = recruit;
        this.playerInventory = playerInventory;

        addPlayerInventorySlots();
    }

    @Override
    public int getInvOffset() {
        return 56;
    }

    public AbstractWorkerEntity getWorkerEntity() {
        return worker;
    }
}
