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
    public static final int NEW_VERSION = 3;


    public static ForgeConfigSpec.BooleanValue PlayVillagerAmbientSound;
    public static ForgeConfigSpec.BooleanValue WorkersLookLikeVillagers;

    static{
        VERSION = BUILDER.comment("\n" +"##Version, do not change!##")
                .defineInRange("Version", 0, 0, Integer.MAX_VALUE);

        BUILDER.comment("Workers Config:").push("Workers");

        PlayVillagerAmbientSound = BUILDER.comment("\n" + "----Should Workers Make Villager Huh? sound?----" + "\n" +
                "\t" + "(takes effect after restart)" + "\n" +
                "\t" + "default: true")
                .define("PlayVillagerAmbientSound", true);

        WorkersLookLikeVillagers = BUILDER.comment("\n" + "----Should Workers look like Villagers?----" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: true")
                .worldRestart()
                .define("WorkersLookLikeVillagers", true);

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

