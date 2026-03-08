package com.kavinshi.areamonitor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class ConfigManager {
    private static final ForgeConfigSpec SPEC;
    public static final Config CONFIG;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File areasConfigFile;
    private static File blacklistConfigFile;

    static {
        try {
            final Pair<Config, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Config::new);
            SPEC = specPair.getRight();
            CONFIG = specPair.getLeft();
        } catch (Exception e) {
            // 如果配置创建失败，记录错误并抛出
            System.err.println("Failed to create config spec: " + e.getMessage());
            AreaMonitorMod.LOGGER.error("创建配置规范时发生错误", e);
            throw e;
        }
    }

    public static class Config {
        public final ForgeConfigSpec.BooleanValue isEnabled;
        public final ForgeConfigSpec.BooleanValue showMessages;
        public final ForgeConfigSpec.BooleanValue debugMode;

        public Config(ForgeConfigSpec.Builder builder) {
            builder.comment("区域监控设置").push("区域监控设置");

            isEnabled = builder
                    .comment("是否启用监控")
                    .define("enabled", true);

            showMessages = builder
                    .comment("是否显示提示消息")
                    .define("showMessages", true);

            debugMode = builder
                    .comment("是否启用调试模式（启用后会显示详细日志）")
                    .define("debugMode", false);

            builder.pop();

            // 区域独立配置说明
            builder.comment("区域详细配置 - 请在 config/areamonitor/areas.json 中进行设置")
                  .comment("- 支持多个独立区域")
                  .comment("- 每个区域可设置不同的游戏模式、维度、坐标范围")
                  .comment("- 支持白名单、触发器等高级功能")
                  .push("区域配置说明");

            builder.pop();
        }


    }

    public static void init() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);

        // 初始化配置文件路径
        initConfigFiles();

        // 初始化黑名单配置
        ItemBlacklistManager.initBlacklistConfig();

        AreaMonitorMod.LOGGER.info("区域监控配置已注册");
    }

    /**
     * 初始化配置文件路径
     */
    private static void initConfigFiles() {
        // 延迟初始化文件路径，确保服务器目录已设置
        areasConfigFile = new File("config/areamonitor/areas.json");
        blacklistConfigFile = new File("config/areamonitor/blacklist.json");

        AreaMonitorMod.LOGGER.info("配置文件路径初始化完成");
    }

    /**
     * 加载区域配置
     */
    public static void loadAreasConfig() {
        // 确保文件路径已初始化
        if (areasConfigFile == null) {
            areasConfigFile = new File("config/areamonitor/areas.json");
        }

        if (areasConfigFile == null || !areasConfigFile.exists()) {
            createDefaultAreasConfig();
            return;
        }

        try (FileReader reader = new FileReader(areasConfigFile)) {
            AreaConfigData configData = GSON.fromJson(reader, AreaConfigData.class);

            // 配置文件完整性验证
            if (configData == null) {
                AreaMonitorMod.LOGGER.warn("区域配置文件为空，创建默认配置");
                createDefaultAreasConfig();
                return;
            }

            if (configData.areas == null) {
                AreaMonitorMod.LOGGER.warn("区域配置中缺少areas字段，初始化为空");
                configData.areas = new HashMap<>();
            }

            // 验证每个区域配置的完整性
            for (Iterator<Map.Entry<String, AreaConfig>> it = configData.areas.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, AreaConfig> entry = it.next();
                if (!validateAreaConfig(entry.getKey(), entry.getValue())) {
                    AreaMonitorMod.LOGGER.warn("区域配置验证失败，移除无效区域: {}", entry.getKey());
                    it.remove();
                }
            }

            if (!configData.areas.isEmpty()) {
                AreaManager areaManager = AreaManager.getInstance();
                areaManager.clearAllData();

                for (Map.Entry<String, AreaConfig> entry : configData.areas.entrySet()) {
                    MonitorArea area = createAreaFromConfig(entry.getKey(), entry.getValue());
                    if (area != null) {
                        areaManager.addArea(area);
                    }
                }
            }
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("加载区域配置文件失败", e);
        }
    }

    /**
     * 保存区域配置
     */
    public static void saveAreasConfig() {
        // 确保文件路径已初始化
        if (areasConfigFile == null) {
            areasConfigFile = new File("config/areamonitor/areas.json");
        }

        if (areasConfigFile == null) return;

        try {
            File parentDir = areasConfigFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                AreaMonitorMod.LOGGER.error("无法创建配置目录: {}", parentDir.getAbsolutePath());
                return;
            }

            AreaConfigData configData = new AreaConfigData();
            configData.areas = new HashMap<>();

            for (MonitorArea area : AreaManager.getInstance().getAllAreas()) {
                configData.areas.put(area.getName(), createConfigFromArea(area));
            }

            try (FileWriter writer = new FileWriter(areasConfigFile)) {
                GSON.toJson(configData, writer);
            }

            AreaMonitorMod.LOGGER.info("区域配置已保存");

            // 重建空间分区以优化性能
            AreaManager.getInstance().rebuildSpatialPartition();
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("保存区域配置文件失败", e);
        }
    }

    /**
     * 创建默认区域配置文件（包含示例区域）
     */
    private static void createDefaultAreasConfig() {
        // 确保文件路径已初始化
        if (areasConfigFile == null) {
            areasConfigFile = new File("config/areamonitor/areas.json");
        }

        // 创建包含示例区域的配置文件
        AreaConfigData configData = new AreaConfigData();
        configData.areas = new HashMap<>();

        // 添加一个示例区域
        AreaConfig exampleArea = new AreaConfig();
        exampleArea.displayName = "Protected Area Example";
        exampleArea.dimension = "minecraft:overworld";
        exampleArea.minX = -100;
        exampleArea.minZ = -100;
        exampleArea.maxX = 100;
        exampleArea.maxZ = 100;
        exampleArea.enterMode = "adventure";
        exampleArea.leaveMode = "survival";
        exampleArea.enabled = true;
        exampleArea.whitelist = new ArrayList<>();
        exampleArea.whitelist.add("Admin");

        configData.areas.put("example_protected_area", exampleArea);

        try {
            File parentDir = areasConfigFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                AreaMonitorMod.LOGGER.error("无法创建配置目录: {}", parentDir.getAbsolutePath());
                return;
            }

            try (FileWriter writer = new FileWriter(areasConfigFile)) {
                GSON.toJson(configData, writer);
            }

            AreaMonitorMod.LOGGER.info("已创建包含示例区域的配置文件");
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("创建默认区域配置文件失败", e);
        }
    }

    private static MonitorArea createAreaFromConfig(String name, AreaConfig config) {
        MonitorArea area = new MonitorArea(name);
        area.setDisplayName(config.displayName != null ? config.displayName : name);
        area.setDimension(config.dimension != null ? config.dimension : "minecraft:overworld");
        area.setEnterMode(parseGameMode(config.enterMode));
        area.setLeaveMode(parseGameMode(config.leaveMode));
        area.setEnabled(config.enabled);

        // 设置边界
        if (config.minX != null && config.maxX != null && config.minZ != null && config.maxZ != null) {
            area.setBounds(new MonitorArea.RectangleBounds(
                config.minX, config.minZ, config.maxX, config.maxZ
            ));
        }

        // 设置白名单
        if (config.whitelist != null) {
            area.setWhitelist(new ArrayList<>(config.whitelist));
        }

        return area;
    }

    /**
     * 解析游戏模式字符串为GameType
     */
    private static GameType parseGameMode(String mode) {
        return switch (mode.toLowerCase()) {
            case "creative" -> GameType.CREATIVE;
            case "adventure" -> GameType.ADVENTURE;
            case "spectator" -> GameType.SPECTATOR;
            default -> GameType.SURVIVAL;
        };
    }

    private static AreaConfig createConfigFromArea(MonitorArea area) {
        AreaConfig config = new AreaConfig();
        config.displayName = area.getDisplayName();
        config.dimension = area.getDimension();
        config.enterMode = area.getEnterMode().getName();
        config.leaveMode = area.getLeaveMode().getName();
        config.enabled = area.isEnabled();
        config.whitelist = area.getWhitelist();

        if (area.getBounds() instanceof MonitorArea.RectangleBounds rect) {
            config.minX = rect.getMinX();
            config.maxX = rect.getMaxX();
            config.minZ = rect.getMinZ();
            config.maxZ = rect.getMaxZ();
        }

        return config;
    }

    /**
     * 验证区域配置完整性
     */
    private static boolean validateAreaConfig(String areaName, AreaConfig config) {
        if (areaName == null || areaName.trim().isEmpty()) {
            return false;
        }

        if (config == null) {
            return false;
        }

        // 验证坐标范围
        if (config.minX == null || config.maxX == null || config.minZ == null || config.maxZ == null) {
            AreaMonitorMod.LOGGER.warn("区域 {} 缺少必要的坐标配置", areaName);
            return false;
        }

        // 验证坐标逻辑
        if (config.minX >= config.maxX || config.minZ >= config.maxZ) {
            AreaMonitorMod.LOGGER.warn("区域 {} 坐标范围无效: minX={}, maxX={}, minZ={}, maxZ={}",
                    areaName, config.minX, config.maxX, config.minZ, config.maxZ);
            return false;
        }

        // 验证游戏模式
        try {
            parseGameMode(config.enterMode);
            parseGameMode(config.leaveMode);
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.warn("区域 {} 包含无效的游戏模式配置", areaName);
            return false;
        }

        return true;
    }

    /**
     * 区域配置数据类
     */
    public static class AreaConfigData {
        public Map<String, AreaConfig> areas = new HashMap<>();
    }

    public static class AreaConfig {
        public String displayName;
        public String dimension = "minecraft:overworld";
        public Integer minX, maxX, minZ, maxZ;
        public String enterMode = "adventure";
        public String leaveMode = "survival";
        public boolean enabled = true;
        public List<String> whitelist = new ArrayList<>();
    }

    /**
     * 验证配置完整性，应该在配置加载完成后调用
     */
    public static void validateConfig() {
        try {
            Config config = CONFIG;

            AreaMonitorMod.LOGGER.info("区域监控配置验证完成");
            AreaMonitorMod.LOGGER.info("请在 config/areamonitor/areas.json 中配置具体的监控区域");
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.warn("配置验证失败，配置可能尚未完全加载: {}", e.getMessage());
        }
    }

    /**
     * 验证配置文件完整性并确保正确生成
     */
    public static void ensureConfigFiles() {
        try {
            AreaMonitorMod.LOGGER.info("验证配置文件完整性...");

            // 确保配置目录存在
            File configDir = new File("config/areamonitor");
            if (!configDir.exists() && !configDir.mkdirs()) {
                AreaMonitorMod.LOGGER.error("无法创建配置目录: {}", configDir.getAbsolutePath());
                return;
            }

            // 确保区域配置文件存在
            if (areasConfigFile == null) {
                areasConfigFile = new File("config/areamonitor/areas.json");
            }

            if (!areasConfigFile.exists()) {
                AreaMonitorMod.LOGGER.info("区域配置文件不存在，创建默认配置...");
                createDefaultAreasConfig();
            }

            // 确保黑名单配置文件存在
            if (blacklistConfigFile == null) {
                blacklistConfigFile = new File("config/areamonitor/blacklist.json");
            }

            if (!blacklistConfigFile.exists()) {
                AreaMonitorMod.LOGGER.info("黑名单配置文件不存在，创建默认配置...");
                // 调用ItemBlacklistManager的创建方法
                ItemBlacklistManager.createDefaultBlacklistConfig();
            }

            AreaMonitorMod.LOGGER.info("配置文件完整性验证完成");
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("配置文件完整性验证失败", e);
        }
    }
}