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
    public static final RegistryObject<SpawnEggItem> ANIMAL_FARMER_SPAWN_EGG = createSpawnEggItem("animal_farmer", ModEntityTypes.ANIMAL_FARMER, 16755200, 16777045);
    public static final RegistryObject<SpawnEggItem> COURIER_SPAWN_EGG = createSpawnEggItem("courier", ModEntityTypes.COURIER, 16755200, 16777045);

    public static RegistryObject<SpawnEggItem> createSpawnEggItem(String entityName, Supplier<? extends EntityType<? extends AbstractRecruitEntity>> supplier, int primaryColor, int secondaryColor) {
        RegistryObject<SpawnEggItem> spawnEgg = ModItems.ITEMS.register(entityName + "_spawn_egg", () -> {
            return new WorkersSpawnEgg(supplier, primaryColor, secondaryColor, new Item.Properties());
        });
        ModItems.SPAWN_EGGS.add(spawnEgg);
        return spawnEgg;
    }
}