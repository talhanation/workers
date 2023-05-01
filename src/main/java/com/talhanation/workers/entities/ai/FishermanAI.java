package com.talhanation.workers.entities.ai;

import com.talhanation.workers.Main;
import com.talhanation.workers.Translatable;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.FishermanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;
import java.util.Objects;

import static com.talhanation.workers.entities.FishermanEntity.State.*;

public class FishermanAI extends Goal {
    private final FishermanEntity fisherman;
    private int fishingTimer = 100;
    private int throwTimer = 0;
    private final int fishingRange = 10;
    private BlockPos fishingPos = null;
    private BlockPos coastPos;
    private Boat boat;
    private FishermanEntity.State state;


    public FishermanAI(FishermanEntity fishermanEntity) {
        this.fisherman = fishermanEntity;
    }

    @Override
    public boolean canUse() {
        return fisherman.getStartPos() != null;// fisherman.canWork();// stop wegen deposit bla
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.coastPos = fisherman.getStartPos();
        //this.fishingPos = this.findWaterBlock();
        this.state = MOVING_COAST;
        super.start();
    }


    @Override
    public void tick() {
        Main.LOGGER.info("State: " + state);

        if(!fisherman.canWork()) this.state = STOPPING;


        switch (state){
            case MOVING_COAST -> {
                if (coastPos == null) return;// ändern

                moveToPos(coastPos);

                if (coastPos.closerThan(fisherman.getOnPos(), 2F)) {
                    List<Boat> list =  fisherman.level.getEntitiesOfClass(Boat.class, fisherman.getBoundingBox().inflate(8D));
                    if(!list.isEmpty()){
                        boat = list.get(0);
                        state = MOVING_BOAT;
                        fishingPos = findWaterBlock();
                    }
                    else {
                        state = FISHING;
                    }
                }

            }

            case MOVING_BOAT -> {
                this.fisherman.walkTowards(boat.getOnPos(), 1F);

                if(boat.getFirstPassenger() != null && this.fisherman.equals(boat.getFirstPassenger())){
                    state = SAILING;
                }
            }

            case SAILING -> {
                updateBoatControl(fishingPos.getX(), fishingPos.getZ());

                if (coastPos.closerThan(fisherman.getOnPos(), 3F)) {
                    state = FISHING;
                }
            }

            case FISHING -> {
                fishing();
            }

            case STOPPING -> {
                if(boat != null && boat.getFirstPassenger() != null && this.fisherman.equals(boat.getFirstPassenger())){
                    updateBoatControl(coastPos.getX(), coastPos.getZ());
                }
                else
                    this.moveToPos(coastPos);

                if (coastPos.closerThan(fisherman.getOnPos(), 3F)) {
                    fisherman.stopRiding();
                    state = STOP;
                }
            }

            case STOP -> {
                stop();
            }
        }
    }

    private void moveToPos(BlockPos pos) {
        //Move to minePos -> normal movement
        if(!pos.closerThan(fisherman.getOnPos(), 12F)){
            this.fisherman.walkTowards(pos, 1F);
        }
        //Near Mine Pos -> presice movement
        if (!pos.closerThan(fisherman.getOnPos(), 4F)) {
            this.fisherman.getMoveControl().setWantedPosition(pos.getX(), fisherman.getStartPos().getY(), pos.getZ(), 1);
        }
    }

    @Override
    public void stop() {
        this.fishingPos = null;
        this.fishingTimer = 0;
        this.resetTask();
        super.stop();
    }

    public void resetTask() {
        fisherman.getNavigation().stop();
        this.fishingTimer = fisherman.getRandom().nextInt(600);
    }

    // Find a water block to fish
    private BlockPos findWaterBlock() {
        for (int x = -this.fishingRange; x < this.fishingRange; ++x) {
            for (int y = -2; y < 2; ++y) {
                for (int z = -this.fishingRange; z < this.fishingRange; ++z) {
                    if (coastPos != null) {
                        BlockPos pos = this.coastPos.offset(x, y, z);
                        BlockState targetBlock = this.fisherman.level.getBlockState(pos);
                        if (targetBlock.is(Blocks.WATER)) {
                            return pos;
                        }
                    }
                }
            }
        }
        //No water nearby
        if (fisherman.getOwner() != null){
            fisherman.tellPlayer(fisherman.getOwner(), Translatable.TEXT_FISHER_NO_WATER);
            this.fisherman.setIsWorking(false, true);
            this.fisherman.clearStartPos();
            this.stop();
        }
        return null;
	}

    public void spawnFishingLoot() {
        //TODO: When water depth is deep, reduce timer
        this.fishingTimer = 500 + fisherman.getRandom().nextInt(4000);
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

    private void fishing(){
        /*
        // When near work pos, find a water block to fish
        if (this.fishingPos == null) {
            this.fishingPos = this.findWaterBlock();
            return;
        }
         */

        // Look at the water block
        if (fishingPos != null) {
            this.fisherman.getLookControl().setLookAt(
                    fishingPos.getX(),
                    fishingPos.getY() + 1,
                    fishingPos.getZ(),
                    10.0F,
                    (float) this.fisherman.getMaxHeadXRot()
            );


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
                this.spawnFishingLoot();
                this.fisherman.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 1, 1);
                this.fisherman.swing(InteractionHand.MAIN_HAND);
                this.fisherman.increaseFarmedItems();
                this.fisherman.consumeToolDurability();
                this.resetTask();
            }
        }
        if (throwTimer > 0) throwTimer--;
    }


    private void updateBoatControl(double posX, double posZ) {
        if(this.fisherman.getVehicle() instanceof Boat onBoat && onBoat.getPassengers().get(0).equals(this.fisherman)) {
            double dx = posX - this.fisherman.getX();
            double dz = posZ - this.fisherman.getZ();

            float angle = Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * 180.0D / 3.14D) - 90.0F);
            float drot = angle - Mth.wrapDegrees(onBoat.getYRot());

            boolean inputLeft = (drot < 0.0F && Math.abs(drot) >= 5.0F);
            boolean inputRight = (drot > 0.0F && Math.abs(drot) >= 5.0F);
            boolean inputUp = (Math.abs(drot) < 20.0F);

            float f = 0.0F;

            if (inputLeft) {
                onBoat.setYRot(onBoat.getYRot() - 3F);
            }

            if (inputRight) {
                onBoat.setYRot(onBoat.getYRot() + 3F);
            }


            if (inputRight != inputLeft && !inputUp) {
                f += 0.005F;
            }

            if (inputUp) {
                f += 0.04F;
            }
            onBoat.setDeltaMovement(onBoat.getDeltaMovement().add((double)(Mth.sin(-onBoat.getYRot() * ((float)Math.PI / 180F)) * f), 0.0D, (double)(Mth.cos(onBoat.getYRot() * ((float)Math.PI / 180F)) * f)));
            onBoat.setPaddleState(inputRight || inputUp, inputLeft || inputUp);
        }
    }
}
