package com.talhanation.workers.entities;

import com.talhanation.workers.Main;
import com.talhanation.workers.config.WorkersModConfig;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.world.ForgeChunkManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class AbstractChunkLoaderEntity extends AbstractInventoryEntity {
    private Optional<WorkersChunk> loadedChunk = Optional.empty();

    public AbstractChunkLoaderEntity(EntityType<? extends AbstractChunkLoaderEntity> entityType, Level world) {
        super(entityType, world);
    }

    ///////////////////////////////////TICK/////////////////////////////////////////

    public void tick() {
        super.tick();
        updateChunkLoading();
    }

    public void updateChunkLoading(){
        if (WorkersModConfig.WorkerChunkLoading.get() && !this.getCommandSenderWorld().isClientSide) {
            WorkersChunk currentChunk = new WorkersChunk(this.chunkPosition().x, this.chunkPosition().z);
            if (loadedChunk.isEmpty()) {
                this.setForceChunk(currentChunk, true);
                loadedChunk = Optional.of(currentChunk);

            } else if (!currentChunk.equals(loadedChunk.get())){

                Set<WorkersChunk> toForce = getSetOfChunks(currentChunk);
                Set<WorkersChunk> toUnForce = getSetOfChunks(loadedChunk.get());
                toUnForce.removeAll(toForce);

                //verbliebene chunks
                Set<WorkersChunk> forced = getSetOfChunks(loadedChunk.get());
                toForce.removeAll(forced);


                toUnForce.forEach(chunk -> this.setForceChunk(chunk, false));
                toForce.forEach(chunk -> this.setForceChunk(chunk, true));
                loadedChunk = Optional.of(currentChunk);
            }
        }
    }

    ////////////////////////////////////DATA////////////////////////////////////

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if(loadedChunk.isPresent()) {
            nbt.putInt("chunkX", loadedChunk.get().x);
            nbt.putInt("chunkZ", loadedChunk.get().z);
        }
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("chunkX")) {
            int x = nbt.getInt("chunkX");
            int z = nbt.getInt("chunkZ");
            loadedChunk = Optional.of(new WorkersChunk(x, z));
        }
    }

    ////////////////////////////////////GET////////////////////////////////////

    private Set<WorkersChunk> getSetOfChunks(WorkersChunk chunk){
        Set<WorkersChunk> set = new HashSet<>();

        for(int i = -1; i <= 1; i++){
            for (int k = -1; k <= 1; k++){
                set.add(new WorkersChunk(chunk.x + i, chunk.z + k));
            }
        }
        return set;
    }

    ////////////////////////////////////SET////////////////////////////////////

    private void setForceChunk(WorkersChunk chunk, boolean add) {
        ForgeChunkManager.forceChunk((ServerLevel) this.getCommandSenderWorld(), Main.MOD_ID, this, chunk.x, chunk.z, add, false);
    }

    public void kill(){
        super.kill();
        if(!this.getCommandSenderWorld().isClientSide) loadedChunk.ifPresent(chunk -> this.getSetOfChunks(chunk).forEach(chunk1 -> this.setForceChunk(chunk1, false)));
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

        public WorkersChunk getNextChunk(Direction direction) {
            WorkersChunk workersChunk = null;
            switch (direction){
                case NORTH -> workersChunk = new WorkersChunk(this.x, this.z - 1);
                case EAST -> workersChunk = new WorkersChunk(this.x + 1, this.z);
                case SOUTH -> workersChunk = new WorkersChunk(this.x, this.z + 1);
                case WEST -> workersChunk = new WorkersChunk(this.x - 1, this.z);

                default -> {}
            }

            return workersChunk;
        }

        public static List<WorkersChunk> getSurroundingChunks(@NotNull WorkersChunk currentChunk){
            List<WorkersChunk> list = new ArrayList<>();

            for(int i = -1; i <= 1; i++){
                for(int k = -1; k <= 1; k++){
                    WorkersChunk newChunk = new WorkersChunk(currentChunk.x + i, currentChunk.z + k);
                    list.add(newChunk);
                }
            }
            return list;
        }

        public static List<WorkersChunk> getChunksToUnload(List<WorkersChunk> currentChunks, List<WorkersChunk> prevChunks) {
            List<WorkersChunk> list = new ArrayList<>();

            for(WorkersChunk prevChunk : prevChunks){
                for(WorkersChunk currentChunk : currentChunks) {
                    if(!currentChunk.isSame(prevChunk)) list.add(prevChunk);
                }
            }
            return list;
        }
    }
}
