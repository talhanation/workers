package com.talhanation.workers.entities.ai;

import com.mojang.authlib.GameProfile;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.compat.FarmersDelight;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.FarmerEntity;
import com.talhanation.workers.entities.workarea.CropArea;
import com.talhanation.workers.world.NeededItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import javax.annotation.Nullable;
import java.util.*;

public class FarmerWorkGoal extends Goal {

    public FarmerEntity farmer;
    public State state;
    public String errorMessage;
    public boolean errorMessageDone;
    public BlockPos blockPos;
    public Stack<BlockPos> stackToPlant;
    public Stack<BlockPos> stackToBreak;
    public Stack<BlockPos> stackToPlow;
    public Stack<BlockPos> stackToBoneMeal;
    public Stack<BlockPos> stackToPick;

    private static final GameProfile FARMER_PICK_PROFILE = new GameProfile(
            java.util.UUID.fromString("c2e5a7d3-9f1b-4e6a-8c0d-2f3e4a5b6c70"), "WorkerFarmerPick"
    );
    public List<NeededItem> neededItems = new ArrayList<>();
    public FarmerWorkGoal(FarmerEntity farmer) {
        this.farmer = farmer;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return !farmer.needsToSleep() && farmer.shouldWork() && !farmer.needsToGetToChest() && this.isCropAreaNotRemoved();
    }

    private boolean isCropAreaNotRemoved() {
        if(farmer.currentCropArea == null || !farmer.currentCropArea.isRemoved()) return true;
        else {
            farmer.currentCropArea = null;
        }
        return false;
    }

