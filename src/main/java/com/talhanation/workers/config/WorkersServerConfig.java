package com.talhanation.workers.config;

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
    public static ForgeConfigSpec.IntValue CraftsmanCost;
    public static ForgeConfigSpec.ConfigValue<List<? extends Integer>> CraftsmanMoraleTicks;
    public static ForgeConfigSpec.BooleanValue VillagerBreedMixinEnabled;
    public static ForgeConfigSpec.IntValue VillagerBreedSaturationThreshold;
    public static ForgeConfigSpec.IntValue AnimalPenMaxAnimals;
    public static ForgeConfigSpec.BooleanValue ShouldWorkAreaOnlyBeInFactionClaim;
    public static ForgeConfigSpec.BooleanValue ShouldOnlyPlacingBuildingsBePossible;
    public static ForgeConfigSpec.EnumValue<BuildMode> BuildModeConfig;
    public static ForgeConfigSpec.EnumValue<BuildPaymentMode> BuildPaymentModeConfig;
    public static ForgeConfigSpec.IntValue BuildingBasePrice;
    public static ForgeConfigSpec.DoubleValue BuildingSizePrice;
    public static ForgeConfigSpec.DoubleValue BuildingDefaultMaterialPrice;
    public static ForgeConfigSpec.ConfigValue<List<String>> BuildingMaterialPrices;
    public static ForgeConfigSpec.ConfigValue<List<String>> BuildingBlockedMaterials;
    public static ForgeConfigSpec.ConfigValue<List<String>> MinerPickup;
    public static ForgeConfigSpec.ConfigValue<List<String>> MinerIgnore;
    public static ForgeConfigSpec.ConfigValue<List<String>> FarmerPickup;
    public static ForgeConfigSpec.ConfigValue<List<String>> AnimalFarmerPickup;
    public static ForgeConfigSpec.ConfigValue<List<String>> LumberjackPickup;
    public static ForgeConfigSpec.ConfigValue<List<String>> FishermanPickup;
    public static ForgeConfigSpec.BooleanValue WorkersReplaceAllay;
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
                    "minecraft:clay_ball",
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

    // Currency per 64 BASE items (what the builder would consume, e.g. stairs count as their planks/stone).
    // Every price is above what villagers pay for that item, so "buy building -> break it -> sell to villagers"
    // never makes profit (e.g. mason buys 20 stone per emerald = 3.2 per stack, stone costs 4).
    // "#" entries are item tags, the first matching entry wins.
    public static ArrayList<String> BUILDING_MATERIAL_PRICES = new ArrayList<>(
            Arrays.asList(
                    "minecraft:dirt=1",
                    "minecraft:coarse_dirt=1",
                    "minecraft:sand=1",
                    "minecraft:red_sand=1",
                    "minecraft:gravel=1",
                    "minecraft:netherrack=1",
                    "minecraft:cobblestone=3",
                    "minecraft:cobbled_deepslate=3",
                    "minecraft:mossy_cobblestone=4",
                    "minecraft:stone=4",
                    "minecraft:deepslate=4",
                    "minecraft:smooth_stone=5",
                    "minecraft:stone_bricks=5",
                    "minecraft:cracked_stone_bricks=5",
                    "minecraft:chiseled_stone_bricks=5",
                    "minecraft:mossy_stone_bricks=6",
                    "minecraft:granite=5",
                    "minecraft:diorite=5",
                    "minecraft:andesite=5",
                    "minecraft:polished_granite=5",
                    "minecraft:polished_diorite=5",
                    "minecraft:polished_andesite=5",
                    "minecraft:polished_deepslate=5",
                    "minecraft:deepslate_bricks=5",
                    "minecraft:deepslate_tiles=5",
                    "minecraft:sandstone=2",
                    "minecraft:cut_sandstone=2",
                    "minecraft:smooth_sandstone=3",
                    "minecraft:red_sandstone=2",
                    "minecraft:cut_red_sandstone=2",
                    "minecraft:smooth_red_sandstone=3",
                    "minecraft:bricks=8",
                    "minecraft:mud_bricks=4",
                    "minecraft:packed_mud=3",
                    "minecraft:clay=28",
                    "minecraft:end_stone=3",
                    "minecraft:end_stone_bricks=4",
                    "minecraft:purpur_block=8",
                    "minecraft:blackstone=3",
                    "minecraft:polished_blackstone=4",
                    "minecraft:polished_blackstone_bricks=4",
                    "minecraft:basalt=3",
                    "minecraft:smooth_basalt=4",
                    "minecraft:nether_bricks=6",
                    "minecraft:red_nether_bricks=8",
                    "minecraft:quartz_block=28",
                    "minecraft:smooth_quartz=28",
                    "minecraft:quartz_bricks=28",
                    "minecraft:quartz_pillar=28",
                    "minecraft:chiseled_quartz_block=28",
                    "minecraft:prismarine=12",
                    "minecraft:prismarine_bricks=20",
                    "minecraft:dark_prismarine=20",
                    "minecraft:sea_lantern=32",
                    "minecraft:glowstone=16",
                    "minecraft:hay_block=36",
                    "minecraft:melon=20",
                    "minecraft:pumpkin=14",
                    "minecraft:bookshelf=60",
                    "minecraft:torch=2",
                    "minecraft:lantern=16",
                    "minecraft:ladder=4",
                    "minecraft:iron_bars=8",
                    "minecraft:iron_door=40",
                    "minecraft:chest=40",
                    "minecraft:barrel=40",
                    "minecraft:crafting_table=20",
                    "minecraft:furnace=24",
                    "minecraft:copper_block=40",
                    "minecraft:cut_copper=40",
                    "minecraft:coal_block=48",
                    "minecraft:redstone_block=48",
                    "minecraft:lapis_block=64",
                    "minecraft:iron_block=180",
                    "minecraft:raw_iron_block=180",
                    "minecraft:gold_block=240",
                    "minecraft:raw_gold_block=240",
                    "minecraft:diamond_block=720",
                    "minecraft:emerald_block=720",
                    "#minecraft:planks=5",
                    "#minecraft:logs=20",
                    "#minecraft:wool=5",
                    "#minecraft:terracotta=6",
                    "#minecraft:beds=32",
                    "#forge:glass=3",
                    "#forge:glass_panes=2"
            ));

    // Base items a building can NOT contain when it should be bought with currency.
    public static ArrayList<String> BUILDING_BLOCKED_MATERIALS = new ArrayList<>(
            Arrays.asList(
                    "#forge:ores",
                    "#minecraft:shulker_boxes",
                    "minecraft:spawner",
                    "minecraft:budding_amethyst",
                    "minecraft:ancient_debris",
                    "minecraft:netherite_block",
                    "minecraft:beacon",
                    "minecraft:conduit",
                    "minecraft:dragon_egg",
                    "minecraft:end_portal_frame",
                    "minecraft:reinforced_deepslate",
                    "minecraft:bedrock",
                    "minecraft:sponge",
                    "minecraft:wet_sponge",
                    "minecraft:command_block",
                    "minecraft:chain_command_block",
                    "minecraft:repeating_command_block",
                    "minecraft:structure_block",
                    "minecraft:jigsaw",
                    "minecraft:barrier",
                    "minecraft:light"
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

        CraftsmanCost = BUILDER.comment("""
                        
                        The amount of currency required to hire a craftsman.
                        \t(takes effect after restart)
                        \tdefault: 30""")
                .worldRestart()
                .defineInRange("CraftsmanCost", 30, 0, 1453);

        CraftsmanMoraleTicks = BUILDER.comment("""
                        
                        Ticks the craftsman needs for one crafted item, depending on his morale.
                        The five values are used for morale >= 80, >= 60, >= 40, >= 20 and below 20.
                        \t(takes effect after restart)
                        \tdefault: [1, 2, 3, 5, 8]""")
                .worldRestart()
                .defineList("CraftsmanMoraleTicks", Arrays.asList(1, 2, 3, 5, 8), o -> o instanceof Integer i && i >= 1);

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

        BuildPaymentModeConfig = BUILDER.comment("""

                        Controls how buildings are paid on this server.
                        \t MATERIALS - the builder fetches the base materials from storage (default)
                        \t CURRENCY  - the player pays a price in currency when pressing build,
                        \t             the builder needs no materials. Only missing blocks are charged.
                        \t             price = BuildingBasePrice + placed blocks / 64 * BuildingSizePrice + material prices
                        \t(takes effect after restart)""")
                .worldRestart()
                .defineEnum("BuildPaymentMode", BuildPaymentMode.MATERIALS);

        BuildingBasePrice = BUILDER.comment("""

                        CURRENCY mode: flat fee in currency for every building.
                        \t(takes effect after restart)
                        \tdefault: 2""")
                .worldRestart()
                .defineInRange("BuildingBasePrice", 2, 0, 1453);

        BuildingSizePrice = BUILDER.comment("""

                        CURRENCY mode: currency per 64 placed blocks (the work of the builder).
                        \t(takes effect after restart)
                        \tdefault: 1.0""")
                .worldRestart()
                .defineInRange("BuildingSizePrice", 1.0D, 0.0D, 1453.0D);

        BuildingDefaultMaterialPrice = BUILDER.comment("""

                        CURRENCY mode: currency per 64 base items for materials that are not in BuildingMaterialPrices.
                        \t(takes effect after restart)
                        \tdefault: 8.0""")
                .worldRestart()
                .defineInRange("BuildingDefaultMaterialPrice", 8.0D, 0.0D, 100000.0D);

        BuildingMaterialPrices = BUILDER.comment("""

                        CURRENCY mode: currency per 64 BASE items ("item=price" or "#item_tag=price", first match wins).
                        Blocks are counted as their base material: stairs, slabs, doors, ... count as their planks/stone.
                        Keep prices above what villagers pay for the item, otherwise players can buy a building,
                        break it and sell the blocks for profit.
                        \t(takes effect after restart)""")
                .worldRestart()
                .define("BuildingMaterialPrices", BUILDING_MATERIAL_PRICES);

        BuildingBlockedMaterials = BUILDER.comment("""

                        CURRENCY mode: buildings containing these base items ("item" or "#item_tag") can not be bought.
                        \t(takes effect after restart)""")
                .worldRestart()
                .define("BuildingBlockedMaterials", BUILDING_BLOCKED_MATERIALS);

        WorkersReplaceAllay = BUILDER.comment("""
                        
                        Should workers replace allay spawns. (In Pillager outposts and mansion)
                        \t(takes effect after restart)
                        \tdefault: true""")
                .worldRestart()
                .define("WorkersReplaceAllay", true);

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
