package com.talhanation.workers.client;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsClaimManager;
import com.talhanation.workers.WorkAreaTypes;
import com.talhanation.workers.config.BuildMode;
import com.talhanation.workers.config.BuildPaymentMode;
import com.talhanation.workers.world.VillagerPriceTable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;

public class WorkersClientManager {

    public static boolean configValueWorkAreaOnlyInFactionClaim;
    public static BuildMode buildMode = BuildMode.FREE;
    public static BuildPaymentMode buildPaymentMode = BuildPaymentMode.MATERIALS;
    public static ItemStack currency = new ItemStack(Items.EMERALD);
    public static List<String> serverBuildingPresetNames = new ArrayList<>();
    public static boolean configValueOnlyBuildings;
    public static Map<Item, VillagerPriceTable.Entry> villagerPrices = new HashMap<>();
    public static boolean isInFactionClaim(BlockPos pos, WorkAreaTypes type){
        if(configValueOnlyBuildings && type != WorkAreaTypes.BUILDING) return false;

        if(!configValueWorkAreaOnlyInFactionClaim) return true;

        if (pos == null) return false;
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        RecruitsClaim claim = RecruitsClaimManager.getClaimAt(chunkPos, ClientManager.recruitsClaims);
        if (claim == null) return false;
        if(ClientManager.ownFaction == null) return false;

        return claim.containsChunk(chunkPos) && claim.getOwnerFaction().getStringID().equals(ClientManager.ownFaction.getStringID());
    }
}
