package com.kavinshi.areamonitor.util;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.nio.file.Path;

/**
 * Configuration path constants class.
 * 
 * <p>Centralizes all configuration file paths using FMLPaths for proper cross-platform support.</p>
 * 
 * @since 1.0.0
 */
public final class ConfigPaths {
    
    private ConfigPaths() {
    }
    
    private static final String MOD_CONFIG_DIR = "areamonitor";
    
    public static Path getConfigDirectoryPath() {
        return FMLPaths.CONFIGDIR.get().resolve(MOD_CONFIG_DIR);
    }
    
    public static Path getAreasConfigPath() {
        return getConfigDirectoryPath().resolve("areas.json");
    }
    
    public static Path getBlacklistConfigPath() {
        return getConfigDirectoryPath().resolve("blacklist.json");
    }
    
    public static Path getWhitelistConfigPath() {
        return getConfigDirectoryPath().resolve("whitelist.txt");
    }
    
    public static File getAreasConfigFile() {
        return getAreasConfigPath().toFile();
    }
    
    public static File getBlacklistConfigFile() {
        return getBlacklistConfigPath().toFile();
    }
    
    public static File getWhitelistConfigFile() {
        return getWhitelistConfigPath().toFile();
    }
    
    public static File getConfigDirectory() {
        return getConfigDirectoryPath().toFile();
    }
    
    public static boolean ensureConfigDirectoryExists() {
        File dir = getConfigDirectory();
        if (!dir.exists()) {
            return dir.mkdirs();
        }
        return true;
    }
}
