package com.talhanation.workers.entities.ai;

import com.talhanation.workers.Main;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.FishermanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

public class FishermanAI extends Goal {
    private final FishermanEntity fisherman;
    private int fishingTimer = 100;
    private int throwTimer = 0;
    private final int fishingRange = 4;
    private BlockPos fishingPos = null;
    private BlockPos workPos;

    public FishermanAI(FishermanEntity fishermanEntity) {
        this.fisherman = fishermanEntity;
    }

    @Override
    public boolean canUse() {
        return (
            this.fisherman.itemsFarmed < 10 &&
            !this.fisherman.getFollow() &&
            !this.fisherman.needsToSleep()
        );
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.workPos = fisherman.getStartPos();
        this.fishingPos = this.findWaterBlock();
        Main.LOGGER.debug("Fishing started");
        super.start();
    }

    @Override
    public void stop() {
        this.fishingPos = null;
        this.fishingTimer = 0;
        Main.LOGGER.debug("Fishing stopped");
        super.stop();
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

        MinecraftServer server = fisherman.getServer();
        if (server == null) return;
        LootTable loottable = server.getLootTables().get(BuiltInLootTables.FISHING);
        List<ItemStack> list = loottable.getRandomItems(lootcontext$builder.create(LootContextParamSets.FISHING));

        for (ItemStack itemstack : list) {
            fisherman.getInventory().addItem(itemstack);
        }
    }

    @Override
    public void tick() {
        if (workPos == null) return;
        // When far from work pos, move to work pos
        if (!workPos.closerThan(fisherman.blockPosition(), 9D)) {
            this.fisherman.walkTowards(workPos, 2);
            return;
        }

        // When near work pos, find a water block to fish
        if (this.fishingPos == null) {
            this.fishingPos = this.findWaterBlock();
            return;
        }

        // Look at the water block
        this.fisherman.getLookControl().setLookAt(
            fishingPos.getX(),
            fishingPos.getY() + 1,
            fishingPos.getZ(),
            10.0F, 
            (float) this.fisherman.getMaxHeadXRot()
        );

        // Either walk towards the water block, or stop and stare.
        if (this.fisherman.blockPosition().closerThan(fishingPos, this.fishingRange)) {
            this.fisherman.getMoveControl().setWantedPosition(
                fishingPos.getX(), 
                fishingPos.getY() + 1, 
                fishingPos.getZ(), 
                0
            );
            this.fisherman.getNavigation().stop();

        } else {
            this.fisherman.walkTowards(fishingPos, 1);
            return;
        }

        if (throwTimer == 0) {
            fisherman.playSound(SoundEvents.FISHING_BOBBER_THROW, 1, 0.5F);
            this.fisherman.swing(InteractionHand.MAIN_HAND);
            throwTimer = fisherman.getRandom().nextInt(400);
            //  TODO: Create FishingBobberEntity compatible with AbstractEntityWorker.
            // fisherman.level.addFreshEntity(new FishermansFishingBobberEntity(this.fisherman.level, this.fisherman.getOwner()));
        }

        if (fishingTimer > 0) fishingTimer--;

        if (fishingTimer == 0) {
            // Get the loot
            spawnFishingLoot();
            fisherman.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 1, 1);
            this.fisherman.swing(InteractionHand.MAIN_HAND);
            this.fisherman.itemsFarmed += 1;
            this.fisherman.consumeToolDurability();
            this.resetTask();
        }
        
        if (throwTimer > 0) throwTimer--;
    }

    // Find a water block to fish
    private BlockPos findWaterBlock() {
        for (int x = -this.fishingRange; x < this.fishingRange; ++x) {
            for (int y = -2; y < 2; ++y) {
                for (int z = -this.fishingRange; z < this.fishingRange; ++z) {
                    BlockPos blockPos = this.workPos.offset(x, y, z);
                    BlockState targetBlock = this.fisherman.level.getBlockState(blockPos);
                    if (targetBlock.is(Blocks.WATER)) {
                        return blockPos;
                    }
                }
            }
        }
        // TODO: Handle no water near workplace
            // Ideally the fisher should tell you his workPos is not valid and setWorkPos(null).
        return null;
	}
}
