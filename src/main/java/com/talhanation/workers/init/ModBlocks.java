package com.talhanation.workers.init;

import com.talhanation.workers.Main;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Main.MOD_ID);

    public static final RegistryObject<Block> MINER_BLOCK = BLOCKS.register("miner_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final RegistryObject<Block> LUMBERJACK_BLOCK = BLOCKS.register("lumberjack_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final RegistryObject<Block> FISHER_BLOCK = BLOCKS.register("fisherman_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final RegistryObject<Block> FARMER_BLOCK = BLOCKS.register("farmer_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final RegistryObject<Block> MERCHANT_BLOCK = BLOCKS.register("merchant_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final RegistryObject<Block> SHEPHERD_BLOCK = BLOCKS.register("shepherd_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final RegistryObject<Block> CATTLE_FARMER_BLOCK = BLOCKS.register("cattle_farmer_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final RegistryObject<Block> CHICKEN_FARMER_BLOCK = BLOCKS.register("chicken_farmer_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    public static final RegistryObject<Block> SWINEHERD_BLOCK = BLOCKS.register("swineherd_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));
}
