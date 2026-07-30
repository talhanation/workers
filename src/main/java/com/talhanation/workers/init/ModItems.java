package com.talhanation.workers.init;

import com.google.common.collect.Lists;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.workers.WorkersMain;
import com.talhanation.workers.items.WorkersSpawnEgg;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, WorkersMain.MOD_ID);
    public static final List<DeferredHolder<Item, SpawnEggItem>> SPAWN_EGGS;

    static {
        SPAWN_EGGS = Lists.newArrayList();
    }
    public static final DeferredHolder<Item, SpawnEggItem> FARMER_SPAWN_EGG = createSpawnEggItem("farmer", ModEntityTypes.FARMER, 16755200, 16777045);
    public static final DeferredHolder<Item, SpawnEggItem> LUMBERJACK_SPAWN_EGG = createSpawnEggItem("lumberjack", ModEntityTypes.LUMBERJACK, 16755200, 16777045);
    public static final DeferredHolder<Item, SpawnEggItem> MINER_SPAWN_EGG = createSpawnEggItem("miner", ModEntityTypes.MINER, 16755200, 16777045);
    public static final DeferredHolder<Item, SpawnEggItem> MERCHANT_SPAWN_EGG = createSpawnEggItem("merchant", ModEntityTypes.MERCHANT, 16755200, 16777045);
    public static final DeferredHolder<Item, SpawnEggItem> BUILDER_SPAWN_EGG = createSpawnEggItem("builder", ModEntityTypes.BUILDER, 16755200, 16777045);
    public static final DeferredHolder<Item, SpawnEggItem> FISHERMAN_SPAWN_EGG = createSpawnEggItem("fisherman", ModEntityTypes.FISHERMAN, 16755200, 16777045);
    public static final DeferredHolder<Item, SpawnEggItem> ANIMAL_FARMER_SPAWN_EGG = createSpawnEggItem("animal_farmer", ModEntityTypes.ANIMAL_FARMER, 16755200, 16777045);
    public static final DeferredHolder<Item, SpawnEggItem> COURIER_SPAWN_EGG = createSpawnEggItem("courier", ModEntityTypes.COURIER, 16755200, 16777045);
    public static final DeferredHolder<Item, SpawnEggItem> COOK_SPAWN_EGG = createSpawnEggItem("cook", ModEntityTypes.COOK, 16755200, 16777045);

    public static DeferredHolder<Item, SpawnEggItem> createSpawnEggItem(String entityName, Supplier<? extends EntityType<? extends AbstractRecruitEntity>> supplier, int primaryColor, int secondaryColor) {
        DeferredHolder<Item, SpawnEggItem> spawnEgg = ModItems.ITEMS.register(entityName + "_spawn_egg", () -> {
            return new WorkersSpawnEgg(supplier, primaryColor, secondaryColor, new Item.Properties());
        });
        ModItems.SPAWN_EGGS.add(spawnEgg);
        return spawnEgg;
    }
}