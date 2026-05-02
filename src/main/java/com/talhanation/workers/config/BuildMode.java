package com.talhanation.workers.config;

public enum BuildMode {
    /** Client can use local scans and upload anything. Default. */
    FREE,
    /** Only server-side preset files can be used. */
    PRESET,
    /** Like PRESET but each team only sees files in a folder matching their team-string-id. */
    PRESET_FACTIONS
}
