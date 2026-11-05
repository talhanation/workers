package com.talhanation.workers;

import com.talhanation.recruits.ClaimEvent;
import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.SiegeEvent;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.workarea.AbstractWorkAreaEntity;
import com.talhanation.workers.entities.workarea.HomeArea;
import com.talhanation.workers.entities.workarea.MarketArea;
import com.talhanation.workers.world.WorkAreaIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
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
        // only the claimed chunks, not every chunk of the bounding box around them
        List<AbstractWorkAreaEntity> areas = WorkAreaIndex.get(level).inChunks(AbstractWorkAreaEntity.class, claim.getClaimedChunks());

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

        UUID newOwnerUUID = getOwnerUUID(claim);

        List<AbstractWorkerEntity> workers =
                level.getEntitiesOfClass(AbstractWorkerEntity.class, claimBounds);

        Random random = new Random();

        for (AbstractWorkerEntity worker : workers) {
            if (worker.isRemoved() || !worker.isOwned()) continue;
            // only workers really standing in the claim (the box also covers unclaimed chunks around it)
            if (!claim.containsChunk(new ChunkPos(worker.blockPosition()))) continue;
            // the conqueror's own workers stay as they are
            if (newOwnerUUID != null && newOwnerUUID.equals(worker.getOwnerUUID())) continue;

            float morale = worker.getMorale();

            if (morale <= 30f) {
                worker.disband(worker.getOwner(), false, false);
                if (newOwnerUUID == null) continue;
                switchToNewOwner(worker, claim, level, newOwnerUUID);
            }
            else {

                BlockPos fleePos = computeFleePos(claimCenter, level, random);
                // he leaves the claim and stays with his old owner, so he no longer belongs to its Governor
                worker.setGovernorClaim(null);
                worker.startFleeing(fleePos);
            }
        }
    }

    /**
     * The worker goes over to the new owner of the claim. Works while the new owner is offline:
     * owner, team and unit count are set from the UUID, like the Governor hires into a garrison.
     * If the new owner has no free unit slot, the worker stays free in the claim and can be hired.
     */
    private void switchToNewOwner(AbstractWorkerEntity worker, RecruitsClaim claim, ServerLevel level, UUID newOwnerUUID) {
        String factionId = claim.getOwnerFactionStringID() != null ? claim.getOwnerFactionStringID() : "";

        Player online = level.getServer().getPlayerList().getPlayer(newOwnerUUID);
        if (online != null) {
            worker.hire(online, null, true);
            worker.setFollowState(0);
            return;
        }

        if (!RecruitEvents.recruitsPlayerUnitManager.canPlayerRecruit(factionId, newOwnerUUID)) return;

        worker.resetPaymentTimer();
        worker.setOwnerUUID(Optional.of(newOwnerUUID));
        worker.setIsOwned(true);
        worker.getNavigation().stop();
        worker.setTarget(null);
        worker.setFollowState(0);
        worker.setAggroState(0);
        worker.despawnTimer = -1;
        RecruitEvents.recruitsPlayerUnitManager.addRecruits(newOwnerUUID, 1);

        PlayerTeam team = factionId.isEmpty() ? null : level.getScoreboard().getPlayerTeam(factionId);
        if (team != null) FactionEvents.addRecruitToTeam(worker, team, level);
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
