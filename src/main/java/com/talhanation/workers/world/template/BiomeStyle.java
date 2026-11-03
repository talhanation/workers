package com.talhanation.workers.world.template;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nullable;

/**
 * Building style of the default templates, the same five styles as vanilla villages.
 * Templates in a folder named like a style ("vanilla/taiga/...") belong to it, all others fit everywhere.
 */
public enum BiomeStyle {
    PLAINS("plains"),
    DESERT("desert"),
    SAVANNA("savanna"),
    TAIGA("taiga"),
    SNOWY("snowy");

    private final String key;

    BiomeStyle(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Nullable
    public static BiomeStyle fromKey(String key) {
        for (BiomeStyle style : values()) if (style.key.equalsIgnoreCase(key)) return style;
        return null;
    }

    /** Style of a biome by the vanilla village tags, plains for every biome without villages. */
    public static BiomeStyle of(Holder<Biome> biome) {
        if (biome.is(BiomeTags.HAS_VILLAGE_DESERT)) return DESERT;
        if (biome.is(BiomeTags.HAS_VILLAGE_SAVANNA)) return SAVANNA;
        if (biome.is(BiomeTags.HAS_VILLAGE_TAIGA)) return TAIGA;
        if (biome.is(BiomeTags.HAS_VILLAGE_SNOWY)) return SNOWY;
        return PLAINS;
    }
}
