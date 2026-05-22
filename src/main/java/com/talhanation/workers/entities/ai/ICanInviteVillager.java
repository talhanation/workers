package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.world.entity.npc.Villager;

import java.util.UUID;

public interface ICanInviteVillager {

    UUID getUUID();
    AbstractWorkerEntity getWorker();
    void setActiveTradingVillager(Villager villager);
    Villager getActiveTradingVillager();
}
