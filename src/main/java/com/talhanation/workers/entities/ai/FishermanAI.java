package com.talhanation.workers.entities.ai;

import com.talhanation.workers.Main;
import com.talhanation.workers.Translatable;
import com.talhanation.workers.entities.FishermanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.loot.FishingLoot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
    private boolean DEBUG = false;
    private boolean messageNoFishingRod = false;
    private int timer;
    private byte fails;

    public FishermanAI(FishermanEntity fishermanEntity) {
        this.fisherman = fishermanEntity;
    }

    @Override
    public boolean canUse() {
        return fisherman.isTame() && fisherman.getStartPos() != null;

    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.coastPos = this.getCoastPos();
        if(coastPos == null){
            coastPos = fisherman.getStartPos();
        }

        this.timer = 0;
        this.setWorkState(FishermanEntity.State.fromIndex(fisherman.getState()));
        super.start();
    }

    @Override
    public void tick() {
        if(DEBUG) Main.LOGGER.info("timer: " + timer);
        if(DEBUG) Main.LOGGER.info("State: " + state);
        switch (state){
            case IDLE -> {
                if(fisherman.getStartPos() != null && fisherman.canWork()){

                    this.setWorkState(CALC_COAST);
                }
            }

            case CALC_COAST -> {
                if(!fisherman.canWork()) this.setWorkState(STOPPING);
                if(fisherman.getVehicle() != null) fisherman.stopRiding();

                coastPos = getCoastPos();
                if (coastPos != null){
                    setWorkState(MOVING_COAST);
                }
                else coastPos = fisherman.getStartPos();

            }

            case MOVING_COAST -> {
                if(!fisherman.canWork()) this.setWorkState(STOPPING);
                if(fisherman.getVehicle() != null) fisherman.stopRiding();

                if (coastPos == null) setWorkState(CALC_COAST);
                else
                    this.moveToPos(coastPos);

                if (coastPos.closerThan(fisherman.getOnPos(), 3F)) {
                    List<Boat> list =  fisherman.getCommandSenderWorld().getEntitiesOfClass(Boat.class, fisherman.getBoundingBox().inflate(8D));
                    list.removeIf(boat -> !boat.getPassengers().isEmpty() || boat.getEncodeId().contains("smallships"));
                    list.sort(Comparator.comparing(boatInList -> boatInList.distanceTo(fisherman)));
                    if(!list.isEmpty()){
                        boat = list.get(0);
                        fishingRange = 40;

                        this.setWorkState(MOVING_TO_BOAT);
                    }
                    else {
                        fishingRange = 2;
                        this.setWorkState(FISHING);
                    }

                    fishingPos = this.findWaterBlocks();
                    if(fishingPos == null) {
                        if (fisherman.getOwner() != null) fisherman.tellPlayer(fisherman.getOwner(), Translatable.TEXT_FISHER_NO_WATER);
                        this.coastPos = null;
                        this.setWorkState(STOPPING);
                    }
                }

            }

            case MOVING_TO_BOAT -> {
                if(boat != null && boat.getPassengers().isEmpty()){
                    if(!fisherman.canWork()) {
                        this.setWorkState(STOPPING);
                    }

                    this.moveToPos(boat.getOnPos());

                    if (coastPos.closerThan(fisherman.getOnPos(), 10F)) {
                        fisherman.startRiding(boat);
                    }
                    else if(++timer > 200){
                        this.setWorkState(STOPPING);
                        timer = 0;
                    }

                    if(boat.getFirstPassenger() != null && this.fisherman.equals(boat.getFirstPassenger())){
                        this.fisherman.setSailPos(fishingPos);
                        this.setWorkState(SAILING);
                    }
                }
                else{
                    this.setWorkState(IDLE);
                }

            }

            case SAILING -> {
                if(!fisherman.canWork()) this.setWorkState(STOPPING);
                if(fishingPos == null){
                    this.setWorkState(IDLE);
                    break;
                }
                double distance = fisherman.distanceToSqr(fishingPos.getX(), fisherman.getY(), fishingPos.getZ());
                if(distance < 30F) { //valid value example: distance = 3.2
                    this.setWorkState(FISHING);
                }
                else if(++timer > 200){
                    timer = 0;
                    fails++;

                    if(fails == 3){
                        this.setWorkState(STOPPING);
                        timer = 0;
                    }
                    else {
                        if(fishingPos != null) fishingPos = findWaterBlocks();
                        else {
                            if (fisherman.getOwner() != null) fisherman.tellPlayer(fisherman.getOwner(), Translatable.TEXT_FISHER_NO_WATER);
                            this.coastPos = null;
                            this.setWorkState(STOPPING);
                            break;
                        }

                        this.fisherman.setSailPos(fishingPos);
                        this.setWorkState(SAILING);

                    }
                }
            }

            case FISHING -> {
                if(!fisherman.canWork()) this.setWorkState(STOPPING);

                if(fishingPos == null) this.setWorkState(STOPPING);

                if(!fisherman.hasMainToolInInv()) {
                    if(!messageNoFishingRod && this.fisherman.getOwner() != null){
                        this.fisherman.tellPlayer(fisherman.getOwner(), Translatable.TEXT_NO_FISHING_ROD);
                        messageNoFishingRod = true;
                    }
                    fisherman.setNeedsTool(true);
                }

                fishing();
            }

            case STOPPING -> {
                if(coastPos != null) {
                    if (boat != null && boat.getFirstPassenger() != null && this.fisherman.equals(boat.getFirstPassenger())) {
                        this.fisherman.setSailPos(coastPos);
                    } else{
                        if(fisherman.needsToSleep()){
                            setWorkState(SLEEP);
                        }

                        else if(fisherman.needsToDeposit()){
                            setWorkState(DEPOSIT);
                        }

                        else if(fisherman.needsToGetFood()){
                            setWorkState(UPKEEP);
                        }
                        else {
                            this.moveToPos(coastPos);
                            fisherman.stopRiding();
                        }
                    }

                    double distance = fisherman.distanceToSqr(coastPos.getX(), coastPos.getY(), coastPos.getZ());
                    if(distance < 6.0F) { //valid value example: distance = 3.2
                        fisherman.stopRiding();
                        this.setWorkState(STOP);
                    }
                    else if(++timer > 200){
                        fisherman.stopRiding();
                        this.setWorkState(STOP);
                        timer = 0;
                    }
                }
                else
                    this.setWorkState(IDLE);
            }

            case STOP -> {
                fisherman.stopRiding();
                if(fisherman.needsToSleep()){
                    setWorkState(SLEEP);
                }

                else if(fisherman.needsToDeposit()){
                    setWorkState(DEPOSIT);
                }

                else if(fisherman.needsToGetFood()){
                    setWorkState(UPKEEP);
                }
                else{
                    this.fisherman.walkTowards(coastPos, 1);

                    double distance = fisherman.distanceToSqr(coastPos.getX(), coastPos.getY(), coastPos.getZ());
                    if(distance < 5.5F) { //valid value example: distance = 3.2
                        stop();
                    }
                }
            }

            case DEPOSIT -> {
                //Separate AI doing stuff
                fisherman.stopRiding();
                if(!fisherman.needsToDeposit()){
                    setWorkState(STOP);
                }
            }

            case UPKEEP -> {
                //Separate AI doing stuff
                fisherman.stopRiding();
                if(!fisherman.needsToGetFood()){
                    setWorkState(STOP);
                }
            }

            case SLEEP -> {
                //Separate AI doing stuff
                fisherman.stopRiding();
                if(!fisherman.needsToSleep()){
                    setWorkState(STOP);
                }
            }
        }
    }

    private void setWorkState(FishermanEntity.@NotNull State state) {
        timer = 0;
        this.state = state;
        this.fisherman.setState(state.getIndex());
        if(state == IDLE) fisherman.setSailPos(null);
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

    private BlockPos findWaterBlocks() {
        List<BlockPos> waterBlocks = new ArrayList<>();
        Direction direction = this.fisherman.getFishingDirection();
        Direction directionR = direction.getClockWise();
        Direction directionL = direction.getCounterClockWise();

        if (coastPos != null) {
            int length = getDistanceWithWater(coastPos, direction);
            int lengthR = getDistanceWithWater(coastPos, directionR);
            int lengthL = getDistanceWithWater(coastPos, directionL);
            int lengthB = getDistanceWithWater(coastPos, direction.getOpposite());

            for (int x = 0; x <= length; ++x) {
                BlockPos pos = this.coastPos.relative(direction, x);
                BlockState targetBlock = this.fisherman.level.getBlockState(pos);
                if(DEBUG) fisherman.level.setBlock(pos.above(4), Blocks.ICE.defaultBlockState(), 3);

                double distance = fisherman.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
                if (targetBlock.is(Blocks.WATER) && distance > fishingRange) {
                    for (int i = 0; i < 4; i++) {
                        if (this.fisherman.level.getBlockState(pos.above(i)).isAir() && i == 3) {
                            waterBlocks.add(pos);
                            break;
                        }
                    }
                }
            }

            for (int x = 0; x <= lengthR; ++x) {
                BlockPos pos = this.coastPos.relative(directionR, x);
                BlockState targetBlock = this.fisherman.level.getBlockState(pos);
                if(DEBUG) fisherman.level.setBlock(pos.above(4), Blocks.ICE.defaultBlockState(), 3);

                double distance = fisherman.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
                if (targetBlock.is(Blocks.WATER) && distance > fishingRange) {
                    for (int i = 0; i < 4; i++) {
                        if (this.fisherman.level.getBlockState(pos.above(i)).isAir() && i == 3) {
                            waterBlocks.add(pos);
                            break;
                        }
                    }
                }
            }

            for (int x = 0; x <= lengthL; ++x) {
                BlockPos pos = this.coastPos.relative(directionL, x);
                BlockState targetBlock = this.fisherman.level.getBlockState(pos);
                if(DEBUG) fisherman.level.setBlock(pos.above(4), Blocks.ICE.defaultBlockState(), 3);

                double distance = fisherman.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
                if (targetBlock.is(Blocks.WATER) && distance > fishingRange) {
                    for (int i = 0; i < 4; i++) {
                        if (this.fisherman.level.getBlockState(pos.above(i)).isAir() && i == 3) {
                            waterBlocks.add(pos);
                            break;
                        }
                    }
                }
            }

            for (int x = 0; x <= lengthB; ++x) {
                BlockPos pos = this.coastPos.relative(direction.getOpposite(), x);
                BlockState targetBlock = this.fisherman.level.getBlockState(pos);
                if(DEBUG) fisherman.level.setBlock(pos.above(4), Blocks.ICE.defaultBlockState(), 3);

                double distance = fisherman.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
                if (targetBlock.is(Blocks.WATER) && distance > fishingRange) {
                    for (int i = 0; i < 4; i++) {
                        if (this.fisherman.level.getBlockState(pos.above(i)).isAir() && i == 3) {
                            waterBlocks.add(pos);
                            break;
                        }
                    }
                }
            }
        }

        waterBlocks.sort(Comparator.comparing(this::getWaterDepth).reversed());
        if(fishingRange < 10){
            if(!waterBlocks.isEmpty()){
                List<BlockPos> validWaterSpots = new ArrayList<>();
                for(BlockPos pos : waterBlocks){
                    if(isValidFishingSpot(pos, true)){
                        validWaterSpots.add(pos);
                    }
                }

                BlockPos fishingSpot;
                if(validWaterSpots.isEmpty() && !waterBlocks.isEmpty()){//do not simplify
                    fishingSpot = waterBlocks.get((waterBlocks.size() / 2));
                }
                else fishingSpot = validWaterSpots.get(0);

                if(DEBUG) fisherman.getCommandSenderWorld().setBlock(new BlockPos(fishingSpot.getX(), fishingSpot.getY() + 5, fishingSpot.getZ()), Blocks.PACKED_ICE.defaultBlockState(), 3);

                return fishingSpot;
            }
            BlockPos fishingSpot;
            if(validWaterSpots.isEmpty() && !waterBlocks.isEmpty()){
                fishingSpot = waterBlocks.get((waterBlocks.size() / 2));
            }
            else fishingSpot = validWaterSpots.get(0);

            if(DEBUG)fisherman.level.setBlock(new BlockPos(fishingSpot.getX(), fishingSpot.getY() + 5, fishingSpot.getZ()), Blocks.PACKED_ICE.defaultBlockState(), 3);

            return fishingSpot;

            else
                return null;

        }
        else{
              if(!waterBlocks.isEmpty()){
                List<BlockPos> validWaterSpots = new ArrayList<>();
                for(BlockPos pos : waterBlocks){
                    if(isValidFishingSpot(pos, false)){
                        validWaterSpots.add(pos);
                    }
                }

                BlockPos fishingSpot;
                if(validWaterSpots.isEmpty() && !waterBlocks.isEmpty()){//do not simplify
                    fishingSpot = waterBlocks.get((waterBlocks.size() / 2));
                }
                else {
                    validWaterSpots.sort(Comparator.comparing(this::getDistanceToFisherStartPos).reversed());
                    fishingSpot = validWaterSpots.get(validWaterSpots.size() / 2);
                }

                if(DEBUG) fisherman.getCommandSenderWorld().setBlock(new BlockPos(fishingSpot.getX(), fishingSpot.getY() + 5, fishingSpot.getZ()), Blocks.PACKED_ICE.defaultBlockState(), 3);

                return fishingSpot;
            }
            else
                return null;
        }
    }

    private double getDistanceToFisherStartPos(BlockPos pos){
        return fisherman.getStartPos().distToCenterSqr(pos.getX(), fisherman.getStartPos().getY(), pos.getZ());//Horizontal distance
    }

    public void spawnFishingLoot() {
        int depth;
        if (fishingPos != null) {
            depth = 1 + ((this.getWaterDepth(fishingPos) + fishingRange) / 10);
        }
        else
            depth = 1;
        double time = EnchantmentHelper.getFishingSpeedBonus(this.fisherman.getItemInHand(InteractionHand.MAIN_HAND));
        this.fishingTimer = (int) (500 - 100*time + fisherman.getRandom().nextInt(1000) / depth);
        double luck = 0.1D;
        double luckFromTool = EnchantmentHelper.getFishingLuckBonus(this.fisherman.getItemInHand(InteractionHand.MAIN_HAND));
        LootContext.Builder lootcontext$builder = (new LootContext.Builder((ServerLevel)fisherman.level))
                .withParameter(LootContextParams.ORIGIN, fisherman.position())
                .withParameter(LootContextParams.TOOL, this.fisherman.getItemInHand(InteractionHand.MAIN_HAND)).withLuck((float) (luck + luckFromTool));

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
                // TODO: Create FishingBobberEntity compatible with AbstractEntityWorker.
                // WorkersFishingHook fishingHook = new WorkersFishingHook(this.fisherman, fisherman.level, fishingPos);
                // fisherman.level.addFreshEntity(fishingHook);
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

    private boolean isValidFishingSpot(BlockPos pos1, boolean coastFishing){
        int range = coastFishing ? 2 : 4;

        for(int i = -range; i <= range; i++){
            for(int k = -range; k <= range; k++) {
                BlockPos pos = pos1.offset(i, 0, k);
                BlockState state = this.fisherman.level.getBlockState(pos);

                if (state.is(Blocks.WATER)){
                    return true;
                }
            }
        }
        return false;
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

    private BlockPos getCoastPos() {
        List<BlockPos> list = new ArrayList<>();
        for(int i = -10; i <= 10; i++){
            for(int k = -10; k <= 10; k++) {
                BlockPos pos = fisherman.getStartPos().offset(i, 0, k);
                BlockState state = this.fisherman.level.getBlockState(pos);
                BlockState targetBlockN = this.fisherman.level.getBlockState(pos.north());
                BlockState targetBlockE = this.fisherman.level.getBlockState(pos.east());
                BlockState targetBlockS = this.fisherman.level.getBlockState(pos.south());
                BlockState targetBlockW = this.fisherman.level.getBlockState(pos.west());

                if (state.is(Blocks.WATER) && (targetBlockN.is(Blocks.WATER) || targetBlockE.is(Blocks.WATER) || targetBlockS.is(Blocks.WATER) || targetBlockW.is(Blocks.WATER))){
                    list.add(pos);
                }
            }
        }

        if(list.isEmpty()){
            if (fisherman.getOwner() != null) fisherman.tellPlayer(fisherman.getOwner(), Translatable.TEXT_FISHER_NO_WATER);

            this.fisherman.setIsWorking(false, true);
            this.fisherman.clearStartPos();
            this.fisherman.stopRiding();
            this.stop();
            return null;
        }
        else {
            list.sort(Comparator.comparing(blockPos -> blockPos.distSqr(fisherman.getStartPos())));
            return list.get(0);
        }
    }

    private int getDistanceWithWater(BlockPos pos, Direction direction){
        int distance = 0;
        for(int i = 0; i < fishingRange; i++){
            BlockState targetBlockN = this.fisherman.level.getBlockState(pos.relative(direction, i));
            if (targetBlockN.is(Blocks.WATER)){
                distance++;
            }
            else break;
        }
        return distance;
    }

    @Nullable
    private BlockPos getWaterField() {
        List<BlockPos> list = new ArrayList<>();

        int distanceNorth = Math.min(fishingRange, getDistanceWithWater(coastPos, Direction.NORTH));
        int distanceEast = Math.min(fishingRange, getDistanceWithWater(coastPos, Direction.EAST));
        int distanceSouth = Math.min(fishingRange,getDistanceWithWater(coastPos, Direction.SOUTH));
        int distanceWest = Math.min(fishingRange, getDistanceWithWater(coastPos, Direction.WEST));

        boolean isNorth = distanceNorth > distanceSouth;
        boolean isEast = distanceEast > distanceWest;

        int maxX = Math.max(distanceNorth, distanceSouth);
        int maxZ = Math.max(distanceEast, distanceWest);

        for(int x = 0; x < maxX; x++){
            for(int z = 0; z < maxZ; z++) {
                int x1 = isNorth || !isEast ? x * -1 : x;
                int z1 = isNorth || isEast ? z * -1 : z;

                BlockPos pos = this.coastPos.offset(x1, 0, z1);

                BlockState targetBlock = this.fisherman.level.getBlockState(pos);

                if (targetBlock.is(Blocks.WATER)) {
                    list.add(pos);
                }
            }
        }

        //if(!list.isEmpty()) for(BlockPos pos: list) fisherman.level.setBlock(new BlockPos(pos.getX(), pos.getY() + 2, pos.getZ()), Blocks.ICE.defaultBlockState(), 3);

        list.sort(Comparator.comparing(blockPos -> blockPos.distSqr(fisherman.getStartPos())));
        //list.sort(Comparator.comparing(this::getWaterDepth));
        //Collections.reverse(list);

        BlockPos pos = null;
        if(!list.isEmpty()){
            int rdm = fisherman.getRandom().nextInt(list.size()/2);
            int index = list.size()/2 + rdm;
            if(index >= list.size()) index = list.size() - 1;

            pos = list.get(index);

            //fisherman.level.setBlock(new BlockPos(pos.getX(), pos.getY() + 3, pos.getZ()), Blocks.ICE.defaultBlockState(), 3);
        }
        return pos;
    }
}
