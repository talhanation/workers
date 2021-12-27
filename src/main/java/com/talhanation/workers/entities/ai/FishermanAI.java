package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.FishermanEntity;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.material.Material;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.*;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import java.util.List;
import java.util.Objects;

public class FishermanAI extends Goal {
    private final FishermanEntity fisherman;
    private int fishingTimer = 20;
    private int throwTimer = 0;
    private BlockPos fishingPos = null;
    private boolean isNearWater = false;

    public FishermanAI(FishermanEntity fishermanEntity) {
        this.fisherman = fishermanEntity;
    }

    @Override
    public boolean canUse() {
        if (!this.fisherman.getStartPos().isPresent()) {
            return false;
        }
        if (this.fisherman.getFollow()) {
            return false;
        } else if (fisherman.getIsWorking() && !this.fisherman.getFollow())
            return true;

        else
            return false;
    }

    @Override
    public void start() {
        super.start();
        this.fishingPos = new BlockPos(fisherman.getStartPos().get().getX(), fisherman.getStartPos().get().getY(), fisherman.getStartPos().get().getZ());
    }


    public void resetTask() {
        fisherman.getNavigation().stop();
        this.isNearWater = false;
        this.fishingTimer = 20; // + fisherman.getRandom().nextInt(500);
    }

    public void spawnFishingLoot() {
        this.fishingTimer = 400 + fisherman.getRandom().nextInt(500);
        double luck = 0.1D;
        LootContext.Builder lootcontext$builder = (new LootContext.Builder((ServerWorld)fisherman.level))
                .withParameter(LootParameters.ORIGIN, fisherman.position())
                .withParameter(LootParameters.TOOL, this.fisherman.getItemInHand(Hand.MAIN_HAND))
                .withLuck((float) luck);

        LootTable loottable = fisherman.getServer().getLootTables().get(LootTables.FISHING);
        List<ItemStack> list = loottable.getRandomItems(lootcontext$builder.create(LootParameterSets.FISHING)); // .create throws null

        for (ItemStack itemstack : list) {
            ItemEntity itementity = new ItemEntity(fisherman.level, fisherman.getX(), fisherman.getY(), fisherman.getZ(), itemstack);
            fisherman.getInventory().addItem(itemstack);
        }
    }

    @Override
    public void tick() {
        if (this.fishingPos != null) {

            if (fishingPos.closerThan(fisherman.position(), 9)) {
                this.fisherman.getLookControl().setLookAt(fishingPos.getX(), fishingPos.getY() + 1, fishingPos.getZ(), 10.0F, (float) this.fisherman.getMaxHeadXRot());
                isNearWater = true;
            } else
                isNearWater = false;

            if(isNearWater){
                this.fisherman.getNavigation().stop();
            }

            if (!isNearWater) {
                this.fisherman.getNavigation().moveTo(this.fishingPos.getX() + 0.5D, this.fishingPos.getY(), this.fishingPos.getZ() + 0.5D, 0.85D);
            }

            if (throwTimer == 0) {
                fisherman.playSound(SoundEvents.FISHING_BOBBER_THROW, 1, 0.5F);
                throwTimer = 20;
                //if (fisherman.getOwner() != null)
                //fisherman.level.addFreshEntity(new FishingBobberEntity((PlayerEntity) this.fisherman.getOwner(), this.fisherman.level, 0, 0)); // need fishbobber for worker
            }

        }else {
            this.resetTask();
        }

        if (isNearWater) {
            if (fishingTimer > 0) {
                fishingTimer--;
            }
            if (fishingTimer == 0) {
                spawnFishingLoot(); // crash because of null
                fisherman.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 1, 1);
            }
        }
        if (throwTimer > 0) {
            throwTimer--;
        }
    }

}
