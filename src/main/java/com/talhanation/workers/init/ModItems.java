package com.talhanation.workers.init;

import com.google.common.collect.Lists;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.items.WorkersSpawnEgg;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, WorkersMain.MOD_ID);
    public static final List<RegistryObject<SpawnEggItem>> SPAWN_EGGS;

    
    static {
        SPAWN_EGGS = Lists.newArrayList();
    }


    public static final RegistryObject<SpawnEggItem> FARMER_SPAWN_EGG = createSpawnEggItem("farmer", ModEntityTypes.FARMER, 16755200, 16777045);
    public static final RegistryObject<SpawnEggItem> LUMBERJACK_SPAWN_EGG = createSpawnEggItem("lumberjack", ModEntityTypes.LUMBERJACK, 16755200, 16777045);
    public static final RegistryObject<SpawnEggItem> MINER_SPAWN_EGG = createSpawnEggItem("miner", ModEntityTypes.MINER, 16755200, 16777045);
    public static final RegistryObject<SpawnEggItem> MERCHANT_SPAWN_EGG = createSpawnEggItem("merchant", ModEntityTypes.MERCHANT, 16755200, 16777045);
    public static final RegistryObject<SpawnEggItem> BUILDER_SPAWN_EGG = createSpawnEggItem("builder", ModEntityTypes.BUILDER, 16755200, 16777045);
    public static final RegistryObject<SpawnEggItem> FISHERMAN_SPAWN_EGG = createSpawnEggItem("fisherman", ModEntityTypes.FISHERMAN, 16755200, 16777045);

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

    public static RegistryObject<SpawnEggItem> createSpawnEggItem(String entityName, Supplier<? extends EntityType<? extends AbstractRecruitEntity>> supplier, int primaryColor, int secondaryColor) {
        RegistryObject<SpawnEggItem> spawnEgg = ModItems.ITEMS.register(entityName + "_spawn_egg", () -> {
            return new WorkersSpawnEgg(supplier, primaryColor, secondaryColor, new Item.Properties());
        });
        ModItems.SPAWN_EGGS.add(spawnEgg);
        return spawnEgg;
    }
}