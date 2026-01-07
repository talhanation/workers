package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.client.gui.FishingAreaScreen;
import com.talhanation.workers.entities.FarmerEntity;
import com.talhanation.workers.entities.FishermanEntity;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class FishingArea extends AbstractWorkAreaEntity {

    public Map<BlockPos, Boolean> fishingSpots = new HashMap<>();

    public FishingArea(EntityType<?> type, Level level) {
        super(type, level);
    }
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);;
    }

    public Item getRenderItem(){
        return Items.FISHING_ROD;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Screen getScreen(Player player) {
        return new FishingAreaScreen(this, player);
    }
    public void scanBreakArea(){

    }

    public boolean isWorkerPerfectCandidate(FishermanEntity fisherman) {
        if (fisherman.getMatchingItem(stack -> stack.getItem() instanceof FishingRodItem) == ItemStack.EMPTY) {
            return false;
        }

        return true;
    }

    public boolean isAir(BlockState state){
        return state.isAir();
    }

}
