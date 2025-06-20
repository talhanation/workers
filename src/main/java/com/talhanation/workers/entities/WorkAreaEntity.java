package com.talhanation.workers.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.Team;

import java.util.Stack;
import java.util.UUID;

public class WorkAreaEntity extends Entity {
    public int radius = 4;
    public int height = 2;
    public ItemStack seedStack = Items.WHEAT_SEEDS.getDefaultInstance();
    public String name;
    public Stack<BlockPos> stackToPlant = new Stack<>();
    public Stack<BlockPos> stackToBreak = new Stack<>();
    public Stack<BlockPos> stackToPlow = new Stack<>();
    public boolean isDone;
    public boolean isBeingWorkedOn;
    public UUID workerUUID;
    public UUID playerUUID;
    public String playerName;
    public Team team;

    public WorkAreaEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        this.playerUUID = tag.getUUID("playerUUID");
        this.isDone = tag.getBoolean("isDone");
        this.resetTimer = tag.getInt("resetTimer");
        this.isBeingWorkedOn = tag.getBoolean("isBeingWorkedOn");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putUUID("playerUUID", playerUUID);
        tag.putBoolean("isDone", isDone);
        tag.putInt("resetTimer", resetTimer);
        tag.putBoolean("isBeingWorkedOn", isBeingWorkedOn);
    }

    int resetTimer;
    @Override
    public void tick() {
        super.tick();
        if(isDone && resetTimer++ > 20*60*3){
            resetTimer = 0;
            this.setDone(false);
        }
    }

    public void scanBreakArea(){
        stackToBreak.clear();
        Level level = this.getCommandSenderWorld();
        for (int i = -radius; i <= radius; i++) {
            for (int k = -height; k <= height; k++) {
                for (int j = -radius; j <= radius; j++) {
                    BlockPos pos = getOnPos().offset(i, k, j);
                    BlockState state = level.getBlockState(pos);

                    BlockPos below = pos.below();
                    BlockState stateBelow = level.getBlockState(below);

                    if(isFarmland(stateBelow) || isTillAble(stateBelow)){
                        if(isCropDone(state) || (isBush(state) && !isCrop(state))){
                            this.stackToBreak.push(pos);
                        }
                    }
                }
            }
        }
    }
    public void scanPlowArea(){
        stackToPlow.clear();
        Level level = this.getCommandSenderWorld();
        for (int i = -radius; i <= radius; i++) {
            for (int k = -height; k <= height; k++) {
                for (int j = -radius; j <= radius; j++) {
                    BlockPos pos = getOnPos().offset(i, k, j);
                    BlockState state = level.getBlockState(pos);

                    BlockPos above = pos.above();
                    BlockState stateAbove = level.getBlockState(above);

                    if(isAir(stateAbove) && isTillAble(state)){
                        this.stackToPlow.push(pos);
                    }
                }
            }
        }
    }
    public void scanPlantArea(){
        stackToPlant.clear();
        Level level = this.getCommandSenderWorld();
        for (int i = -radius; i <= radius; i++) {
            for (int k = -height; k <= height; k++) {
                for (int j = -radius; j <= radius; j++) {
                    BlockPos pos = getOnPos().offset(i, k, j);
                    BlockState state = level.getBlockState(pos);

                    BlockPos below = pos.below();
                    BlockState stateBelow = level.getBlockState(below);

                    if(isAir(state) && isFarmland(stateBelow)){
                        this.stackToPlant.push(pos);
                    }
                }
            }
        }
    }

    public void scanArea() {
        this.setBeingWorkedOn(true);
        stackToPlant.clear();
        stackToBreak.clear();
        stackToPlow.clear();
        Level level = this.getCommandSenderWorld();
        for (int i = -radius; i <= radius; i++) {
            for (int k = -height; k <= height; k++) {
                for (int j = -radius; j <= radius; j++) {
                    BlockPos pos = getOnPos().offset(i, k, j);
                    BlockState state = level.getBlockState(pos);

                    BlockPos above = pos.above();
                    BlockState stateAbove = level.getBlockState(above);

                    boolean canBeTilled = FarmerEntity.TILLABLES.contains(state.getBlock());
                    boolean hasSpaceAbove = stateAbove.isAir() || stateAbove.getBlock() instanceof BushBlock;

                    if(state.getBlock() instanceof FarmBlock){
                        if(hasSpaceAbove){//what?
                            this.stackToPlant.push(pos.above());
                        }
                    }
                    else if (canBeTilled && hasSpaceAbove){
                        this.stackToPlow.push(pos);
                    }
                    else if(state.getBlock() instanceof CropBlock cropBlock){
                        int currentAge = cropBlock.getAge(state);
                        int maxAge = cropBlock.getMaxAge();

                        if(currentAge == maxAge){
                            this.stackToBreak.push(pos);
                        }
                    }
                    else if(state.getBlock() instanceof BushBlock){
                        this.stackToBreak.push(pos);
                    }
                }
            }
        }
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

    public boolean isCropDone(BlockState state){
        return state.getBlock() instanceof CropBlock cropBlock && cropBlock.getAge(state) == cropBlock.getMaxAge();
    }

    public boolean isAir(BlockState state){
        return state.isAir();
    }
    @Override
    public boolean hurt(DamageSource damageSource, float a) {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity entity) {
        return false;
    }

    @Override
    protected boolean canRide(Entity entity) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean canRiderInteract() {
        return false;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean isEffectiveAi() {
        return false;
    }

    public Item getRenderItem(){
        return Items.IRON_HOE;
    }

    public boolean canWorkHere(FarmerEntity farmer) {
        return farmer.isOwned() && farmer.getOwnerUUID().equals(this.playerUUID);
    }

    public boolean hasWorkOpen(){
        return !stackToBreak.isEmpty() || !stackToPlant.isEmpty() || !stackToPlow.isEmpty();
    }

    public void setDone(boolean b) {
        this.isDone = b;
    }

    public boolean isDone(){
        return this.isDone;
    }

    public void setBeingWorkedOn(boolean b) {
        this.isBeingWorkedOn = b;
    }

    public boolean isBeingWorkedOn(){
        return this.isBeingWorkedOn;
    }
}
