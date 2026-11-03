package com.talhanation.workers.world.template;

import javax.annotation.Nullable;

/**
 * What a building template is for. Detected from the file name (keywords) or, for work buildings,
 * from the work areas stored in the scan. Each type has its own required content, see {@link TemplateValidator}.
 */
public enum BuildingType {
    // work buildings: detected by their work area
    WORKER_HOUSE("worker_house", "workers:homearea"),
    VILLAGER_HOUSE("villager_house", null),
    FARM("farm", "workers:croparea"),
    LUMBER("lumber", "workers:lumberarea"),
    FISHER("fisher", "workers:fishingarea"),
    ANIMAL_PEN("animal_pen", "workers:animalpenarea"),
    MINE("mine", "workers:miningarea"),
    MARKET("market", "workers:marketarea"),
    STORAGE("storage", "workers:storagearea"),
    KITCHEN("kitchen", "workers:kitchenarea"),
    WORKSHOP("workshop", "workers:workshoparea"),
    // infrastructure: only by file name
    ROAD("road", null),
    WALL_BASIC("wall_basic", null),
    WALL_STRAIGHT("wall_straight", null),
    WALL_CORNER("wall_corner", null),
    GATE_BASIC("gate_basic", null),
    GATE_EXTENDED("gate_extended", null);

    private final String key;
    @Nullable
    private final String workArea;

    BuildingType(String key, @Nullable String workArea) {
        this.key = key;
        this.workArea = workArea;
    }

    public String getKey() {
        return key;
    }

    /** Entity id of the work area this building must contain, null if it needs none. */
    @Nullable
    public String getWorkArea() {
        return workArea;
    }

    /** Wall pieces, gates and roads are placed on a grid and need an exact footprint. */
    public boolean isInfrastructure() {
        return this.ordinal() >= ROAD.ordinal();
    }

    @Nullable
    public static BuildingType fromKey(String key) {
        for (BuildingType type : values()) if (type.key.equals(key)) return type;
        return null;
    }

    /**
     * Type from the file name, without the biome prefix ("plains_wall_corner_2" -> WALL_CORNER).
     * The more specific keywords are checked first. Null = decide by content.
     */
    @Nullable
    public static BuildingType fromFileName(String name) {
        String n = name.toLowerCase(java.util.Locale.ROOT);
        if (n.contains("gate_extended") || n.contains("gate_ext")) return GATE_EXTENDED;
        if (n.contains("gate")) return GATE_BASIC;
        if (n.contains("wall_corner") || n.contains("corner")) return WALL_CORNER;
        if (n.contains("wall_straight") || n.contains("wall_chunk")) return WALL_STRAIGHT;
        if (n.contains("wall_basic") || n.contains("wall")) return WALL_BASIC;
        if (n.contains("road") || n.contains("path")) return ROAD;
        if (n.contains("villager_house")) return VILLAGER_HOUSE;
        if (n.contains("worker_house")) return WORKER_HOUSE;
        return null;
    }
}
