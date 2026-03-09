package com.talhanation.workers.world;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;

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

        return new BuildBlockParse(Item.BY_BLOCK.get(block), false);
    }
}


