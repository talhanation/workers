package com.talhanation.workers;

import com.talhanation.recruits.ClaimEvent;
import com.talhanation.recruits.SiegeEvent;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.HomeArea;
import com.talhanation.workers.entities.workarea.MarketArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class WorkerClaimEvents {

    @SubscribeEvent
    public void onSiegeSuccess(SiegeEvent.Success event) {
        ServerLevel level    = event.getLevel();
        RecruitsClaim claim  = event.getClaim();
        UUID newOwnerUUID    = getOwnerUUID(claim);
        String newOwnerName  = getOwnerName(claim);

        transferWorkAreasInClaim(claim, level, newOwnerUUID, newOwnerName);

        handleWorkersUnderSiege(claim, level);
    }

    @SubscribeEvent
    public void onClaimUpdated(ClaimEvent.Updated event) {
        RecruitsClaim claim = event.getClaim();
        UUID newOwnerUUID   = getOwnerUUID(claim);

        transferWorkAreasInClaim(claim, event.getLevel(), newOwnerUUID, getOwnerName(claim));
    }

    private void transferWorkAreasInClaim(RecruitsClaim claim, ServerLevel level, UUID newOwnerUUID, String newOwnerName) {
        AABB claimBounds = getClaimBounds(claim, level);
        if (claimBounds == null) return;

        List<AbstractWorkAreaEntity> areas = level.getEntitiesOfClass(AbstractWorkAreaEntity.class, claimBounds);

        for (AbstractWorkAreaEntity area : areas) {
            if (area.isRemoved()) continue;

            if (area.getPlayerUUID() == null) continue;

            area.setPlayerUUID(newOwnerUUID);
            area.setPlayerName(newOwnerName);

            if(area instanceof HomeArea homeArea) homeArea.clearResident();
            if(area instanceof MarketArea marketArea) marketArea.clearAssignedMerchant();
        }
    }

    private void handleWorkersUnderSiege(RecruitsClaim claim, ServerLevel level) {
        AABB claimBounds = getClaimBounds(claim, level);
        if (claimBounds == null) return;

        BlockPos claimCenter = getClaimCenter(claim);

        List<AbstractWorkerEntity> workers =
                level.getEntitiesOfClass(AbstractWorkerEntity.class, claimBounds);

        Random random = new Random();

        for (AbstractWorkerEntity worker : workers) {
            if (worker.isRemoved() || !worker.isOwned()) continue;

            float morale = worker.getMorale();

            if (morale <= 30f) {

                worker.disband(worker.getOwner(), false, false);
            }
            else {
                // Higher morale — worker flees to safety outside the claim
                BlockPos fleePos = computeFleePos(claimCenter, level, random);
                worker.startFleeing(fleePos);
            }
        }
    }

    @Nullable
    private AABB getClaimBounds(RecruitsClaim claim, Level level) {
        List<ChunkPos> chunks = claim.getClaimedChunks();
        if (chunks.isEmpty()) return null;

        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (ChunkPos chunk : chunks) {
            minX = Math.min(minX, chunk.getMinBlockX());
            minZ = Math.min(minZ, chunk.getMinBlockZ());
            maxX = Math.max(maxX, chunk.getMaxBlockX() + 1);
            maxZ = Math.max(maxZ, chunk.getMaxBlockZ() + 1);
        }

        return new AABB(minX, level.getMinBuildHeight(), minZ,
                        maxX, level.getMaxBuildHeight(),  maxZ);
    }

    private BlockPos getClaimCenter(RecruitsClaim claim) {
        ChunkPos center = claim.getCenter();
        if (center != null) return center.getMiddleBlockPosition(64);

        List<ChunkPos> chunks = claim.getClaimedChunks();
        if (chunks.isEmpty()) return BlockPos.ZERO;

        long avgX = 0, avgZ = 0;
        for (ChunkPos c : chunks) { avgX += c.getMiddleBlockX(); avgZ += c.getMiddleBlockZ(); }
        return new BlockPos((int)(avgX / chunks.size()), 64, (int)(avgZ / chunks.size()));
    }

    private BlockPos computeFleePos(BlockPos center, ServerLevel level, Random random) {
        double angle = random.nextDouble() * 2 * Math.PI;
        int    dist  = 150 + random.nextInt(101); // 150–250 blocks
        int    tx    = center.getX() + (int)(Math.cos(angle) * dist);
        int    tz    = center.getZ() + (int)(Math.sin(angle) * dist);
        return level.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(tx, 0, tz));
    }

    @Nullable
    private UUID getOwnerUUID(RecruitsClaim claim) {
        RecruitsPlayerInfo info = claim.getPlayerInfo();
        return info != null ? info.getUUID() : null;
    }

    private String getOwnerName(RecruitsClaim claim) {
        RecruitsPlayerInfo info = claim.getPlayerInfo();
        return info != null ? info.getName() : "";
    }
}
