package com.talhanation.workers.entities;

import com.mojang.datafixers.util.Pair;
import com.talhanation.workers.Main;
import net.minecraft.world.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public abstract class AbstractChunkLoaderEntity extends AbstractInventoryEntity{

    private Optional<Pair<Integer, Integer>> loadedChunk = Optional.empty();

    public AbstractChunkLoaderEntity(EntityType<? extends AbstractChunkLoaderEntity> entityType, Level world) {
        super(entityType, world);
    }

    ///////////////////////////////////TICK/////////////////////////////////////////

    public void tick() {
        super.tick();
        updateChunkLoading();
    }

    public void updateChunkLoading(){
        if (this.shouldLoadChunk() && !this.level.isClientSide) {
            Pair<Integer, Integer> currentChunk = new Pair<>(this.chunkPosition().x, this.chunkPosition().z);
            if (!loadedChunk.isPresent()) {
                this.forceChunk(currentChunk);
                loadedChunk = Optional.of(currentChunk);

            } else if (!currentChunk.equals(loadedChunk.get())){

                Set<Pair<Integer, Integer>> toForce = getSetOfChunks(currentChunk);
                Set<Pair<Integer, Integer>> toUnForce = getSetOfChunks(loadedChunk.get());
                toUnForce.removeAll(toForce);

                //verbliebene chunks
                Set<Pair<Integer, Integer>> forced = getSetOfChunks(loadedChunk.get());
                toForce.removeAll(forced);


                toUnForce.forEach(this::unForceChunk);
                toForce.forEach(this::forceChunk);
                loadedChunk = Optional.of(currentChunk);
            }
        }
    }

    ////////////////////////////////////DATA////////////////////////////////////

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if(loadedChunk.isPresent()) {
            nbt.putInt("chunkX", loadedChunk.get().getFirst());
            nbt.putInt("chunkZ", loadedChunk.get().getSecond());
        }
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("chunkX")) {
            int x = nbt.getInt("chunkX");
            int z = nbt.getInt("chunkZ");
            loadedChunk = Optional.of(new Pair<>(x, z));
        }
    }

    ////////////////////////////////////GET////////////////////////////////////

    private Set<Pair<Integer, Integer>> getSetOfChunks(Pair<Integer, Integer> chunk){
        Set<Pair<Integer, Integer>> set = new HashSet<>();

        for(int i = -1; i <= 1; i++){
            for (int k = -1; k <= 1; k++){
                set.add(new Pair<>(chunk.getFirst() + i, chunk.getSecond() + k));
            }
        }
        return set;
    }

    ////////////////////////////////////SET////////////////////////////////////

    private void forceChunk(Pair<Integer, Integer> chunk) {
        ForgeChunkManager.forceChunk((ServerLevel) this.level, Main.MOD_ID, this.getUUID(), chunk.getFirst(), chunk.getSecond(), true, true);
    }

    private void unForceChunk(Pair<Integer, Integer> chunk) {
        ForgeChunkManager.forceChunk((ServerLevel) this.level, Main.MOD_ID, this.getUUID(), chunk.getFirst(), chunk.getSecond(), false, true);
    }

    protected abstract boolean shouldLoadChunk();

    public void remove(){
        super.remove(RemovalReason.DISCARDED);
        if(!this.level.isClientSide) loadedChunk.ifPresent(chunk -> this.getSetOfChunks(chunk).forEach(this::unForceChunk));
    }
}
