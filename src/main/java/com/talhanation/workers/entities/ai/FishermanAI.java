package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.FishermanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;
import java.util.Random;

public class FishermanAI extends Goal {
    private final FishermanEntity fisherman;
    private int fishingTimer = 100;
    private int throwTimer = 0;
    private BlockPos fishingPos = null;
    private BlockPos workPos;

    public FishermanAI(FishermanEntity fishermanEntity) {
        this.fisherman = fishermanEntity;
    }

    @Override
    public boolean canUse() {
        if (
            this.fisherman.getFollow() ||
            this.fisherman.needsToSleep()
        ) {
            return false;
        }
        return isNearWater();
    }

    @Override
    public void start() {
        this.workPos = fisherman.getStartPos();
        super.start();
    }


    public void resetTask() {
        fisherman.getNavigation().stop();
        this.fishingTimer = fisherman.getRandom().nextInt(600);
    }

    public void spawnFishingLoot() {
        this.fishingTimer = 500 + fisherman.getRandom().nextInt(2000);
        double luck = 0.1D;
        LootContext.Builder lootcontext$builder = (new LootContext.Builder((ServerLevel)fisherman.level))
                .withParameter(LootContextParams.ORIGIN, fisherman.position())
                .withParameter(LootContextParams.TOOL, this.fisherman.getItemInHand(InteractionHand.MAIN_HAND))
                .withLuck((float) luck);

        LootTable loottable = fisherman.getServer().getLootTables().get(BuiltInLootTables.FISHING);
        List<ItemStack> list = loottable.getRandomItems(lootcontext$builder.create(LootContextParamSets.FISHING));

        for (ItemStack itemstack : list) {
            fisherman.getInventory().addItem(itemstack);
        }
    }

    @Override
    public void tick() {
        if ( workPos != null && !workPos.closerThan(fisherman.getOnPos(), 10D) && !fisherman.getFollow())
            this.fisherman.getNavigation().moveTo(workPos.getX(), workPos.getY(), workPos.getZ(), 1);


        if (this.fishingPos != null && this.isNearWater()) {

            if (fishingPos.closerThan(fisherman.getOnPos(), 9))
                this.fisherman.getLookControl().setLookAt(fishingPos.getX(), fishingPos.getY() + 1, fishingPos.getZ(), 10.0F, (float) this.fisherman.getMaxHeadXRot());

            if(isNearWater()){
                this.fisherman.getNavigation().stop();
            }

            if (!isNearWater()) {
                this.fisherman.getNavigation().moveTo(this.fishingPos.getX() + 0.5D, this.fishingPos.getY(), this.fishingPos.getZ() + 0.5D, 0.85D);
            }

            if (throwTimer == 0) {
                fisherman.playSound(SoundEvents.FISHING_BOBBER_THROW, 1, 0.5F);
                throwTimer = fisherman.getRandom().nextInt(400);
                //if (fisherman.getOwner() != null)
                FishingBobberEntity
                //fisherman.level.addFreshEntity(new FishermansFishingBobberEntity(this.fisherman.level, this.fisherman.getOwner())); // need fishbobber for worker
            }

        }else {
            this.resetTask();
        }

        if (isNearWater()) {
            if (fishingTimer > 0) {
                fishingTimer--;
            }
            if (fishingTimer == 0) {
                spawnFishingLoot();
                fisherman.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 1, 1);
            }
        }
        if (throwTimer > 0) {
            throwTimer--;
        }
    }

    public boolean isNearWater() {
        Random random = new Random();
        int range = 14;
        for(int i = 0; i < 15; i++){
            BlockPos blockpos1 = this.fisherman.getWorkerOnPos().offset(random.nextInt(range) - range/2, 3, random.nextInt(range) - range/2);
            while(this.fisherman.level.isEmptyBlock(blockpos1) && blockpos1.getY() > 1){
                blockpos1 = blockpos1.below();
            }
            if(this.fisherman.level.getFluidState(blockpos1).is(FluidTags.WATER)){
                if (throwTimer == 0)
                    this.fishingPos = blockpos1;
                return true;
            }
        }
        return false;
    }
}
