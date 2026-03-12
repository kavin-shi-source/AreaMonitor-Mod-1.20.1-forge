package com.kavinshi.areamonitor.util;

import com.kavinshi.areamonitor.LocalizationManager;

/**
 * Dimension utility class.
 * 
 * <p>Provides dimension-related constants and utility methods.</p>
 * 
 * @since 1.0.0
 */
public final class DimensionUtils {
    
    public static final String OVERWORLD = "minecraft:overworld";
    public static final String THE_NETHER = "minecraft:the_nether";
    public static final String THE_END = "minecraft:the_end";
    
    private DimensionUtils() {
    }
    
    /**
     * Get display name for dimension.
     * 
     * @param dimensionId Dimension ID
     * @return Localized display name
     */
    public static String getDisplayName(String dimensionId) {
        if (dimensionId == null) {
            return "";
        }
        return switch (dimensionId) {
            case OVERWORLD -> LocalizationManager.translate("dimension.minecraft.overworld");
            case THE_NETHER -> LocalizationManager.translate("dimension.minecraft.the_nether");
            case THE_END -> LocalizationManager.translate("dimension.minecraft.the_end");
            default -> dimensionId;
        };
    }
    
    /**
     * Check if dimension ID is valid.
     * 
     * @param dimensionId Dimension ID
     * @return Whether the dimension is valid
     */
    public static boolean isValidDimension(String dimensionId) {
        if (dimensionId == null) {
            return false;
        }
        return dimensionId.startsWith("minecraft:") || dimensionId.contains(":");
    }
}
