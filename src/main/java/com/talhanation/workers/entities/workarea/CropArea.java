package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.Main;
import com.talhanation.workers.client.gui.CropAreaScreen;
import com.talhanation.workers.entities.FarmerEntity;
import com.talhanation.workers.network.MessageToClientOpenWorkAreaScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.network.PacketDistributor;

import java.util.Stack;

public class CropArea extends AbstractWorkAreaEntity {

    public static final EntityDataAccessor<ItemStack> SEED_STACK = SynchedEntityData.defineId(CropArea.class, EntityDataSerializers.ITEM_STACK);

    public Stack<BlockPos> stackToPlant = new Stack<>();
    public Stack<BlockPos> stackToBreak = new Stack<>();
    public Stack<BlockPos> stackToPlow = new Stack<>();

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

    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag nbt = new CompoundTag();
        this.getSeedStack().save(nbt);
        tag.put("seedItem", nbt);
    }

    public Item getRenderItem(){
        return Items.IRON_HOE;
    }

    @Override
    public Screen getScreen(Player player) {
        return new CropAreaScreen(this, player);
    }

    public InteractionResult interact(Player player, InteractionHand hand) {
        if(!player.getUUID().equals(this.getPlayerUUID())) return InteractionResult.PASS;

        if (this.getCommandSenderWorld().isClientSide()) {
            return InteractionResult.CONSUME;
        }
        else{
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToClientOpenWorkAreaScreen(this.getUUID()));
            return InteractionResult.CONSUME;
        }
    }

    public void scanBreakArea(){
        stackToBreak.clear();
        Level level = this.getCommandSenderWorld();

        Fluid centerPosFluid = level.getFluidState(this.getOnPos()).getType();
        if(!(centerPosFluid == Fluids.WATER)|| (centerPosFluid == Fluids.FLOWING_WATER)) {
            this.stackToBreak.push(this.getOnPos());
        }

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockState state = level.getBlockState(pos);

            BlockPos below = pos.below();
            BlockState stateBelow = level.getBlockState(below);

            if(isFarmland(stateBelow) || isTillAble(stateBelow)){
                if(isCropDone(state) || (isBush(state) && !isCrop(state))){
                    this.stackToBreak.push(pos);
                }
            }
        });
    }
    public void scanPlowArea(){
        stackToPlow.clear();
        Level level = this.getCommandSenderWorld();

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockState state = this.getCommandSenderWorld().getBlockState(pos);

            BlockPos above = pos.above();
            BlockState stateAbove = level.getBlockState(above);

            if(isAir(stateAbove) && isTillAble(state)){
                this.stackToPlow.push(pos);
            }
        });
    }
    public void scanPlantArea(){
        stackToPlant.clear();
        Level level = this.getCommandSenderWorld();

        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockState state = level.getBlockState(pos);

            BlockPos below = pos.below();
            BlockState stateBelow = level.getBlockState(below);

            if(isAir(state) && isFarmland(stateBelow)){
                this.stackToPlant.push(pos);
            }
        });
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
}
