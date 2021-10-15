package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.FishermanEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameterSet;
import net.minecraft.loot.LootTables;
import net.minecraft.world.server.ServerWorld;

import java.util.List;

public class FishermanAI extends Goal {
    private final FishermanEntity fisherman;
    private int fishingTimer = 1000;
    private int throwTimer = 0;
    private boolean isNearWater = false;

    public FishermanAI(FishermanEntity fishermanEntity) {
        this.fisherman = fishermanEntity;
    }

    @Override
    public boolean canUse() {
        return false;
    }


    public void resetTask() {
        fisherman.getNavigation().stop();
        //resetTarget();
        this.isNearWater = false;
        this.fishingTimer = 250 +  fisherman.getRandom().nextInt(500);
    }

    public void spawnFishingLoot() {
        this.fishingTimer = 250 + fisherman.getRandom().nextInt(500);
        double luck = 0.1D;
        LootContext.Builder lootcontext$builder = new LootContext.Builder((ServerWorld) this.fisherman.level);
        lootcontext$builder.withLuck((float) luck); // Forge: add player & looted entity to LootContext
        LootParameterSet.Builder lootparameterset$builder = new LootParameterSet.Builder();
        List<ItemStack> result = fisherman.level.getServer().getLootTables().get(LootTables.FISHING_FISH).getRandomItems(lootcontext$builder.create(lootparameterset$builder.build()));

        for (ItemStack itemstack : result) {
            ItemEntity ItemEntity = new ItemEntity(this.fisherman.level, this.fisherman.getX(), this.fisherman.getY(), this.fisherman.getZ(), itemstack);
            this.fisherman.level.addFreshEntity(ItemEntity);
        }
    }

}
