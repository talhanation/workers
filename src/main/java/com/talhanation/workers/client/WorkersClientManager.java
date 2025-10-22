package com.talhanation.workers.client;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsClaimManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public class WorkersClientManager {

    public static boolean configValueWorkAreaOnlyInFactionClaim;

    public static boolean isInFactionClaim(BlockPos pos){
        if(!configValueWorkAreaOnlyInFactionClaim) return true;
        if (pos == null) return false;
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        RecruitsClaim claim = RecruitsClaimManager.getClaimAt(chunkPos, ClientManager.recruitsClaims);
        if (claim == null) return false;
        return claim.containsChunk(chunkPos) && claim.getOwnerFaction().getStringID().equals(ClientManager.ownFaction.getStringID());
    }
}
