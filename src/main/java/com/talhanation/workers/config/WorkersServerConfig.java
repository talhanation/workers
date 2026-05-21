package com.talhanation.workers.config;

import com.talhanation.workers.config.BuildMode;
import com.talhanation.workers.entities.FishermanEntity;
import com.talhanation.workers.entities.LumberjackEntity;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public static ForgeConfigSpec.IntValue CookCost;
    public static ForgeConfigSpec.BooleanValue VillagerBreedMixinEnabled;
    public static ForgeConfigSpec.IntValue VillagerBreedSaturationThreshold;
    public static ForgeConfigSpec.IntValue AnimalPenMaxAnimals;
    public static ForgeConfigSpec.BooleanValue ShouldWorkAreaOnlyBeInFactionClaim;
    public static ForgeConfigSpec.BooleanValue ShouldOnlyPlacingBuildingsBePossible;
    public static ForgeConfigSpec.EnumValue<BuildMode> BuildModeConfig;
    public static ForgeConfigSpec.ConfigValue<List<String>> MinerPickup;
    public static ForgeConfigSpec.ConfigValue<List<String>> MinerIgnore;
    public static ForgeConfigSpec.ConfigValue<List<String>> FarmerPickup;
    public static ForgeConfigSpec.ConfigValue<List<String>> AnimalFarmerPickup;
    public static ForgeConfigSpec.ConfigValue<List<String>> LumberjackPickup;
    public static ForgeConfigSpec.ConfigValue<List<String>> FishermanPickup;
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
                    "farmersdelight:straw",
                    "herbalbrews:green_tea_leaf"
            ));
    public static ArrayList<String> MINER_PICKUP = new ArrayList<>(
            Arrays.asList(
                    "minecraft:torch",
                    "minecraft:lantern",
                    "minecraft:redstone",
                    "minecraft:cobblestone",
                    "minecraft:deepslate",
                    "minecraft:cobbled_deepslate",
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
                    "minecraft:diamond",
                    "psg:raw_potassium",
                    "psg:sulphur"
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
                    "minecraft:mutton",
                    "minecraft:egg",
                    "minecraft:prokchop",
                    "minecraft:beef",
                    "minecraft:rabbit",
                    "naturalist:duck",
                    "naturalist:venison",
                    "naturalist:fur",
                    "naturalist:bushmeat",
                    "naturalist:antler",
                    "naturalist:duck_egg",
                    "farmersdelight:ham"
            ));


    public static ArrayList<String> FISHERMAN_PICKUP = new ArrayList<>(
            Arrays.asList(
                    "minecraft:tropical_fish",
                    "minecraft:pufferfish",
                    "naturalist:catfish",
                    "naturalist:bass"
            ));

    public static ArrayList<String> LUMBERJACK_PICKUP = new ArrayList<>(
            Arrays.asList(
                    "minecraft:stick",
                    "minecraft:bee_nest",
                    "dynamictrees:oak_seed",
                    "dynamictrees:birch_seed",
                    "dynamictrees:apple_oak_seed",
                    "dynamictrees:spruce_seed",
                    "dynamictrees:cocoa_seed"
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

        CookCost = BUILDER.comment("""
                        
                        The amount of currency required to hire a cook.
                        \t(takes effect after restart)
                        \tdefault: 25""")
                .worldRestart()
                .defineInRange("CookCost", 25, 0, 1453);

        VillagerBreedMixinEnabled = BUILDER.comment("""
                        
                        Whether the villager breeding mixin is active.
                        When enabled, villagers can breed using any food item
                        based on its nutrition value instead of only vanilla
                        bread/carrot/potato/beetroot.
                        Disable this if you experience compatibility issues with other mods.
                        \t(takes effect after restart)
                        \tdefault: true""")
                .worldRestart()
                .define("VillagerBreedMixinEnabled", true);

        VillagerBreedSaturationThreshold = BUILDER.comment("""
                        
                        The total saturation value required in a villager's inventory
                        to trigger breeding (only used when VillagerBreedMixinEnabled = true).
                        Saturation is calculated as: nutrition * saturationModifier * 2
                        Examples at default threshold 15:
                        - Bread (saturation 6.0): needs 3 bread  → 18 >= 15
                        - Cooked Beef (saturation 12.8): needs 2 → 25 >= 15
                        - Baked Potato (saturation 6.0): needs 3 → 18 >= 15
                        \t(takes effect after restart)
                        \tdefault: 15""")
                .worldRestart()
                .defineInRange("VillagerBreedSaturationThreshold", 15, 1, 100);

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

        BuildModeConfig = BUILDER.comment("""

                        Controls which build mode is active on this server.
                        \t FREE           - clients may use local scans (default)
                        \t PRESET         - only server-side preset files can be loaded
                        \t PRESET_FACTIONS - like PRESET but filtered by team folder
                        \t(takes effect after restart)""")
                .worldRestart()
                .defineEnum("BuildMode", BuildMode.FREE);

        MinerPickup = BUILDER.comment("""
                        
                        Pickup configuration for miners
                        \t(takes effect after restart)
                        \tItems in this list will be picked up, for example: ["minecraft:sugar", "minecraft:sheep", ...]""")
                .worldRestart()
                .define("MinerPickup", MINER_PICKUP);

        MinerIgnore = BUILDER.comment("""
                        
                        Blocks in this list will be skipped by the miner.
                        \t(takes effect after restart)
                        \tFor example: ["minecraft:torch", "minecraft:chest", ...]""")
                .worldRestart()
                .define("MinerIgnore", MINER_IGNORE);

        AnimalFarmerPickup = BUILDER.comment("""
                        
                        Pickup configuration for animal-farmers
                        \t(takes effect after restart)
                        \tItems in this list will be picked up, for example: ["minecraft:sugar", "minecraft:stone", ...]""")
                .worldRestart()
                .define("AnimalFarmerPickup", ANIMAL_FARMER_PICKUP);

        FarmerPickup = BUILDER.comment("""
                        
                        Pickup configuration for farmers
                        \t(takes effect after restart)
                        \tItems in this list will be picked up, for example: ["minecraft:sugar", "minecraft:stone", ...]""")
                .worldRestart()
                .define("FarmerPickup", FARMER_PICKUP);

        LumberjackPickup = BUILDER.comment("""
                        
                        Pickup configuration for lumberjacks
                        \t(takes effect after restart)
                        \tItems in this list will be picked up, for example: ["minecraft:sugar", "minecraft:stone", ...]""")
                .worldRestart()
                .define("LumberjackPickup", LUMBERJACK_PICKUP);

        FishermanPickup = BUILDER.comment("""
                        
                        Pickup configuration for fishermans
                        \t(takes effect after restart)
                        \tItems in this list will be picked up, for example: ["minecraft:sugar", "minecraft:stone", ...]""")
                .worldRestart()
                .define("FishermanPickup", FISHERMAN_PICKUP);

        SERVER = BUILDER.build();
    }
}
