package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.Main;
import com.talhanation.workers.client.gui.BuildAreaScreen;
import com.talhanation.workers.network.MessageToClientOpenWorkAreaScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.network.PacketDistributor;

import java.util.Stack;

public class BuildArea extends AbstractWorkAreaEntity {

    public Stack<BlockPos> stackToBreak = new Stack<>();

    public BuildArea(EntityType<?> type, Level level) {
        super(type, level);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
    }
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public Item getRenderItem() {
        return Items.IRON_SHOVEL;
    }

    @Override
    public Screen getScreen(Player player) {
        return new BuildAreaScreen(this, player);
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
    @Override
    public void tick() {
        super.tick();
        if(isDone()) this.remove(RemovalReason.DISCARDED);
    }

    public void scanBreakArea(){
        stackToBreak.clear();
        Level level = this.getCommandSenderWorld();

        Fluid centerPosFluid = level.getFluidState(this.getOnPos()).getType();
        if(!(centerPosFluid == Fluids.WATER)|| (centerPosFluid == Fluids.FLOWING_WATER)) {
            this.stackToBreak.push(this.getOnPos());
        }

        for (int i = -getSize(); i <= getSize(); i++) {
            for (int k = -getHeight(); k <= getHeight(); k++) {
                for (int j = -getSize(); j <= getSize(); j++) {
                    BlockPos pos = getOnPos().offset(i, k, j);
                    BlockState state = level.getBlockState(pos);

                    if(!state.isAir()){
                        this.stackToBreak.push(pos);
                    }
                }
            }
        }
    }
}
