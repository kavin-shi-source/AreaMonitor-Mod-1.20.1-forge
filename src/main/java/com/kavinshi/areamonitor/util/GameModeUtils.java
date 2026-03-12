package com.kavinshi.areamonitor.util;

import com.kavinshi.areamonitor.LocalizationManager;
import net.minecraft.world.level.GameType;

/**
 * Game mode utility class.
 * 
 * <p>Provides conversion methods between game mode strings and {@link GameType}.</p>
 * 
 * @since 1.0.0
 */
public final class GameModeUtils {
    
    private GameModeUtils() {
    }
    
    /**
     * Get the corresponding game mode from a name string.
     * 
     * @param name Game mode name (case-insensitive)
     * @return Corresponding GameType, returns SURVIVAL if unrecognized
     */
    public static GameType fromName(String name) {
        if (name == null) {
            return GameType.SURVIVAL;
        }
        return switch (name.toLowerCase()) {
            case "creative" -> GameType.CREATIVE;
            case "adventure" -> GameType.ADVENTURE;
            case "spectator" -> GameType.SPECTATOR;
            default -> GameType.SURVIVAL;
        };
    }
    
    /**
     * Convert game mode to name string.
     * 
     * @param type Game mode
     * @return Corresponding name string
     */
    public static String toName(GameType type) {
        if (type == null) {
            return "survival";
        }
        return switch (type) {
            case CREATIVE -> "creative";
            case ADVENTURE -> "adventure";
            case SPECTATOR -> "spectator";
            default -> "survival";
        };
    }
    
    /**
     * Get display name for game mode.
     * 
     * @param type Game mode
     * @return Localized display name
     */
    public static String getDisplayName(GameType type) {
        return LocalizationManager.getGameModeDisplayName(type);
    }
}
