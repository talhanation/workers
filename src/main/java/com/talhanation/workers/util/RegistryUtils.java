package com.talhanation.workers.util;

import com.talhanation.workers.init.ModItems;
import com.talhanation.workers.items.WorkersSpawnEgg;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class RegistryUtils {

    public static RegistryObject<Item> createSpawnEggItem(String entityName, Supplier<? extends EntityType<? extends Mob>> supplier, int primaryColor, int secondaryColor) {
        RegistryObject<Item> spawnEgg = ModItems.ITEMS.register(entityName + "_spawn_egg", () -> new WorkersSpawnEgg(supplier, primaryColor, secondaryColor, new Item.Properties()));
        ModItems.SPAWN_EGGS.add(spawnEgg);
        return spawnEgg;
    }
}