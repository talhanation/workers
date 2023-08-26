package com.talhanation.workers.entities.ai;

import com.talhanation.workers.Main;
import com.talhanation.workers.Translatable;
import com.talhanation.workers.entities.FishermanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.talhanation.workers.entities.FishermanEntity.State.*;

public class FishermanAI extends Goal {
    private final FishermanEntity fisherman;
    private int fishingTimer = 100;
    private int throwTimer = 0;
    private int fishingRange;
    private BlockPos fishingPos = null;
    private BlockPos coastPos;
    private Boat boat;
    private FishermanEntity.State state;
    private List<BlockPos> waterBlocks;


    public FishermanAI(FishermanEntity fishermanEntity) {
        this.fisherman = fishermanEntity;
    }

    @Override
    public boolean canUse() {
        return true;

    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.coastPos = fisherman.getStartPos();
        this.setWorkState(FishermanEntity.State.fromIndex(fisherman.getState()));
        super.start();
    }


    @Override
    public void tick() {

        switch (state){
            case IDLE -> {
                if(fisherman.getStartPos() != null && fisherman.canWork()){

                    this.setWorkState(MOVING_COAST);
                }
            }

            case MOVING_COAST -> {
                if(!fisherman.canWork()) this.setWorkState(STOPPING);
                if(fisherman.getVehicle() != null) fisherman.stopRiding();

                if (coastPos == null) coastPos = fisherman.getStartPos();

                this.moveToPos(coastPos);

                if (coastPos.closerThan(fisherman.getOnPos(), 3F)) {
                    List<Boat> list =  fisherman.level.getEntitiesOfClass(Boat.class, fisherman.getBoundingBox().inflate(8D));
                    list.sort(Comparator.comparing(boatInList -> boatInList.distanceTo(fisherman)));
                    if(!list.isEmpty()){
                        boat = list.get(0);
                        fishingRange = 20;
                        this.setWorkState(MOVING_TO_BOAT);
                    }
                    else {
                        fishingRange = 5;
                        this.setWorkState(FISHING);
                    }

                    this.findWaterBlocks();
                    fishingPos = waterBlocks.get(fisherman.getRandom().nextInt(waterBlocks.size()));
                    this.fisherman.setDestPos(fishingPos);

                    if(fishingPos == null) this.setWorkState(STOPPING);

                }

            }

            case MOVING_TO_BOAT -> {
                if(boat != null){
                    if(!fisherman.canWork()) this.setWorkState(STOPPING);

                    this.moveToPos(boat.getOnPos());

                    if (coastPos.closerThan(fisherman.getOnPos(), 10F)) {
                        fisherman.startRiding(boat);
                    }

                    if(boat.getFirstPassenger() != null && this.fisherman.equals(boat.getFirstPassenger())){
                        this.fisherman.setSailPos(fishingPos);
                        this.setWorkState(SAILING);
                    }
                }
                else this.setWorkState(IDLE);

            }

            case SAILING -> {
                if(!fisherman.canWork()) this.setWorkState(STOPPING);

                double distance = fisherman.distanceToSqr(fishingPos.getX(), fishingPos.getY(), fishingPos.getZ());
                if(distance < 7.5F) { //valid value example: distance = 3.2
                    this.setWorkState(FISHING);
                }

            }

            case FISHING -> {
                if(!fisherman.canWork()) this.setWorkState(STOPPING);

                if(fishingPos == null) this.setWorkState(STOPPING);
                fishing();
            }

            case STOPPING -> {
                if(coastPos != null) {
                    if (boat != null && boat.getFirstPassenger() != null && this.fisherman.equals(boat.getFirstPassenger())) {
                        this.fisherman.setSailPos(coastPos);
                    } else{
                        this.moveToPos(coastPos);
                        fisherman.stopRiding();
                    }

                    if (coastPos.closerThan(fisherman.getOnPos(), 3.0F)) {
                        fisherman.stopRiding();
                        this.setWorkState(STOP);
                    }
                }
                else
                    this.setWorkState(IDLE);
            }

            case STOP -> {
                this.moveToPos(coastPos);
                fisherman.stopRiding();
                if (coastPos.closerThan(fisherman.getOnPos(), 1.5F)){
                    stop();
                }
            }
        }
    }

    private void setWorkState(FishermanEntity.State state) {
        this.state = state;
        this.fisherman.setState(state.getIndex());
    }

    private void moveToPos(BlockPos pos) {
        if(pos != null) {
            //Move to Pos -> normal movement
            if (!pos.closerThan(fisherman.getOnPos(), 12F)) {
                this.fisherman.walkTowards(pos, 1F);
            }
            //Near Pos -> presice movement
            if (!pos.closerThan(fisherman.getOnPos(), 2F)) {
                this.fisherman.getMoveControl().setWantedPosition(pos.getX(), fisherman.getStartPos().getY(), pos.getZ(), 1);
            }
        }
    }

    @Override
    public void stop() {
        this.fishingPos = null;
        this.fishingTimer = 0;
        this.resetTask();
        this.setWorkState(IDLE);
        super.stop();
    }

    public void resetTask() {
        fisherman.getNavigation().stop();
        this.fishingTimer = fisherman.getRandom().nextInt(600);
    }

    // Find a water block to fish
    private void findWaterBlocks() {
        this.waterBlocks = new ArrayList<>();
        for (int x = -this.fishingRange; x < this.fishingRange; ++x) {
            for (int y = -2; y < 2; ++y) {
                for (int z = -this.fishingRange; z < this.fishingRange; ++z) {
                    if (coastPos != null) {
                        BlockPos pos = this.coastPos.offset(x, y, z);
                        BlockState targetBlock = this.fisherman.level.getBlockState(pos);
                        if (targetBlock.is(Blocks.WATER) && fisherman.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > fishingRange) {
                            for( int i = 0; i < 4 ;i++){
                                if(this.fisherman.level.getBlockState(pos.above(i)).isAir() && i == 3){
                                    this.waterBlocks.add(pos);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        this.waterBlocks.sort(Comparator.comparing(this::getWaterDepth));

        if(waterBlocks.isEmpty()){
            //No water nearby
            if (fisherman.getOwner() != null){
                fisherman.tellPlayer(fisherman.getOwner(), Translatable.TEXT_FISHER_NO_WATER);
                this.fisherman.setIsWorking(false, true);
                this.fisherman.clearStartPos();
                this.stop();

            }
        }
    }

    public void spawnFishingLoot() {
        //TODO: When water depth is deep, reduce timer
        this.fishingTimer = 5 * 750 + fisherman.getRandom().nextInt(4000) / fishingRange;
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
        if (this.fisherman.getVehicle() == null && !coastPos.closerThan(fisherman.getOnPos(), 5F)) {
            this.moveToPos(coastPos);
        }
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


    private int getWaterDepth(BlockPos pos){
        int depth = 0;
        for(int i = 0; i < 10; i++){
            BlockState state = fisherman.level.getBlockState(pos.below(i));
            if(state.is(Blocks.WATER)){
                depth++;
            }
            else break;
        }
        return depth;
    }
}
