package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.client.gui.CropAreaScreen;
import com.talhanation.workers.entities.FarmerEntity;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Stack;

public class CropArea extends AbstractWorkAreaEntity {

    public static final EntityDataAccessor<ItemStack> SEED_STACK = SynchedEntityData.defineId(CropArea.class, EntityDataSerializers.ITEM_STACK);

    public Stack<BlockPos> stackToPlant = new Stack<>();
    public Stack<BlockPos> stackToBreak = new Stack<>();
    public Stack<BlockPos> stackToPlow = new Stack<>();
    public FieldType fieldType;

    public CropArea(EntityType<?> type, Level level) {
        super(type, level);
    }
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SEED_STACK, ItemStack.EMPTY);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if(tag.contains("seedItem")){
            ItemStack stack = ItemStack.of(tag.getCompound("seedItem"));
            this.setSeedStack(stack);
        }
        fieldType = FieldType.fromIndex(tag.getInt("fieldType"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag nbt = new CompoundTag();
        this.getSeedStack().save(nbt);
        tag.put("seedItem", nbt);
        tag.putInt("fieldType", fieldType.getIndex());
    }

    public Item getRenderItem(){
        return Items.IRON_HOE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Screen getScreen(Player player) {
        return new CropAreaScreen(this, player);
    }
    public void scanBreakArea(){
        if(area == null) area = this.getArea();

        stackToBreak.clear();
        Level level = this.getCommandSenderWorld();
        BlockPos centerPos = this.getWaterPosCenter();

        if(centerPos == null) return;

        Fluid centerPosFluid = level.getFluidState(centerPos).getType();
        if(!(centerPosFluid == Fluids.WATER || centerPosFluid == Fluids.FLOWING_WATER)) {
            this.stackToBreak.push(centerPos);
        }

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockState state = level.getBlockState(pos);

            BlockPos below = pos.below();
            BlockState stateBelow = level.getBlockState(below);
            if(fieldType == FieldType.STEM){
                if (state.getBlock() instanceof MelonBlock || state.getBlock() instanceof PumpkinBlock  || (isBush(state) && !isStem(state))) {
                    stackToBreak.push(pos.immutable());
                }
            }
            else{
                if(isFarmland(stateBelow) || isTillAble(stateBelow)){
                    if(isCropDone(state) || (isBush(state) && !isCrop(state))){
                        this.stackToBreak.push(pos.immutable());
                    }
                }
            }
        });
    }
    public void scanPlowArea(){
        if(area == null) area = this.getArea();
        stackToPlow.clear();
        Level level = this.getCommandSenderWorld();
        BlockPos origin = this.getOnPos();

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            if (fieldType == FieldType.STEM) {
                int col = getColumnIndex(origin, pos, getFacing());
                if (col == 2 || col == 3 || col == 7 || col == 8) {
                    BlockState state = level.getBlockState(pos);
                    BlockState above = level.getBlockState(pos.above());
                    if (isTillAble(state) && isAir(above)) {
                        stackToPlow.push(pos.immutable());
                    }
                }
            } else {
                // altes Crop/Default-Verhalten
                BlockState state = level.getBlockState(pos);
                BlockPos above = pos.above();
                BlockState stateAbove = level.getBlockState(above);
                if(isTillAble(state) && isAir(stateAbove)){
                    stackToPlow.push(pos.immutable());
                }
            }
        });
    }
    public void scanPlantArea(){
        stackToPlant.clear();
        Level level = this.getCommandSenderWorld();
        BlockPos origin = this.getOnPos();

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            if (fieldType == FieldType.STEM) {
                int col = getColumnIndex(origin, pos, getFacing());
                BlockState state = level.getBlockState(pos);
                BlockState below = level.getBlockState(pos.below());

                if ((col == 2 || col == 3 || col == 7 || col == 8)) {
                    // Muss gepflanzt werden
                    if (isAir(state) && isFarmland(below)) {
                        stackToPlant.push(pos.immutable());
                    }
                } else {
                    // Rest muss Air sein (keine Seeds rein!)
                }
            } else {
                // altes Crop-Verhalten
                BlockState state = level.getBlockState(pos);
                BlockState below = level.getBlockState(pos.below());
                if(isAir(state) && isFarmland(below)){
                    stackToPlant.push(pos.immutable());
                }
            }
        });
    }
    private int getColumnIndex(BlockPos origin, BlockPos pos, Direction facing) {
        int dx = Math.abs(pos.getX() - origin.getX());
        int dz = Math.abs(pos.getZ() - origin.getZ());

        return switch (facing) {
            case NORTH -> dx + 1;
            case SOUTH -> (8 - dx) + 1;
            case WEST  -> dz + 1;
            case EAST  -> (8 - dz) + 1;
            default    -> 0;
        };
    }
    public BlockPos getWaterPosCenter() {
        BlockPos origin = this.getOnPos();
        Direction facing = this.getFacing();

        return switch (facing) {
            case NORTH -> origin.offset(4, 0, -4);
            case SOUTH -> origin.offset(-4, 0, 4);
            case WEST  -> origin.offset(-4, 0, -4);
            case EAST  -> origin.offset(4, 0, 4);
            default    -> origin;
        };
    }

    public boolean isWorkerPerfectCandidate(FarmerEntity farmer) {
        if (farmer.getMatchingItem(stack -> stack.getItem() instanceof HoeItem) == ItemStack.EMPTY) {
            return false;
        }

        if (farmer.getMatchingItem(stack -> stack.is(this.getSeedStack().getItem())) == ItemStack.EMPTY) {
            return false;
        }

        return true;
    }

    public boolean isFarmland(BlockState state){
        return state.getBlock() instanceof FarmBlock;
    }
    public boolean isTillAble(BlockState state){
        return FarmerEntity.TILLABLES.contains(state.getBlock());
    }

    public boolean isBush(BlockState state){
        return state.getBlock() instanceof BushBlock;
    }

    public boolean isCrop(BlockState state){
        return state.getBlock() instanceof CropBlock;
    }

    public boolean isStem(BlockState state){
          return state.getBlock() instanceof StemBlock || state.getBlock() instanceof StemGrownBlock || state.getBlock() instanceof AttachedStemBlock;
    }

    public boolean isCropDone(BlockState state){
        return state.getBlock() instanceof CropBlock cropBlock && cropBlock.getAge(state) == cropBlock.getMaxAge();
    }

    public boolean isAir(BlockState state){
        return state.isAir();
    }

    public void setSeedStack(ItemStack seedStack) {
        this.entityData.set(SEED_STACK, seedStack);
    }

    public ItemStack getSeedStack(){
        return entityData.get(SEED_STACK);
    }

    public void updateType() {
        if(this.getSeedStack().getItem() instanceof BlockItem blockItem){
            if(blockItem.getBlock() instanceof StemBlock){
                this.fieldType = FieldType.STEM;
                return;
            }
            else if(blockItem.getBlock() instanceof SweetBerryBushBlock){
                this.fieldType = FieldType.BUSH;
                return;
            }
        }

        this.fieldType = FieldType.CROP;
    }

    public enum FieldType {
        CROP(0),
        BUSH(1),
        STEM(2);

        private final int index;
        FieldType(int index){
            this.index = index;
        }
        public int getIndex(){
            return this.index;
        }

        public static FieldType fromIndex(int index) {
            for (FieldType messengerState : FieldType.values()) {
                if (messengerState.getIndex() == index) {
                    return messengerState;
                }
            }
            throw new IllegalArgumentException("Invalid State index: " + index);
        }
    }
}
