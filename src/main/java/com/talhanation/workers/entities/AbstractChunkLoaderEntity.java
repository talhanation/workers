package com.talhanation.workers.entities;

import com.talhanation.workers.Main;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.world.ForgeChunkManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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
        //if(currentChunk != null)Main.LOGGER.info("currentChunk: ["+ currentChunk.x + ";" + currentChunk.z + "] ");
        //if(prevChunk != null)Main.LOGGER.info("prevChunk: ["+ prevChunk.x + ";" + prevChunk.z + "] ");
    }

    public void updateChunkLoading() {
        if (this.shouldLoadChunk() && !this.level.isClientSide) {
            this.currentChunk = new WorkersChunk(this.chunkPosition());
            this.forceChunk(currentChunk);

            if (!currentChunk.isSame(prevChunk)) {


                if(prevChunk != null) this.unForceChunk(prevChunk);
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
        List<WorkersChunk> list = new ArrayList<>();

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
        ForgeChunkManager.forceChunk((ServerLevel) this.level, Main.MOD_ID, this.getUUID(), chunk.x, chunk.z, false, false);
    }

    public void die(@NotNull DamageSource dmg) {
        super.die(dmg);
        this.unForceChunk(currentChunk);
		this.unForceChunk(prevChunk);
    }
	
	
	
	public static class WorkersChunk{
		int x;
		int z;
		
		public WorkersChunk(ChunkPos chunkPosition){
			this(chunkPosition.x, chunkPosition.z);
		}
		
		public WorkersChunk(int x, int z){
			this.x = x;
			this.z = z;
		}
		
		public boolean isSame(WorkersChunk otherChunk){
			if(otherChunk != null) return this.x == otherChunk.x && this.z == otherChunk.z;
			else return false;
		}
	}
}
