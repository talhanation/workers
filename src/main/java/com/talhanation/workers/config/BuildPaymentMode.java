package com.talhanation.workers.config;

public enum BuildPaymentMode {
    /** The builder fetches the base materials from storage. Default. */
    MATERIALS,
    /** The player pays the building price in currency when pressing build, the builder needs no materials. */
    CURRENCY
}
