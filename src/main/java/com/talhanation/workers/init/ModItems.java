package com.talhanation.workers.init;

import com.google.common.collect.Lists;
import com.talhanation.workers.Main;
import com.talhanation.workers.util.RegistryUtils;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MOD_ID);
    public static final List<RegistryObject<Item>> SPAWN_EGGS = Lists.newArrayList();

    public static final RegistryObject<Item> MINER_SPAWN_EGG = RegistryUtils.createSpawnEggItem("miner", ModEntityTypes.MINER::get, 16755200, 16777045);
    public static final RegistryObject<Item> LUMBER_SPAWN_EGG = RegistryUtils.createSpawnEggItem("lumberjack", ModEntityTypes.LUMBERJACK::get, 16755200, 16777045);
    public static final RegistryObject<Item> FISHERMAN_SPAWN_EGG = RegistryUtils.createSpawnEggItem("fisherman", ModEntityTypes.FISHERMAN::get, 16755201, 16777044);
    public static final RegistryObject<Item> SHEPHERD_SPAWN_EGG = RegistryUtils.createSpawnEggItem("shepherd", ModEntityTypes.SHEPHERD::get, 16755200, 16777045);
    public static final RegistryObject<Item> FARMER_SPAWN_EGG = RegistryUtils.createSpawnEggItem("farmer", ModEntityTypes.FARMER::get, 16755200, 16777045);
    public static final RegistryObject<Item> MERCHANT_SPAWN_EGG = RegistryUtils.createSpawnEggItem("merchant", ModEntityTypes.MERCHANT::get, 16755200, 16777045);

    public static final RegistryObject<BlockItem> miner_block = ITEMS.register("miner_block", () -> new BlockItem(ModBlocks.MINER_BLOCK.get(), (new Item.Properties()).tab(CreativeModeTab.TAB_DECORATIONS)));
    public static final RegistryObject<BlockItem> lumberjack_block = ITEMS.register("lumberjack_block", () -> new BlockItem(ModBlocks.LUMBERJACK_BLOCK.get(), (new Item.Properties()).tab(CreativeModeTab.TAB_DECORATIONS)));
    public static final RegistryObject<BlockItem> fisherman_block = ITEMS.register("fisherman_block", () -> new BlockItem(ModBlocks.FISHER_BLOCK.get(), (new Item.Properties()).tab(CreativeModeTab.TAB_DECORATIONS)));

    public static final RegistryObject<BlockItem> merchant_block = ITEMS.register("merchant_block", () -> new BlockItem(ModBlocks.MERCHANT_BLOCK.get(), (new Item.Properties()).tab(CreativeModeTab.TAB_DECORATIONS)));
    public static final RegistryObject<BlockItem> farmer_block = ITEMS.register("farmer_block", () -> new BlockItem(ModBlocks.FARMER_BLOCK.get(), (new Item.Properties()).tab(CreativeModeTab.TAB_DECORATIONS)));
    public static final RegistryObject<BlockItem> shepherd_block = ITEMS.register("shepherd_block", () -> new BlockItem(ModBlocks.SHEPHERD_BLOCK.get(), (new Item.Properties()).tab(CreativeModeTab.TAB_DECORATIONS)));

}