    @Override
    public void start() {
        super.start();
        if(farmer.getFollowState() == 0) farmer.setFollowState(6); //Working
        setState(State.SELECT_WORK_AREA);
    }
    boolean workDone;
    int cooldown;
    @Override
    public void tick() {
        super.tick();
        if(this.farmer.getCommandSenderWorld().isClientSide()) return;

        if(state == null) return;
        if(blockPos != null) this.farmer.getLookControl().setLookAt(blockPos.getCenter());
        if(farmer.tickCount % 5 != 0) return;

        if(!isCropAreaNotRemoved()) return;

        if(state != State.SELECT_WORK_AREA && this.farmer.currentCropArea == null){
            setState(State.SELECT_WORK_AREA);
            return;
        }

        if(blockPos != null && moveToPosition(blockPos, 20)) return;

        switch(state){
            case SELECT_WORK_AREA -> {
                if(this.farmer.currentCropArea != null) setState(State.MOVE_TO_WORK_AREA);

                if(++cooldown < farmer.getRandom().nextInt(300)) return;
                this.cooldown = 0;

                List<CropArea> areas = getAvailableWorkAreasByPriority((ServerLevel) farmer.getCommandSenderWorld(), farmer, this.farmer.currentCropArea);

                if (!areas.isEmpty()) {
                    this.farmer.currentCropArea = areas.get(0);
                }

                if(this.farmer.currentCropArea == null) return;

                this.farmer.currentCropArea.setBeingWorkedOn(true);
                this.farmer.currentCropArea.setTime(0);
                this.workDone = false;
                setState(State.MOVE_TO_WORK_AREA);
            }

            case MOVE_TO_WORK_AREA ->{
                this.blockPos = null;
                if(this.moveToPosition(this.farmer.currentCropArea.getOnPos(), 20)) return;
                setState(State.PREPARE_BONE_MEAL);
            }
            case PREPARE_BONE_MEAL -> {
                this.farmer.currentCropArea.scanBoneMealArea();

                this.stackToBoneMeal = this.farmer.currentCropArea.stackToBoneMeal;

                if(stackToBoneMeal.isEmpty()){
                    setState(State.PREPARE_PICK_BUSHES);
                    return;
                }

                farmer.switchMainHandItem(itemStack -> itemStack.getItem() instanceof BoneMealItem);

                boolean hasBoneMeal = farmer.getMainHandItem().getItem() instanceof BoneMealItem;
                if(!hasBoneMeal){
                    this.neededItems.add(new NeededItem(stack -> stack.getItem() instanceof BoneMealItem, stackToBoneMeal.size(), false, this.farmer.currentCropArea.getUUID()));
                    this.blockPos = null;
                    setState(State.PREPARE_PICK_BUSHES);
                    return;
                }

                setState(State.BONE_MEAL);
            }
            case BONE_MEAL -> {
                if(this.useBoneMeal(stackToBoneMeal)) return;

                setState(State.PREPARE_PICK_BUSHES);
            }
            case PREPARE_PICK_BUSHES -> {
                this.farmer.currentCropArea.scanPickArea();

                this.stackToPick = this.farmer.currentCropArea.stackToPick;

                if(stackToPick.isEmpty()){
                    setState(State.PREPARE_BREAK_BLOCKS);
                    return;
                }

                setState(State.PICK_BUSHES);
            }
            case PICK_BUSHES -> {
                if(this.pickBushes(stackToPick)) return;

                setState(State.PREPARE_BREAK_BLOCKS);
            }
            case PREPARE_BREAK_BLOCKS -> {
                this.farmer.currentCropArea.scanBreakArea();

                this.stackToBreak = this.farmer.currentCropArea.stackToBreak;

                if(stackToBreak.isEmpty()){
                    setState(State.PREPARE_PLOWING);
                    return;
                }

                if(WorkersMain.isFarmersDelightInstalled){
                    farmer.switchMainHandItem(itemStack -> FarmersDelight.isKnife(itemStack));

                    if(!FarmersDelight.isKnife(farmer.getMainHandItem())){
                        this.neededItems.add(new NeededItem(FarmersDelight::isKnife, 1, false, this.farmer.currentCropArea.getUUID()));
                    }
                }
                else{
                    farmer.switchMainHandItem(itemStack -> itemStack.getItem().getDefaultInstance().isEmpty());
                }

                setState(State.BREAK_BLOCKS);
            }
            case BREAK_BLOCKS -> {
                if(this.breakBlocks(this.stackToBreak)) return;

                setState(State.PREPARE_WATER_SPOT);
            }
            case PREPARE_WATER_SPOT -> {
                if(this.farmer.currentCropArea.fieldType == CropArea.FieldType.RICE){
                    setState(State.PREPARE_PLOWING);
                    return;
                }

                BlockState centerPosState = farmer.getCommandSenderWorld().getBlockState(this.farmer.currentCropArea.getWaterPosCenter());
                if(centerPosState.isAir()){

                    ItemStack itemStack = farmer.getMatchingItem(item -> farmer.isBucketWithWater(item));
                    if(itemStack == null){
                        farmer.addNeededItem(new NeededItem(item -> farmer.isBucketWithWater(item),  1, true));
                        return;
                    }
                    else if(itemStack.getItem() instanceof BucketItem bucketItem){
                        farmer.switchMainHandItem(item -> farmer.isBucketWithWater(item));

                        bucketItem.emptyContents(null,  farmer.getCommandSenderWorld(), this.farmer.currentCropArea.getWaterPosCenter(), null);
                    }
                }

                setState(State.PREPARE_PLOWING);
            }

            case PREPARE_PLOWING -> {
                this.farmer.currentCropArea.scanPlowArea();

                this.stackToPlow = this.farmer.currentCropArea.stackToPlow;
                if(stackToPlow.isEmpty()){
                    setState(State.PREPARE_PLANT_SEEDS);
                    return;
                }

                farmer.switchMainHandItem(itemStack -> itemStack.getItem() instanceof HoeItem);

                boolean hasHoe = farmer.getMainHandItem().getItem() instanceof HoeItem;
                if(!hasHoe){
                    this.neededItems.add(new NeededItem(stack -> stack.getItem() instanceof HoeItem, 1, true, this.farmer.currentCropArea.getUUID()));
                    this.blockPos = null;
                    setState(State.PREPARE_PLANT_SEEDS);
                    return;
                }

                setState(State.PLOWING);
            }
            case PLOWING -> {
                if(this.plowBlocks(stackToPlow)) return;

                setState(State.PREPARE_PLANT_SEEDS);
            }

            case PREPARE_PLANT_SEEDS -> {
                this.farmer.currentCropArea.scanPlantArea();

                this.stackToPlant = this.farmer.currentCropArea.stackToPlant;
                if(stackToPlant.isEmpty()){
                    setState(State.DONE);
                    return;
                }

                if(this.farmer.currentCropArea.getSeedStack().isEmpty()){
                    setState(State.DONE);
                    return;
                }

                ItemStack seedFromInv = farmer.getMatchingItem(itemStack -> itemStack.is(this.farmer.currentCropArea.getSeedStack().getItem()));
                if(seedFromInv == null){
                    ItemStack seedStack = this.farmer.currentCropArea.getSeedStack();
                    this.neededItems.add(new NeededItem(itemStack -> ItemStack.isSameItemSameTags(itemStack, seedStack),  stackToPlant.size(), true, this.farmer.currentCropArea.getUUID()));
                    setState(State.DONE);
                    this.blockPos = null;
                    return;
                }

                this.farmer.switchMainHandItem(itemStack -> itemStack.is(this.farmer.currentCropArea.getSeedStack().getItem()));

                setState(State.PLANT_SEEDS);
            }

            case PLANT_SEEDS -> {
                if(this.plantSeeds(stackToPlant)) return;

                setState(State.DONE);
            }

            case DONE -> {
                if(!workDone){
                    workDone = true;
                    setState(State.SELECT_WORK_AREA);

                    this.farmer.currentCropArea.setBeingWorkedOn(false);
                    blockPos = null;
                    this.farmer.currentCropArea = null;
                    if(this.farmer.getFollowState() == 6) this.farmer.setFollowState(0);//Wander

                    if(!this.neededItems.isEmpty()){
                        for(NeededItem neededItem : neededItems){
                            this.farmer.addNeededItem(neededItem);
                        }
                        this.neededItems.clear();
                    }
                }
            }

            case ERROR ->{
                if(!errorMessageDone){
                    errorMessageDone = true;
                }
            }
        }
    }

