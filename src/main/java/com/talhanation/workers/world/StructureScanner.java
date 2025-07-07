package com.talhanation.workers.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

public class StructureScanner {
    public static CompoundTag scanStructure(Level level, BlockPos center, int size, int height) {
        CompoundTag root = new CompoundTag();
        ListTag blockList = new ListTag();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos start = center.offset(-size, 0, -size);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x <= size * 2; x++) {
                for (int z = 0; z <= size * 2; z++) {
                    pos.set(start.getX() + x, center.getY() + y, start.getZ() + z);
                    BlockState state = level.getBlockState(pos);

                    if (state.isAir() || state.getBlock() instanceof BushBlock) continue;

                    CompoundTag blockTag = new CompoundTag();

                    // Relative Position
                    blockTag.putInt("x", x);
                    blockTag.putInt("y", y);
                    blockTag.putInt("z", z);

                    // BlockState speichern
                    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    blockTag.putString("block", id.toString());
                    blockTag.put("state", NbtUtils.writeBlockState(state));

                    blockList.add(blockTag);
                }
            }
        }

        root.put("blocks", blockList);
        root.putInt("size", size);
        root.putInt("height", height);
        return root;
    }
}
