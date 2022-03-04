package com.talhanation.workers;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.MerchantEntity;
import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class CommandEvents {

    public static void setStartPosWorker(UUID player_uuid, AbstractWorkerEntity worker, BlockPos blockpos) {
        LivingEntity owner = worker.getOwner();

        if (worker.isTame() && Objects.equals(worker.getOwnerUUID(), player_uuid)) {
            if (owner != null) {
                if (worker instanceof MinerEntity) {
                    Direction playerDirection = owner.getDirection();
                    MinerEntity miner = (MinerEntity) worker;
                    miner.setMineDirection(playerDirection);
                }
            }
            worker.setStartPos(Optional.of(blockpos));
            worker.setFollow(false);
            worker.setIsWorking(true);
        }
    }

    public static void handleMerchantTrade(PlayerEntity player, MerchantEntity merchant){

    }

}
