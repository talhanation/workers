package com.talhanation.workers.inventory;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.init.ModMenuTypes;
import de.maxhenkel.corelib.inventory.ContainerBase;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class MerchantWaypointContainer extends ContainerBase {
    private final AbstractWorkerEntity worker;

    public MerchantWaypointContainer(int id, Player playerEntity, AbstractWorkerEntity recruit, Inventory playerInventory) {
        super(ModMenuTypes.WAYPOINT_CONTAINER_TYPE.get(), id, null, new SimpleContainer(0));
        String playerName = playerEntity.getDisplayName().getString();
        String workerName = recruit.getDisplayName().getString();
        this.worker = recruit;
        this.playerInventory = playerInventory;
    }

    @Override
    public int getInvOffset() {
        return 56;
    }

    public AbstractWorkerEntity getWorkerEntity() {
        return worker;
    }
}
