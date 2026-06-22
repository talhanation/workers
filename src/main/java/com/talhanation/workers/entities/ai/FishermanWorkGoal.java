package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.FishermanEntity;
import com.talhanation.workers.entities.FishingBobberEntity;
import com.talhanation.workers.entities.workarea.FishingArea;
import com.talhanation.workers.world.NeededItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

public class FishermanWorkGoal extends Goal {

    public FishermanEntity fisherman;
    public FishingBobberEntity fishingBobber;
    public State state;
    public String errorMessage;
    public boolean errorMessageDone;
    public BlockPos blockPos;
    private int catchTime = 0;
    private int throwTime;
    public FishermanWorkGoal(FishermanEntity fisherman) {
        this.fisherman = fisherman;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return !fisherman.needsToSleep() && fisherman.shouldWork() && !fisherman.needsToGetToChest() && this.isAreaNotRemoved();
    }

    private boolean isAreaNotRemoved() {
        if(fisherman.currentFishingArea == null || !fisherman.currentFishingArea.isRemoved()) return true;
        else {
            fisherman.currentFishingArea = null;
        }
        return false;
    }

    @Override
    public void start() {
        super.start();
        if(fisherman.getFollowState() == 0) fisherman.setFollowState(6); //Working
        setState(State.SELECT_WORK_AREA);
    }

