package com.talhanation.workers.world;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class BuildBlockParse {
    private final Item item;
    private final boolean wasParsed;

    public BuildBlockParse(Item item, boolean wasParsed) {
        this.item = item;
        this.wasParsed = wasParsed;
    }

    public Item getItem() {
        return item;
    }

    public boolean wasParsed() {
        return wasParsed;
    }

    public static BuildBlockParse parseBlock(Block block) {
        if (block == Blocks.GRASS_BLOCK
                || block == Blocks.MYCELIUM
                || block == Blocks.PODZOL
                || block == Blocks.ROOTED_DIRT
                || block == Blocks.FARMLAND) {
            return new BuildBlockParse(Item.BY_BLOCK.get(Blocks.DIRT), true);
        }
        else if (block instanceof FlowerPotBlock) {
            return new BuildBlockParse(Item.BY_BLOCK.get(Blocks.FLOWER_POT), true);
        }
        else if (block instanceof DoorBlock doorBlock) {
            if (doorBlock.defaultBlockState().hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                DoubleBlockHalf half = doorBlock.defaultBlockState().getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
                if (half == DoubleBlockHalf.UPPER) {
                    new BuildBlockParse(Item.BY_BLOCK.get(Blocks.AIR), false);
                }
            }
        }

        return new BuildBlockParse(Item.BY_BLOCK.get(block), false);
    }
}


