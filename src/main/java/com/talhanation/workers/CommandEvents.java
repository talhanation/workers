package com.talhanation.workers;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.workers.entities.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class CommandEvents {
	public void setWorkArea(UUID player_uuid, AbstractWorkerEntity worker, AABB area) {
        LivingEntity owner = worker.getOwner();
    }

    public static void onAddDepositCommand(UUID player_uuid, AbstractWorkerEntity worker, int group, BlockPos blockPos) {
        if (worker.isEffectedByCommand(player_uuid, group)) {
            worker.addDepositPosition(blockPos);
        }
    }

}
