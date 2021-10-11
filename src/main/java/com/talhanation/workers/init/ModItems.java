package com.talhanation.workers.init;

import com.google.common.collect.Lists;
import com.talhanation.workers.Main;
import com.talhanation.workers.init.ModEntityTypes;
import com.talhanation.workers.util.RegistryUtils;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MOD_ID);
    public static final List<RegistryObject<Item>> SPAWN_EGGS = Lists.newArrayList();

    public static final RegistryObject<Item> MINER_SPAWN_EGG = RegistryUtils.createSpawnEggItem("miner", ModEntityTypes.MINER::get, 16755200, 16777045);
    public static final RegistryObject<Item> LUMBER_SPAWN_EGG = RegistryUtils.createSpawnEggItem("lumberjack", ModEntityTypes.LUMBERJACK::get, 16755200, 16777045);
    public static final RegistryObject<Item> SHEPHERD_SPAWN_EGG = RegistryUtils.createSpawnEggItem("shepherd", ModEntityTypes.SHEPHERD::get, 16755200, 16777045);

    public static final RegistryObject<BlockItem> RECRUIT_BLOCK = ITEMS.register("miner_block", () -> new BlockItem(ModBlocks.MINER_BLOCK.get(), (new Item.Properties()).tab(ItemGroup.TAB_DECORATIONS)));
    public static final RegistryObject<BlockItem> BOWMAN_BLOCK = ITEMS.register("lumberjack_block", () -> new BlockItem(ModBlocks.LUMBERJACK_BLOCK.get(), (new Item.Properties()).tab(ItemGroup.TAB_DECORATIONS)));

}