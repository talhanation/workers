package com.talhanation.workers.entities;

import com.mojang.datafixers.util.Pair;
import com.talhanation.workers.Main;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.world.ForgeChunkManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public abstract class AbstractChunkLoaderEntity extends AbstractInventoryEntity {


    public AbstractChunkLoaderEntity(EntityType<? extends AbstractChunkLoaderEntity> entityType, Level world) {
        super(entityType, world);
    }
		
	public WorkersChunk currentChunk;
	public WorkersChunk prevChunk;
	
	protected abstract boolean shouldLoadChunk();
	
    ///////////////////////////////////TICK/////////////////////////////////////////

    public void tick() {
        super.tick();
        updateChunkLoading();
    }

    public void updateChunkLoading() {
        if (this.shouldLoadChunk() && !this.level.isClientSide) {
            this.currentChunk = new WorkersChunk(this.chunkPosition());		
			
            if (currentChunk != null && currentChunk.isSame(prevChunk)) {
                this.forceChunk(currentChunk);
                
				if(prevChunk != null){
					//isLoaded maybe
					this.unForceChunk(prevChunk);
				}
				prevChunk = currentChunk;
            } 
        }
    }

    ////////////////////////////////////DATA////////////////////////////////////
	/*
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if(currentChunk != null) {
            nbt.putInt("currentChunkX", currentChunk.x);
            nbt.putInt("currentChunkZ", currentChunk.z);
        }
    }

    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("currentChunkX")) {
            int x = nbt.getInt("currentChunkX");
            int z = nbt.getInt("currentChunkZ");
            currentChunk = new WorkersChunk(x, z);
        }
    }
	*/

    ////////////////////////////////////GET////////////////////////////////////

    private List<WorkersChunk> getListOfChunks(WorkersChunk currentChunk){
        List<WorkersChunk> list = new List<>();

        for(int i = -1; i <= 1; i++){
            for (int k = -1; k <= 1; k++){
                list.add(new WorkersChunk(currentChunk.x + i, currentChunk.z + k));
            }
        }
        return list;
    }

    ////////////////////////////////////SET////////////////////////////////////

    private void forceChunk(WorkersChunk chunk) {
        ForgeChunkManager.forceChunk((ServerLevel) this.level, Main.MOD_ID, this.getUUID(), chunk.x, chunk.z, true, true);
    }

    private void unForceChunk(WorkersChunk chunk) {
        ForgeChunkManager.forceChunk((ServerLevel) this.level, Main.MOD_ID, this.getUUID(), chunk.x, chunk.z, false, true);
    }

    public void die(@NotNull DamageSource dmg) {
        super.die(dmg);
        this.unForceChunk(currentChunk);
		this.unForceChunk(prevChunk);
    }
	
	
	
	public class WorkersChunk{
		int x
		int z
		
		public WorkersChunk(ChunkPosition chunkPosition){
			this(chunkPosition.x, chunkPosition.z);
		}
		
		public WorkersChunk(int x, int z){
			this.x = x;
			this.z = z;
		}
		
		public boolean same(WorkersChunk otherChunk){
			if(otherChunk != null) return this.x == otherChunk.x && this.z == otherChunk.z;
			else return false;
		}
	}
}