    public void setState(State state) {
        //if(farmer.getOwner() != null) farmer.getOwner().sendSystemMessage(Component.literal(state.toString()));
        this.state = state;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public boolean plantSeeds(Stack<BlockPos> positions){
        if(positions != null){
            ItemStack seedFromInv = farmer.getMatchingItem(itemStack -> itemStack.is(this.farmer.currentCropArea.getSeedStack().getItem()));
            if(seedFromInv == null){
                setState(State.PREPARE_PLANT_SEEDS);
                return false;
            }

            if(blockPos == null){
                if(!positions.isEmpty()) blockPos = positions.pop();
                return blockPos != null;
            }

            BlockState state = farmer.getCommandSenderWorld().getBlockState(blockPos);

            if(this.farmer.currentCropArea.fieldType == CropArea.FieldType.RICE){
                if(this.farmer.currentCropArea.isRiceCrop(state) || this.farmer.currentCropArea.isRicePanicles(state)){
                    if(!positions.isEmpty()){
                        blockPos = positions.pop();
                    }
                    else{
                        this.blockPos = null;
                        return false;
                    }
                }
                else if(FarmersDelight.plantRice((ServerLevel) farmer.getCommandSenderWorld(), blockPos, seedFromInv)){
                    farmer.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                    seedFromInv.shrink(1);
                    this.farmer.swing(InteractionHand.MAIN_HAND);
                }
                return true;
            }

            if(this.farmer.currentCropArea.fieldType == CropArea.FieldType.BUSH){
                boolean alreadyPlanted = seedFromInv.getItem() instanceof BlockItem bi && state.is(bi.getBlock());
                if(alreadyPlanted){
                    if(!positions.isEmpty()){
                        blockPos = positions.pop();
                    }
                    else{
                        this.blockPos = null;
                        return false;
                    }
                }
                else if(plantViaUseOn(blockPos, seedFromInv)){
                    farmer.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                    seedFromInv.shrink(1);
                    this.farmer.swing(InteractionHand.MAIN_HAND);
                }
                else{
                    // Placement failed (e.g. tomato without a rope above) - skip this position
                    if(!positions.isEmpty()){
                        blockPos = positions.pop();
                    }
                    else{
                        this.blockPos = null;
                        return false;
                    }
                }
                return true;
            }

            if(state.getBlock() instanceof CropBlock || state.getBlock() instanceof StemBlock){
                if(!positions.isEmpty()){
                    blockPos = positions.pop();
                }
                else{
                    this.blockPos = null;
                    return false;
                }
            }
            else if (seedFromInv.getItem() instanceof BlockItem blockItem) {
                farmer.getCommandSenderWorld().setBlockAndUpdate(blockPos, blockItem.getBlock().defaultBlockState());

                farmer.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                seedFromInv.shrink(1);
                this.farmer.swing(InteractionHand.MAIN_HAND);
            }
            else if (seedFromInv.getItem() instanceof IPlantable plantable) {
                if (plantable.getPlantType(farmer.getCommandSenderWorld(), blockPos) == PlantType.CROP) {
                    farmer.getCommandSenderWorld().setBlock(blockPos, plantable.getPlant(farmer.getCommandSenderWorld(), blockPos), 3);

                    farmer.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                    seedFromInv.shrink(1);
                    this.farmer.swing(InteractionHand.MAIN_HAND);
                }
            }
            return true;
        }
        this.blockPos = null;
        return false;
    }

    public boolean useBoneMeal(Stack<BlockPos> positions){
        if(positions != null){
            ItemStack boneMealFromInv = farmer.getMatchingItem(itemStack -> itemStack.getItem() instanceof BoneMealItem);
            if(boneMealFromInv == null){
                setState(State.PREPARE_BONE_MEAL);
                return false;
            }

            if(blockPos == null){
                if(!positions.isEmpty()) blockPos = positions.pop();
                return blockPos != null;
            }

            BlockState state = farmer.getCommandSenderWorld().getBlockState(blockPos);
            if(!this.farmer.currentCropArea.isBoneMealable(state, farmer.getCommandSenderWorld(), blockPos)){
                if(!positions.isEmpty()){
                    blockPos = positions.pop();
                }
                else{
                    this.blockPos = null;
                    return false;
                }
            }
            else{
                farmer.switchMainHandItem(itemStack -> itemStack.getItem() instanceof BoneMealItem);

                BoneMealItem.growCrop(boneMealFromInv, farmer.getCommandSenderWorld(), blockPos);
                farmer.getCommandSenderWorld().levelEvent(1505, blockPos, 0);
                this.farmer.swing(InteractionHand.MAIN_HAND);
            }
            return true;
        }
        this.blockPos = null;
        return false;
    }

    public boolean pickBushes(Stack<BlockPos> positions){
        if(positions != null){
            if(blockPos == null){
                if(!positions.isEmpty()) blockPos = positions.pop();
                return blockPos != null;
            }

            BlockState state = farmer.getCommandSenderWorld().getBlockState(blockPos);
            if(!this.farmer.currentCropArea.isPickable(state)){
                if(!positions.isEmpty()){
                    blockPos = positions.pop();
                }
                else{
                    this.blockPos = null;
                    return false;
                }
            }
            else{
                pickAt(blockPos, state);
                this.farmer.swing(InteractionHand.MAIN_HAND);
            }
            return true;
        }
        this.blockPos = null;
        return false;
    }

    private void pickAt(BlockPos pos, BlockState state){
        Level level = farmer.getCommandSenderWorld();
        if (!(level instanceof ServerLevel serverLevel)) return;

        FakePlayer fake = FakePlayerFactory.get(serverLevel, FARMER_PICK_PROFILE);
        fake.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        Vec3 hitVec = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);
        state.use(level, fake, InteractionHand.MAIN_HAND, hitResult);
    }

