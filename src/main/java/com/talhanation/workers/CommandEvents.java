package com.talhanation.workers;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;

import java.util.Objects;
import java.util.UUID;

public class CommandEvents {

    public static void onCKeyPressed(UUID player_uuid, AbstractWorkerEntity worker) {
    /*
        Minecraft minecraft = Minecraft.getInstance();
        LivingEntity owner = worker.getOwner();
        if (worker.isTame() &&  Objects.equals(worker.getOwnerUUID(), player_uuid)) {
            boolean state = worker.isTame();

            if (state != 2){
                RayTraceResult rayTraceResult = minecraft.hitResult;
                if (rayTraceResult != null) {
                    if (rayTraceResult.getType() == RayTraceResult.Type.BLOCK) {
                        BlockRayTraceResult blockraytraceresult = (BlockRayTraceResult) rayTraceResult;
                        BlockPos blockpos = blockraytraceresult.getBlockPos();
                        worker.setMovePos(blockpos);
                        worker.setMove(true);
                    }
                    else if (rayTraceResult.getType() == RayTraceResult.Type.ENTITY){
                        Entity crosshairEntity = minecraft.crosshairPickEntity;
                        if (crosshairEntity != null){
                            worker.setMount(crosshairEntity.getUUID());
                        }

                    }
                }

            }
        }
        */

    }

}
