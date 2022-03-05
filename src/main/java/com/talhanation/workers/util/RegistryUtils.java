package com.talhanation.workers.util;

import com.talhanation.workers.init.ModItems;
import com.talhanation.workers.items.WorkersSpawnEgg;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.RegistryObject;

import java.util.function.Supplier;

public class RegistryUtils {

    public static RegistryObject<Item> createSpawnEggItem(String entityName, Supplier<EntityType<?>> supplier, int primaryColor, int secondaryColor) {
        RegistryObject<Item> spawnEgg = ModItems.ITEMS.register(entityName + "_spawn_egg", () -> new WorkersSpawnEgg(supplier, primaryColor, secondaryColor, new Item.Properties().tab(ItemGroup.TAB_MISC)));
        ModItems.SPAWN_EGGS.add(spawnEgg);
        return spawnEgg;
    }
}