package com.talhanation.workers.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Arrays;

@Mod.EventBusSubscriber
public class WorkersServerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec SERVER;
    public static ForgeConfigSpec.IntValue FarmerCost;
    public static ForgeConfigSpec.IntValue LumberjackCost;
    public static ForgeConfigSpec.IntValue MinerCost;
    public static ForgeConfigSpec.IntValue BuilderCost;
    public static ForgeConfigSpec.IntValue MerchantCost;
    public static ForgeConfigSpec.IntValue CourierCost;
    public static ForgeConfigSpec.BooleanValue BuilderActive;
    public static ForgeConfigSpec.IntValue AnimalPenMaxAnimals;
    public static ForgeConfigSpec.BooleanValue ShouldWorkAreaOnlyBeInFactionClaim;
    public static ForgeConfigSpec.BooleanValue ShouldOnlyPlacingBuildingsBePossible;
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
                    "supplementaries:flax",
                    "herbalbrews:green_tea_leaf"

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
                    "minecraft:flint",
                    "minecraft:coal",
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

    public static ArrayList<String> ANIMAL_FARMER_PICKUP = new ArrayList<>(
            Arrays.asList(
                    "minecraft:feather",
                    "minecraft:leather",
                    "minecraft:milk_bucket",
                    "minecraft:chicken",
                    "minecraft:lamb",
                    "minecraft:egg",
                    "minecraft:lamb",
                    "minecraft:lamb",
                    "minecraft:lamb",
                    "minecraft:lamb",
                    "minecraft:lamb",
                    "minecraft:lamb",
                    "minecraft:lamb",
                    "minecraft:lamb",
                    "minecraft:lamb",
                    "minecraft:lamb",
                    "minecraft:lamb"
            ));


    public static ArrayList<String> FISHERMAN_PICKUP = new ArrayList<>(
            Arrays.asList(

            ));

    static {
        BUILDER.comment("Workers Config:").push("Workers");
        FarmerCost = BUILDER.comment("""
                        
                        The amount of currency required to hire a farmer.
                        \t(takes effect after restart)
                        \tdefault: 10""")
                .worldRestart()
                .defineInRange("FarmerCost", 10, 0, 1453);

        LumberjackCost = BUILDER.comment("""
                        
                        The amount of currency required to hire a lumberjack.
                        \t(takes effect after restart)
                        \tdefault: 12""")
                .worldRestart()
                .defineInRange("LumberjackCost", 12, 0, 1453);

        MinerCost = BUILDER.comment("""
                        
                        The amount of currency required to hire a miner.
                        \t(takes effect after restart)
                        \tdefault: 16""")
                .worldRestart()
                .defineInRange("MinerCost", 16, 0, 1453);

        BuilderCost = BUILDER.comment("""
                        
                        The amount of currency required to hire a builder.
                        \t(takes effect after restart)
                        \tdefault: 20""")
                .worldRestart()
                .defineInRange("BuilderCost", 20, 0, 1453);

        MerchantCost = BUILDER.comment("""
                        
                        The amount of currency required to hire a merchant.
                        \t(takes effect after restart)
                        \tdefault: 30""")
                .worldRestart()
                .defineInRange("MerchantCost", 30, 0, 1453);

        CourierCost = BUILDER.comment("""
                        
                        The amount of currency required to hire a courier.
                        \t(takes effect after restart)
                        \tdefault: 30""")
                .worldRestart()
                .defineInRange("CourierCost", 20, 0, 1453);

        ShouldWorkAreaOnlyBeInFactionClaim = BUILDER.comment("""
                        
                        Should placing a work ara or a building only be allowed when in a claim of the own faction.
                        \t(takes effect after restart)
                        \tdefault: false""")
                .worldRestart()
                .define("ShouldWorkAreaOnlyBeInFactionClaim", false);

        ShouldOnlyPlacingBuildingsBePossible = BUILDER.comment("""
                        
                        Should only buildings be allowed to place.
                        \t(takes effect after restart)
                        \tdefault: false""")
                .worldRestart()
                .define("ShouldOnlyPlacingBuildingsBePossible", false);

        AnimalPenMaxAnimals = BUILDER.comment("""
                        
                        The max amount of animals in a pen. After the animal worker will not breed. 
                        \t(takes effect after restart)
                        \tdefault: 32""")
                .worldRestart()
                .defineInRange("AnimalPenMaxAnimals", 32, 0, 1453);

        SERVER = BUILDER.build();
    }
}