    @Override
    public void stop() {
        super.stop();
        if(fishingBobber != null && fishingBobber.isAlive()){
            this.fisherman.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 1, 1);
            this.fishingBobber.discard();
            this.fishingBobber = null;
        }
    }

    int cooldown;
    @Override
    public void tick() {
        super.tick();
        if(this.fisherman.getCommandSenderWorld().isClientSide()) return;
        if(state == null) return;
        if(blockPos != null) this.fisherman.getLookControl().setLookAt(blockPos.getCenter());
        if(fisherman.tickCount % 5 != 0) return;

        if(!isAreaNotRemoved()) return;

        if(state != State.SELECT_WORK_AREA && this.fisherman.currentFishingArea == null){
            setState(State.SELECT_WORK_AREA);
            return;
        }

        switch(state){
            case SELECT_WORK_AREA -> {
                if(this.fisherman.currentFishingArea != null) setState(State.MOVE_TO_WORK_AREA);

                if(++cooldown < fisherman.getRandom().nextInt(300)) return;
                this.cooldown = 0;

                List<FishingArea> areas = getAvailableWorkAreasByPriority((ServerLevel) fisherman.getCommandSenderWorld(), fisherman, this.fisherman.currentFishingArea);

                if (!areas.isEmpty()) {
                    this.fisherman.currentFishingArea = areas.get(0);
                }

                if(this.fisherman.currentFishingArea == null) return;

                this.fisherman.currentFishingArea.setBeingWorkedOn(true);
                this.fisherman.currentFishingArea.setTime(0);

                setState(State.MOVE_TO_WORK_AREA);
            }

            case MOVE_TO_WORK_AREA -> {
                this.blockPos = null;
                if(this.moveToPosition(this.fisherman.currentFishingArea.getOnPos(), 20)) return;

                setState(State.PREPARE_FISHING);
            }

            case PREPARE_FISHING -> {
                if(++cooldown < 20) return;
                this.cooldown = 0;

                if(!fisherman.hasFreeInvSlot()){
                    fisherman.forcedDeposit = true;
                    return;
                }

                boolean hasFishingRod = fisherman.getInventory().hasAnyMatching(itemStack -> itemStack.getItem() instanceof FishingRodItem);
                if(!hasFishingRod){
                    fisherman.addNeededItem(new NeededItem(stack -> stack.getItem() instanceof FishingRodItem, 1, true));
                    return;
                }
                else {
                    fisherman.switchMainHandItem(itemStack -> itemStack.getItem() instanceof FishingRodItem);
                }

                this.fisherman.swing(InteractionHand.MAIN_HAND);
                this.fisherman.playSound(SoundEvents.FISHING_BOBBER_THROW, 1, 1);

                Vec3 center = fisherman.currentFishingArea.getArea().getCenter();

                this.fishingBobber = fisherman.throwFishingHook(center);

                blockPos = BlockPos.containing(center);

                catchTime = 130;

                setState(State.FISHING);
            }

            case FISHING -> {
                if(fishingBobber != null){
                    if(fishingBobber.hooked){
                        setState(State.CATCH);
                        return;
                    }
                    else if(fishingBobber.onGround()){
                        this.fisherman.swing(InteractionHand.MAIN_HAND);
                        this.fisherman.playSound(SoundEvents.FISHING_BOBBER_RETRIEVE, 1, 1);
                        this.fishingBobber.discard();
                        this.fishingBobber = null;

                        setState(State.MOVE_TO_WORK_AREA);
                        return;
                    }
                }

                if(++throwTime > catchTime){
                    throwTime = 0;
                    this.fisherman.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 1, 1);
                    setState(State.CATCH);
                }
            }

            case CATCH -> {
                this.fisherman.swing(InteractionHand.MAIN_HAND);
                this.fisherman.playSound(SoundEvents.FISHING_BOBBER_RETRIEVE, 1, 1);

                this.spawnFishingLoot();

                if(this.fishingBobber != null){
                    this.fishingBobber.discard();
                    this.fishingBobber = null;
                }
                this.fisherman.farmedItems++;
                if(this.fisherman.tickCount % 2 == 0) this.fisherman.damageMainHandItem();

                setState(State.PREPARE_FISHING);
            }

            case DONE -> {
                this.fisherman.currentFishingArea.setBeingWorkedOn(false);
                if(this.fisherman.getFollowState() == 6) this.fisherman.setFollowState(0);//Wander
                blockPos = null;
                this.fisherman.currentFishingArea = null;
            }

            case ERROR -> {
                if(!errorMessageDone){
                    errorMessageDone = true;
                }
            }
        }
    }

    public void setState(State state) {
        //if(fisherman.getOwner() != null) fisherman.getOwner().sendSystemMessage(Component.literal(state.toString()));
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

    public static List<FishingArea> getAvailableWorkAreasByPriority(ServerLevel level, FishermanEntity fisherman, @Nullable FishingArea currentArea) {
        List<FishingArea> list = level.getEntitiesOfClass(FishingArea.class, fisherman.getBoundingBox().inflate(64));


        return list;
    }

    public boolean moveToPosition(BlockPos pos, int threshold){
        if(pos == null){
            return false;
        }
        else{
            double distance = fisherman.getHorizontalDistanceTo(pos.getCenter());
            if(distance < threshold){
                fisherman.getNavigation().stop();
                return false;
            }
            else{
                fisherman.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.8F);
                // start() already claimed the working state; calling it here every
                // tick would override owner commands and pull the worker back to work.
                fisherman.getLookControl().setLookAt(pos.getCenter());
            }
            return true;
        }
    }

    public void spawnFishingLoot() {
        if(fishingBobber == null ) return;

        double luckFromTool = EnchantmentHelper.getFishingLuckBonus(this.fisherman.getItemInHand(InteractionHand.MAIN_HAND));
        double luckFromDepth = Math.min(25, fishingBobber.getWaterDepth())/10F;
        double luck = 0.1D + luckFromTool + luckFromDepth;

        LootParams lootparams = (new LootParams.Builder((ServerLevel)this.fisherman.getCommandSenderWorld()))
                .withParameter(LootContextParams.ORIGIN, this.fisherman.position())
                .withParameter(LootContextParams.TOOL, fisherman.getMainHandItem())
                .withParameter(LootContextParams.KILLER_ENTITY, this.fisherman)
                .withLuck((float)(luck + luckFromTool))
                .create(LootContextParamSets.FISHING);
        LootTable loottable = this.fisherman.getCommandSenderWorld().getServer().getLootData().getLootTable(BuiltInLootTables.FISHING);
        List<ItemStack> list = loottable.getRandomItems(lootparams);

        MinecraftServer server = fisherman.getServer();
        if (server == null) return;

        for (ItemStack itemstack : list) {
            fisherman.getInventory().addItem(itemstack);
        }
    }

    public enum State{
        SELECT_WORK_AREA,
        MOVE_TO_WORK_AREA,
        PREPARE_FISHING,
        FISHING,
        CATCH,
        DONE,
        ERROR

    }
}
