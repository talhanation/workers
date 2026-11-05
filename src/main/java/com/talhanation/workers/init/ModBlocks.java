package com.talhanation.workers.init;

import com.talhanation.workers.WorkersMain;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, WorkersMain.MOD_ID);

    /*
    public static final DeferredHolder<Block, Block> MINER_BLOCK = BLOCKS.register("miner_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final DeferredHolder<Block, Block> LUMBERJACK_BLOCK = BLOCKS.register("lumberjack_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final DeferredHolder<Block, Block> FISHER_BLOCK = BLOCKS.register("fisherman_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final DeferredHolder<Block, Block> FARMER_BLOCK = BLOCKS.register("farmer_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final DeferredHolder<Block, Block> MERCHANT_BLOCK = BLOCKS.register("merchant_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final DeferredHolder<Block, Block> SHEPHERD_BLOCK = BLOCKS.register("shepherd_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final DeferredHolder<Block, Block> CATTLE_FARMER_BLOCK = BLOCKS.register("cattle_farmer_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final DeferredHolder<Block, Block> CHICKEN_FARMER_BLOCK = BLOCKS.register("chicken_farmer_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final DeferredHolder<Block, Block> SWINEHERD_BLOCK = BLOCKS.register("swineherd_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

     */
}
