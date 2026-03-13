package com.kavinshi.areamonitor;

import net.minecraft.world.level.GameType;

/**
 * Test utilities.
 */
public class TestUtils {

    /**
     * Create a test area.
     */
    public static MonitorArea createTestArea(String name, int minX, int minZ, int maxX, int maxZ) {
        MonitorArea area = new MonitorArea(name);
        area.setDisplayName("Test Area " + name);
        area.setDimension("minecraft:overworld");
        area.setEnterMode(GameType.ADVENTURE);
        area.setLeaveMode(GameType.SURVIVAL);
        area.setEnabled(true);
        area.setBounds(new MonitorArea.RectangleBounds(minX, minZ, maxX, maxZ));
        return area;
    }

    /**
     * Create a test area config.
     */
    public static ConfigManager.AreaConfig createTestAreaConfig(int minX, int minZ, int maxX, int maxZ) {
        ConfigManager.AreaConfig config = new ConfigManager.AreaConfig();
        config.displayName = "Test Config";
        config.dimension = "minecraft:overworld";
        config.minX = minX;
        config.minZ = minZ;
        config.maxX = maxX;
        config.maxZ = maxZ;
        config.enterMode = "adventure";
        config.leaveMode = "survival";
        config.enabled = true;
        config.whitelist = new java.util.ArrayList<>();
        return config;
    }

    /**
     * Clean up test environment.
     */
    public static void cleanupTestEnvironment() {
        AreaManager areaManager = AreaManager.getInstance();
        for (String areaName : areaManager.getAreaNames()) {
            areaManager.removeArea(areaName);
        }

        LocalizationManager.switchLanguage(LocalizationManager.LANGUAGE_ENGLISH);
    }
}
