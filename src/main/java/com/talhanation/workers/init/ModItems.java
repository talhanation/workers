package com.talhanation.workers.init;

import com.talhanation.recruits.util.RegistryUtils;
import com.talhanation.workers.Main;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MOD_ID);
    public static final RegistryObject<SpawnEggItem> FARMER_SPAWN_EGG = RegistryUtils.createSpawnEggItem("farmer", ModEntityTypes.FARMER, 16755200, 16777045);
    public static final RegistryObject<SpawnEggItem> LUMBERJACK_SPAWN_EGG = RegistryUtils.createSpawnEggItem("lumberjack", ModEntityTypes.LUMBERJACK, 16755200, 16777045);
    public static final RegistryObject<SpawnEggItem> MINER_SPAWN_EGG = RegistryUtils.createSpawnEggItem("miner", ModEntityTypes.MINER, 16755200, 16777045);
    /*
    public static final RegistryObject<Item> MINER_SPAWN_EGG = RegistryUtils.createSpawnEggItem("miner", ModEntityTypes.MINER::get, 16755200, 16777045);
    public static final RegistryObject<Item> LUMBER_SPAWN_EGG = RegistryUtils.createSpawnEggItem("lumberjack", ModEntityTypes.LUMBERJACK::get, 16755200, 16777045);
    public static final RegistryObject<Item> FISHERMAN_SPAWN_EGG = RegistryUtils.createSpawnEggItem("fisherman", ModEntityTypes.FISHERMAN::get, 16755201, 16777044);
    public static final RegistryObject<Item> SHEPHERD_SPAWN_EGG = RegistryUtils.createSpawnEggItem("shepherd", ModEntityTypes.SHEPHERD::get, 16755200, 16777045);

    public static final RegistryObject<Item> MERCHANT_SPAWN_EGG = RegistryUtils.createSpawnEggItem("merchant", ModEntityTypes.MERCHANT::get, 16755200, 16777045);
    public static final RegistryObject<Item> CATTLE_FARMER_SPAWN_EGG = RegistryUtils.createSpawnEggItem("cattle_farmer", ModEntityTypes.CATTLE_FARMER::get, 16755200, 16777045);
    public static final RegistryObject<Item> CHICKEN_FARMER_SPAWN_EGG = RegistryUtils.createSpawnEggItem("chicken_farmer", ModEntityTypes.CHICKEN_FARMER::get, 16755200, 16777045);
    public static final RegistryObject<Item> SWINEHERD_SPAWN_EGG = RegistryUtils.createSpawnEggItem("swineherd", ModEntityTypes.SWINEHERD::get, 16755200, 16777045);
    */
}