    /**
     * Plants a BUSH-type seed by simulating a right-click of the seed item on
     * the soil block (the block under {@code targetPos}). This routes through
     * the item's own placement logic (canSurvive checks, FD's rope-detection
     * for tomatoes, etc.) instead of blindly setBlock'ing the default state.
     */
    private boolean plantViaUseOn(BlockPos targetPos, ItemStack seed){
        Level level = farmer.getCommandSenderWorld();
        if (!(level instanceof ServerLevel serverLevel)) return false;

        BlockPos soilPos = targetPos.below();

        FakePlayer fake = FakePlayerFactory.get(serverLevel, FARMER_PICK_PROFILE);
        fake.setPos(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);

        ItemStack single = seed.copy();
        single.setCount(1);
        fake.setItemInHand(InteractionHand.MAIN_HAND, single);

        Vec3 hitVec = new Vec3(soilPos.getX() + 0.5, soilPos.getY() + 1.0, soilPos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, soilPos, false);
        UseOnContext context = new UseOnContext(level, fake, InteractionHand.MAIN_HAND, single, hitResult);

        InteractionResult result = single.useOn(context);
        return result.consumesAction();
    }

    public boolean plowBlocks(Stack<BlockPos> positions){
        if(positions != null){
            boolean hasTool = farmer.getMainHandItem().getItem() instanceof HoeItem;
            if(!hasTool){
                setState(State.PREPARE_PLOWING);
                return true;
            }

            if(blockPos == null){
                if(!positions.isEmpty()) blockPos = positions.pop();
                return blockPos != null;
            }

            BlockState state = farmer.getCommandSenderWorld().getBlockState(blockPos);
            if(state.getBlock() instanceof FarmBlock){
                if(!positions.isEmpty()){
                    blockPos = positions.pop();
                }
                else{
                    this.blockPos = null;
                    return false;
                }
            }
            else{
                this.farmer.swing(InteractionHand.MAIN_HAND);
                farmer.getCommandSenderWorld().setBlock(blockPos, Blocks.FARMLAND.defaultBlockState(), 3);
                farmer.getCommandSenderWorld().playSound(null, blockPos.getX(), blockPos.getY(), blockPos.getZ(), SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                this.farmer.damageMainHandItem();
            }
            return true;
        }
        return false;
    }
    int blockBreakTime;
    public boolean breakBlocks(Stack<BlockPos> positions){
        if(positions != null){
            if(blockPos == null){
                if(!positions.isEmpty()) blockPos = positions.pop();
                return blockPos != null;
            }

            if(AbstractWorkerEntity.isPosBroken(blockPos, this.farmer.getCommandSenderWorld(), true)){
                if(!positions.isEmpty()){
                    blockPos = positions.pop();
                }
                else{
                    this.blockPos = null;
                    return false;
                }
                blockBreakTime = 0;

            }
            else{
                this.farmer.mineBlock(blockPos);
                this.farmer.swing(InteractionHand.MAIN_HAND);
            }
            return true;
        }
        return false;
    }

    public static List<CropArea> getAvailableWorkAreasByPriority(ServerLevel level, FarmerEntity farmer, @Nullable CropArea currentArea) {
        List<CropArea> list = level.getEntitiesOfClass(CropArea.class, farmer.getBoundingBox().inflate(64));

        Map<CropArea, Integer> priorityMap = new HashMap<>();

        for (CropArea area : list) {
            if (area == null || area == currentArea || !area.canWorkHere(farmer)) continue;

            int priority = 0;

            boolean perfectCandidate = area.isWorkerPerfectCandidate(farmer);

            if (perfectCandidate) {
                priority += 10;
            } else {
                priority += 1;
            }

            if (!area.isBeingWorkedOn()) {
                priority += 10;
            }

            priority += area.getTime() * 10;

            priorityMap.put(area, priority);
        }

        List<CropArea> sorted = new ArrayList<>(priorityMap.keySet());
        sorted.sort((a, b) -> Integer.compare(priorityMap.get(b), priorityMap.get(a)));

        return sorted;
    }



    public boolean moveToPosition(BlockPos pos, int threshold){
        if(pos == null){
            return false;
        }
        else{
            double distance = farmer.getHorizontalDistanceTo(pos.getCenter());
            if(distance < threshold){
                farmer.getNavigation().stop();
                return false;
            }
            else{
                farmer.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.8F);
                farmer.setFollowState(6); //Working
                farmer.getLookControl().setLookAt(pos.getCenter());
            }
            return true;
        }
    }

    public enum State{
        SELECT_WORK_AREA,
        MOVE_TO_WORK_AREA,
        PREPARE_BONE_MEAL,
        BONE_MEAL,
        PREPARE_PICK_BUSHES,
        PICK_BUSHES,
        PREPARE_BREAK_BLOCKS,
        BREAK_BLOCKS,
        PREPARE_WATER_SPOT,
        PREPARE_PLOWING,
        PLOWING,
        PREPARE_PLANT_SEEDS,
        PLANT_SEEDS,
        DONE,
        ERROR

    }
}