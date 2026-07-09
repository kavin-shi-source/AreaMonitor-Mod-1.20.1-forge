package com.kavinshi.areamonitor.util;

import com.kavinshi.areamonitor.LocalizationManager;
import net.minecraft.resources.ResourceLocation;

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

    public static final int TICKS_PER_DAY = 24000;
    
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
     * Check if dimension ID is valid using Minecraft's built-in parser.
     * This ensures strict validation of namespace:path format.
     * 
     * @param dimensionId Dimension ID
     * @return Whether the dimension is a valid ResourceLocation
     */
    public static boolean isValidDimension(String dimensionId) {
        if (dimensionId == null || dimensionId.isEmpty() || !dimensionId.contains(":")) {
            return false;
        }
        try {
            new ResourceLocation(dimensionId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String shortDim(String dim) {
        return switch (dim) {
            case OVERWORLD -> "overworld";
            case THE_NETHER -> "nether";
            case THE_END -> "end";
            default -> dim;
        };
    }

    public static boolean isInTimeRange(long time, int min, int max) {
        if (min <= max) {
            return time >= min && time <= max;
        }
        return time >= min || time <= max;
    }
}
