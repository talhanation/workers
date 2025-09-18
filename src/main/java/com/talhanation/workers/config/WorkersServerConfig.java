package com.talhanation.workers.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Arrays;

@Mod.EventBusSubscriber
public class WorkersServerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec SERVER;
    public static ArrayList<String> FARMER_PICKUP = new ArrayList<>(
            Arrays.asList(
                    "minecraft:wheat",
                    "minecraft:wheat_seeds",
                    "minecraft:beetroot_seeds",
                    "minecraft:beetroot_seeds",
                    "minecraft:carrot",
                    "minecraft:potato",
                    "minecraft:poisonous_potato",
                    "minecraft:pumpkin",
                    "minecraft:pumpkin_seeds",
                    "minecraft:melon",
                    "minecraft:melon_seeds",
                    "minecraft:melon_slice",
                    "supplementaries:flax"
            ));

    public static ArrayList<String> LUMBERMAN_PICKUP = new ArrayList<>(
            Arrays.asList(
                    "minecraft:stick",
                    "minecraft:bee_nest"
            ));

    public static ArrayList<String> MINER_PICKUP = new ArrayList<>(
            Arrays.asList(
                    "minecraft:torch",
                    "minecraft:lantern",
                    "minecraft:redstone",
                    "minecraft:cobblestone",
                    "minecraft:sandstone",
                    "minecraft:sand",
                    "minecraft:gravel",
                    "minecraft:calcite",
                    "minecraft:clay",
                    "minecraft:dripstone_block",
                    "minecraft:pointed_dripstone",
                    "minecraft:netherrack",
                    "minecraft:emerald",
                    "minecraft:lapis_lazuli",
                    "minecraft:diamond"
            ));

    public static ArrayList<String> MINER_IGNORE = new ArrayList<>(
            Arrays.asList(
                    "minecraft:air",
                    "minecraft:cave_air",
                    "minecraft:torch",
                    "minecraft:wall_torch",
                    "minecraft:lantern",
                    "minecraft:lever",
                    "minecraft:redstone_torch",
                    "minecraft:redstone_wall_torch",
                    "minecraft:redstone_wire",
                    "minecraft:redstone_lamp",
                    "minecraft:rail",
                    "minecraft:water",
                    "minecraft:soul_lantern",
                    "minecraft:soul_torch",
                    "minecraft:soul_wall_torch",
                    "minecraft:netherrack"
            ));
}
