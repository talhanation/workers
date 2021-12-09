package com.talhanation.workers.client.events;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.MinerEntity;
import com.talhanation.workers.network.MessageStartPos;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class PlayerEvents {

    public static void onCKeyPressed(UUID player_uuid, AbstractWorkerEntity worker) {

        Minecraft minecraft = Minecraft.getInstance();
        LivingEntity owner = worker.getOwner();


        if (worker.isTame() &&  Objects.equals(worker.getOwnerUUID(), player_uuid)) {

            if (owner != null){
                if (worker instanceof MinerEntity){
                    Direction playerDirection = owner.getDirection();
                    MinerEntity miner = (MinerEntity) worker;
                    miner.setMineDirectrion(playerDirection);
                }
            }

            RayTraceResult rayTraceResult = minecraft.hitResult;
            if (rayTraceResult != null) {
                if (rayTraceResult.getType() == RayTraceResult.Type.BLOCK) {
                    BlockRayTraceResult blockraytraceresult = (BlockRayTraceResult) rayTraceResult;
                    BlockPos blockpos = blockraytraceresult.getBlockPos();

                    //worker.getOwner().sendMessage(new StringTextComponent(worker.getMineDirectrion().getSerializedName()), worker.getOwner().getUUID());
                }
            }
        }

    }

}
