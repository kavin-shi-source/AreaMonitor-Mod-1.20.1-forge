package com.kavinshi.areamonitor;

import net.minecraft.world.level.GameType;

/**
 * 测试工具类
 */
public class TestUtils {

    /**
     * 创建测试用的区域
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
     * 创建测试用的区域配置
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
     * 清理测试环境
     */
    public static void cleanupTestEnvironment() {
        AreaManager areaManager = AreaManager.getInstance();
        for (String areaName : areaManager.getAreaNames()) {
            areaManager.removeArea(areaName);
        }

        // 清理LocalizationManager缓存
        LocalizationManager.setLanguage(LocalizationManager.LANGUAGE_ENGLISH);
    }
}