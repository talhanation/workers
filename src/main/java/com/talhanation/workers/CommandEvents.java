package com.talhanation.workers;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.MinerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.StringTextComponent;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class CommandEvents {

    public static void onCKeyPressed(UUID player_uuid, MinerEntity worker) {

        Minecraft minecraft = Minecraft.getInstance();
        LivingEntity owner = worker.getOwner();

        if (worker.isTame() &&  Objects.equals(worker.getOwnerUUID(), player_uuid)) {
            boolean tame = worker.isTame();

            if (owner != null){
                Direction playerDirection = owner.getDirection();
                worker.setMineDirectrion(playerDirection);
            }

            RayTraceResult rayTraceResult = minecraft.hitResult;
            if (rayTraceResult != null) {
                if (rayTraceResult.getType() == RayTraceResult.Type.BLOCK) {
                    BlockRayTraceResult blockraytraceresult = (BlockRayTraceResult) rayTraceResult;
                    BlockPos blockpos = blockraytraceresult.getBlockPos();
                    worker.setStartPos(Optional.of(blockpos));
                    worker.setFollow(false);
                    worker.setIsWorking(true);
                    worker.getOwner().sendMessage(new StringTextComponent(worker.getMineDirectrion().getSerializedName()), worker.getOwner().getUUID());
                }
            }
        }

    }

}
