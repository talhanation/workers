package com.talhanation.workers.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Path;

@Mod.EventBusSubscriber
public class WorkersModConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec CONFIG;
    public static ForgeConfigSpec.IntValue VERSION;
    public static final int NEW_VERSION = 6;

    public static ForgeConfigSpec.BooleanValue WorkerChunkLoading;
    public static ForgeConfigSpec.BooleanValue PlayVillagerAmbientSound;
    public static ForgeConfigSpec.BooleanValue WorkersLookLikeVillagers;
    public static ForgeConfigSpec.IntValue MinerCost;
    public static ForgeConfigSpec.IntValue LumberjackCost;
    public static ForgeConfigSpec.IntValue FishermanCost;
    public static ForgeConfigSpec.IntValue FarmerCost;
    public static ForgeConfigSpec.IntValue CattleFarmerCost;
    public static ForgeConfigSpec.IntValue ChickenFarmerCost;
    public static ForgeConfigSpec.IntValue MerchantCost;
    public static ForgeConfigSpec.IntValue ShepherdCost;
    public static ForgeConfigSpec.IntValue SwineherdCost;
    public static ForgeConfigSpec.ConfigValue<String> WorkersCurrency;
    public static ForgeConfigSpec.BooleanValue CommandScreenToggle;
    public static ForgeConfigSpec.BooleanValue MonsterAttackWorkers;
    public static ForgeConfigSpec.BooleanValue PillagerAttackWorkers;
    public static ForgeConfigSpec.BooleanValue OwnerReceiveInfo;
    public static ForgeConfigSpec.BooleanValue WorkersTablesPOIReleasing;

    static{
        VERSION = BUILDER.comment("\n" +"##Version, do not change!##")
                .defineInRange("Version", 0, 0, Integer.MAX_VALUE);

        BUILDER.comment("Workers Config Client Side:").push("WorkersClientSide");

        PlayVillagerAmbientSound = BUILDER.comment("""
                        
                        ----Should Workers Make Villager Huh? sound?----
                        \t(takes effect after restart)
                        \tdefault: true""")
                .define("PlayVillagerAmbientSound", true);

        WorkersLookLikeVillagers = BUILDER.comment("""
                        
                        ----Should Workers look like Villagers?----
                        \t(takes effect after restart)
                        \tdefault: true""")
                .worldRestart()
                .define("WorkersLookLikeVillagers", true);

        CommandScreenToggle = BUILDER.comment("""
                        
                        ----CommandScreenToggle----
                        \t(takes effect after restart)
                        \t
                        Should the key to open the command screen be toggled instead of held?""
                        default: false""")

                .worldRestart()
                .define("CommandScreenToggle", false);
        /*
        Workers Config
         */
        BUILDER.pop();
        BUILDER.comment("Workers Config:").push("Workers");

        OwnerReceiveInfo = BUILDER.comment("""

                        Should Owners receive info's about the worker through text?
                        \t(takes effect after restart)
                        \tdefault: true""")
                .worldRestart()
                .define("OwnerReceiveInfo", true);

        WorkerChunkLoading = BUILDER.comment("""
                        
                        ----WorkerChunkLoading----
                        \t(takes effect after restart)
                        \t
                        Should workers load the chunk they are in?""
                        default: true""")

                .worldRestart()
                .define("WorkerChunkLoading", true);

        WorkersCurrency = BUILDER.comment("""

                        ----Currency----
                        \t(takes effect after restart)
                        \tThe Item defined here, will be used to hire workers. For example: ["minecraft:diamond"]\tdefault: ["minecraft:emerald"]""")
                .worldRestart()
                .define("WorkersCurrency", "minecraft:emerald");

        MinerCost = BUILDER.comment("""

                        The amount of currency required to hire a miner.
                        \t(takes effect after restart)
                        \tdefault: 20""")
                .worldRestart()
                .defineInRange("MinerCost", 20, 0, 999);

        LumberjackCost = BUILDER.comment("""

                        The amount of currency required to hire a lumberjack.
                        \t(takes effect after restart)
                        \tdefault: 14""")
                .worldRestart()
                .defineInRange("LumberjackCost", 14, 0, 999);

        FishermanCost = BUILDER.comment("""

                        The amount of currency required to hire a fisherman.
                        \t(takes effect after restart)
                        \tdefault: 25""")
                .worldRestart()
                .defineInRange("FishermanCost", 25, 0, 999);

        FarmerCost = BUILDER.comment("""

                        The amount of currency required to hire a farmer.
                        \t(takes effect after restart)
                        \tdefault: 11""")
                .worldRestart()
                .defineInRange("FarmerCost", 11, 0, 999);

        ChickenFarmerCost = BUILDER.comment("""

                        The amount of currency required to hire a chicken farmer.
                        \t(takes effect after restart)
                        \tdefault: 15""")
                .worldRestart()
                .defineInRange("ChickenFarmerCost", 15, 0, 999);

        CattleFarmerCost = BUILDER.comment("""

                        The amount of currency required to hire a cattle farmer.
                        \t(takes effect after restart)
                        \tdefault: 15""")
                .worldRestart()
                .defineInRange("CattleFarmerCost", 18, 0, 999);

        ShepherdCost = BUILDER.comment("""

                        The amount of currency required to hire a shepherd.
                        \t(takes effect after restart)
                        \tdefault: 10""")
                .worldRestart()
                .defineInRange("ShepherdCost", 16, 0, 999);

        SwineherdCost = BUILDER.comment("""

                        The amount of currency required to hire a swineherd.
                        \t(takes effect after restart)
                        \tdefault: 10""")
                .worldRestart()
                .defineInRange("SwineherdCost", 19, 0, 999);

        MerchantCost = BUILDER.comment("""

                        The amount of currency required to hire a merchant.
                        \t(takes effect after restart)
                        \tdefault: 12""")
                .worldRestart()
                .defineInRange("MerchantCost", 12, 0, 999);

        MonsterAttackWorkers = BUILDER.comment("""

                        Should Hostile Mobs attack Workers?
                        \t(takes effect after restart)
                        \tdefault: true""")
                .worldRestart()
                .define("MonsterAttackWorkers", true);

        PillagerAttackWorkers = BUILDER.comment("""

                        Should Pillagers attack Workers?
                        \t(takes effect after restart)
                        \tdefault: true""")
                .worldRestart()
                .define("PillagerAttackWorkers", true);

        BUILDER.pop();
        BUILDER.comment("Recruit Village Config:").push("Villages");

        WorkersTablesPOIReleasing = BUILDER.comment("""

                        ----Should Villager Workers that were created with Tables release the POI for other Villagers?----
                        ----True -> allows multiple villagers to become a worker with one table.----
                        ----False -> only one villager can become a recruit with one table.----
                        \t(takes effect after restart)
                        \tdefault: true""")
                .worldRestart()
                .define("WorkersTablesPOIReleasing", true);


        /*
        BeekeeperCost = BUILDER.comment("""

                        The amount of currency required to hire a beekeeper.
                        \t(takes effect after restart)
                        \tdefault: 15""")
                .worldRestart()
                .defineInRange("BeekeeperCost", 22, 0, 999);

         */

        /*
        RabbitFarmerCost = BUILDER.comment("""

                        The amount of currency required to hire a rabbit farmer.
                        \t(takes effect after restart)
                        \tdefault: 15""")
                .worldRestart()
                .defineInRange("RabbitFarmerCost", 22, 0, 999);

         */


    CONFIG = BUILDER.build();
}

    public static void loadConfig(ForgeConfigSpec spec, Path path) {
        CommentedFileConfig configData = CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build();
        configData.load();
        spec.setConfig(configData);
        if (VERSION.get() != NEW_VERSION) {
            configData = CommentedFileConfig.builder(path)
                    .sync()
                    .autosave()
                    .writingMode(WritingMode.REPLACE)
                    .build();
            spec.setConfig(configData);
            VERSION.set(NEW_VERSION);
            configData.save();
        }
    }

}

