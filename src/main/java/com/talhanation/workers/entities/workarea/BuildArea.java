package com.talhanation.workers.entities.workarea;

import com.talhanation.workers.Main;
import com.talhanation.workers.client.gui.BuildAreaScreen;
import com.talhanation.workers.network.MessageToClientOpenWorkAreaScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
    public static final EntityDataAccessor<CompoundTag> STRUCTURE = SynchedEntityData.defineId(BuildArea.class, EntityDataSerializers.COMPOUND_TAG);
    public Stack<BlockPos> stackToBreak = new Stack<>();

    public BuildArea(EntityType<?> type, Level level) {
        super(type, level);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STRUCTURE, new CompoundTag());
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

    public void setStructureNBT(CompoundTag tag){
        this.entityData.set(STRUCTURE, tag);
    }

    public CompoundTag getStructureNBT(){
        return this.entityData.get(STRUCTURE);
    }

    public void setStartBuild() {
        CompoundTag tag = getStructureNBT();
        if (tag == null || !tag.contains("blocks", Tag.TAG_LIST)) return;

        ListTag blockList = tag.getList("blocks", Tag.TAG_COMPOUND);
        BlockPos origin = this.getOnPos(); // dein Zentrum, z. B. BuildArea.getCenter()

        for (Tag t : blockList) {
            CompoundTag blockTag = (CompoundTag) t;

            // Relative Position
            int relX = blockTag.getInt("x");
            int relY = blockTag.getInt("y");
            int relZ = blockTag.getInt("z");

            BlockPos relPos = new BlockPos(relX, relY, relZ);
            BlockPos worldPos = origin.offset(relPos); // Weltposition berechnen

            // BlockState lesen
            CompoundTag stateTag = blockTag.getCompound("state");
            BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), stateTag);

            // Block setzen
            this.getCommandSenderWorld().setBlock(worldPos, state, 3);
        }
    }


    public void scanBreakArea(){
        stackToBreak.clear();
        Level level = this.getCommandSenderWorld();

        Fluid centerPosFluid = level.getFluidState(this.getOnPos()).getType();
        if(!(centerPosFluid == Fluids.WATER)|| (centerPosFluid == Fluids.FLOWING_WATER)) {
            this.stackToBreak.push(this.getOnPos());
        }

        for (int i = 0; i <= getXSize(); i++) {
            for (int k = 0; k <= getYSize(); k++) {
                for (int j = 0; j <= getZSize(); j++) {
                    BlockPos pos = getOnPos().offset(i, k, j);
                    BlockState state = level.getBlockState(pos);

                    if(!state.isAir()){
                        this.stackToBreak.push(pos);
                    }
                }
            }
        }
    }

    public void setXSize(int x) {
    }
    public void setYSize(int y) {
    }
    public void setZSize(int z) {
    }
}
