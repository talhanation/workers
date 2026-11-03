package com.talhanation.workers.world.template;

import javax.annotation.Nullable;

/**
 * One scan file as the server sees it. Only the summary is kept, the NBT is read again when a building is started.
 *
 * @param path          path relative to the scan folder, with '/' ("vanilla/plains/plains_farmer.nbt")
 * @param style         biome style from the folder, null = fits every biome
 * @param faction       faction folder ("factions/&lt;team&gt;/..."), null = for everyone
 * @param type          detected type, null = not a governor building (ignored)
 * @param invalidReason translation key why the template does not fulfil its type, null = valid
 * @param beds          beds in the template (foot parts)
 * @param lastModified  file time, used to parse a file again only when it changed
 */
public record TemplateInfo(String path,
                           @Nullable BiomeStyle style,
                           @Nullable String faction,
                           @Nullable BuildingType type,
                           @Nullable String invalidReason,
                           int width, int height, int depth,
                           int beds,
                           long lastModified) {

    public boolean isValid() {
        return type != null && invalidReason == null;
    }

    /** File name without folder and extension. */
    public String name() {
        String n = path.substring(path.lastIndexOf('/') + 1);
        return n.endsWith(".nbt") ? n.substring(0, n.length() - 4) : n;
    }
}
