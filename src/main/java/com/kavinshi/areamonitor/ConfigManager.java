package com.kavinshi.areamonitor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.kavinshi.areamonitor.util.GameModeUtils;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class ConfigManager {
    private static final ForgeConfigSpec SPEC;
    public static final Config CONFIG;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File areasConfigFile;
    private static File blacklistConfigFile;

    static {
        ForgeConfigSpec tempSpec;
        Config tempConfig;
        try {
            final Pair<Config, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Config::new);
            tempSpec = specPair.getRight();
            tempConfig = specPair.getLeft();
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("Error creating config spec, using hardcoded fallback", e);
            ForgeConfigSpec.Builder fallbackBuilder = new ForgeConfigSpec.Builder();
            tempConfig = new Config(fallbackBuilder);
            tempSpec = fallbackBuilder.build();
        }
        SPEC = tempSpec;
        CONFIG = tempConfig;
    }

    public static class Config {
        public final ForgeConfigSpec.BooleanValue isEnabled;
        public final ForgeConfigSpec.BooleanValue showMessages;
        public final ForgeConfigSpec.BooleanValue debugMode;

        // Performance tuning
        public final ForgeConfigSpec.LongValue gameModeSwitchDelayMs;
        public final ForgeConfigSpec.LongValue optimizationCooldownMs;
        public final ForgeConfigSpec.DoubleValue particleSpacing;
        public final ForgeConfigSpec.ConfigValue<String> selectionToolItemId;

        public Config(ForgeConfigSpec.Builder builder) {
            builder.comment("Area Monitor Settings").push("area_monitor");

            isEnabled = builder
                    .comment("Enable monitoring")
                    .define("enabled", true);

            showMessages = builder
                    .comment("Show notification messages")
                    .define("showMessages", true);

            debugMode = builder
                    .comment("Enable debug mode (shows detailed logs)")
                    .define("debugMode", false);

            gameModeSwitchDelayMs = builder
                    .comment("Delay in milliseconds before applying game mode switch after entering/leaving an area")
                    .defineInRange("gameModeSwitchDelayMs", 1000L, 0L, 10000L);

            optimizationCooldownMs = builder
                    .comment("Minimum cooldown in milliseconds between performance optimization actions")
                    .defineInRange("optimizationCooldownMs", 30000L, 5000L, 300000L);

            particleSpacing = builder
                    .comment("Spacing between particles when visualizing area boundaries")
                    .defineInRange("particleSpacing", 1.0, 0.25, 5.0);

            selectionToolItemId = builder
                    .comment("Item ID used for the area selection tool (e.g., \"minecraft:wooden_axe\")")
                    .define("selectionToolItemId", "minecraft:wooden_axe");

            builder.pop();

            // Area configuration instructions
            builder.comment("Area Configuration - Configure in config/areamonitor/areas.json")
                  .comment("- Supports multiple independent areas")
                  .comment("- Each area can have different game modes, dimensions, coordinate ranges")
                  .comment("- Supports advanced features like whitelist, triggers, etc.")
                  .push("area_config_info");

            builder.pop();
        }
    }

    public static void init() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);

        // Initialize config file paths
        initConfigFiles();

        // Initialize blacklist config
        ItemBlacklistManager.initBlacklistConfig();

        AreaMonitorMod.LOGGER.info("Area monitor config registered");
    }

    /**
     * Initialize config file paths using FMLPaths
     */
    private static void initConfigFiles() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("areamonitor");
        areasConfigFile = configDir.resolve("areas.json").toFile();
        blacklistConfigFile = configDir.resolve("blacklist.json").toFile();

        AreaMonitorMod.LOGGER.info("Config file paths initialized: {}", configDir.toAbsolutePath());
    }

    /**
     * Get config directory path
     */
    private static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get().resolve("areamonitor");
    }

    /**
     * Load area configuration
     */
    public static void loadAreasConfig() {
        // Ensure file path is initialized
        if (areasConfigFile == null) {
            areasConfigFile = getConfigDir().resolve("areas.json").toFile();
        }

        if (areasConfigFile == null || !areasConfigFile.exists()) {
            createDefaultAreasConfig();
            return;
        }

        try (FileReader reader = new FileReader(areasConfigFile)) {
            AreaConfigData configData = GSON.fromJson(reader, AreaConfigData.class);

            // Config file integrity validation
            if (configData == null) {
                AreaMonitorMod.LOGGER.warn("Area config file is empty, creating default config");
                createDefaultAreasConfig();
                return;
            }

            if (configData.areas == null) {
                AreaMonitorMod.LOGGER.warn("Area config missing 'areas' field, initializing as empty");
                configData.areas = new HashMap<>();
            }

            // Validate each area config integrity
            for (Iterator<Map.Entry<String, AreaConfig>> it = configData.areas.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, AreaConfig> entry = it.next();
                if (!validateAreaConfig(entry.getKey(), entry.getValue())) {
                    AreaMonitorMod.LOGGER.warn("Area config validation failed, removing invalid area: {}", entry.getKey());
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
        } catch (FileNotFoundException e) {
            AreaMonitorMod.LOGGER.warn("Area config file not found: {}", areasConfigFile.getAbsolutePath());
            createDefaultAreasConfig();
        } catch (JsonSyntaxException e) {
            AreaMonitorMod.LOGGER.error("Area config JSON syntax error: {}", areasConfigFile.getAbsolutePath(), e);
        } catch (IOException e) {
            AreaMonitorMod.LOGGER.error("Failed to read area config: {}", areasConfigFile.getAbsolutePath(), e);
        }
    }

    /**
     * Save area configuration
     */
    public static void saveAreasConfig() {
        // Ensure file path is initialized
        if (areasConfigFile == null) {
            areasConfigFile = getConfigDir().resolve("areas.json").toFile();
        }

        if (areasConfigFile == null) return;

        try {
            File parentDir = areasConfigFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                AreaMonitorMod.LOGGER.error("Failed to create config directory: {}", parentDir.getAbsolutePath());
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

            AreaMonitorMod.LOGGER.info("Area config saved");

            // Rebuild spatial partition for performance optimization
            AreaManager.getInstance().rebuildSpatialPartition();
        } catch (IOException e) {
            AreaMonitorMod.LOGGER.error("Failed to save area config: {}", areasConfigFile.getAbsolutePath(), e);
        }
    }

    /**
     * Create default area config file (with example area)
     */
    private static void createDefaultAreasConfig() {
        // Ensure file path is initialized
        if (areasConfigFile == null) {
            areasConfigFile = getConfigDir().resolve("areas.json").toFile();
        }

        // Create config file with example area
        AreaConfigData configData = new AreaConfigData();
        configData.areas = new HashMap<>();

        // Add an example area
        AreaConfig exampleArea = new AreaConfig();
        exampleArea.setDisplayName("Protected Area Example");
        exampleArea.setDimension("minecraft:overworld");
        exampleArea.setMinX(-100);
        exampleArea.setMinZ(-100);
        exampleArea.setMaxX(100);
        exampleArea.setMaxZ(100);
        exampleArea.setEnterMode("adventure");
        exampleArea.setLeaveMode("survival");
        exampleArea.setEnabled(true);
        List<String> exampleWhitelist = new ArrayList<>();
        exampleWhitelist.add("Admin");
        exampleArea.setWhitelist(exampleWhitelist);

        configData.areas.put("example_protected_area", exampleArea);

        try {
            File parentDir = areasConfigFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                AreaMonitorMod.LOGGER.error("Failed to create config directory: {}", parentDir.getAbsolutePath());
                return;
            }

            try (FileWriter writer = new FileWriter(areasConfigFile)) {
                GSON.toJson(configData, writer);
            }

            AreaMonitorMod.LOGGER.info("Created config file with example area");
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("Failed to create default area config", e);
        }
    }

    private static MonitorArea createAreaFromConfig(String name, AreaConfig config) {
        MonitorArea area = new MonitorArea(name);
        area.setDisplayName(config.getDisplayName() != null ? config.getDisplayName() : name);
        area.setDimension(config.getDimension() != null ? config.getDimension() : "minecraft:overworld");
        area.setEnterMode(parseGameMode(config.getEnterMode()));
        area.setLeaveMode(parseGameMode(config.getLeaveMode()));
        area.setEnabled(config.isEnabled());

        // Set bounds based on boundsType
        String type = config.getBoundsType() != null ? config.getBoundsType() : "RECTANGLE";
        switch (type) {
            case "CIRCLE":
                if (config.getCenterX() != null && config.getCenterZ() != null && config.getRadius() != null) {
                    area.setBounds(new MonitorArea.CircleBounds(
                        config.getCenterX(), config.getCenterZ(), config.getRadius()));
                }
                break;
            case "POLYGON":
                if (config.getVertices() != null && config.getVertices().length >= 3) {
                    List<MonitorArea.Vec2i> vertexList = new ArrayList<>();
                    for (int[] v : config.getVertices()) {
                        if (v.length >= 2) vertexList.add(new MonitorArea.Vec2i(v[0], v[1]));
                    }
                    if (vertexList.size() >= 3) {
                        area.setBounds(new MonitorArea.PolygonBounds(vertexList));
                    }
                }
                break;
            default: // RECTANGLE — backward compatible
                if (config.getMinX() != null && config.getMaxX() != null && config.getMinZ() != null && config.getMaxZ() != null) {
                    area.setBounds(new MonitorArea.RectangleBounds(
                        config.getMinX(), config.getMinZ(), config.getMaxX(), config.getMaxZ()));
                }
                break;
        }

        // Set whitelist
        if (config.getWhitelist() != null) {
            area.setWhitelist(new ArrayList<>(config.getWhitelist()));
        }

        // Set protection settings
        if (config.getProtection() != null) {
            area.setProtection(config.getProtection());
        }

        // Set triggers
        if (config.getEnterTrigger() != null) {
            area.setEnterTrigger(config.getEnterTrigger());
        }
        if (config.getLeaveTrigger() != null) {
            area.setLeaveTrigger(config.getLeaveTrigger());
        }

        // Load schedule / condition / chain
        if (config.getScheduleEnabled() != null) area.setScheduleEnabled(config.getScheduleEnabled());
        if (config.getScheduleTimeMin() != null) area.setScheduleTimeMin(config.getScheduleTimeMin());
        if (config.getScheduleTimeMax() != null) area.setScheduleTimeMax(config.getScheduleTimeMax());
        if (config.getConditionEnabled() != null) area.setConditionEnabled(config.getConditionEnabled());
        if (config.getConditionMinPlayers() != null) area.setConditionMinPlayers(config.getConditionMinPlayers());
        if (config.getConditionRequirePlayer() != null) area.setConditionRequirePlayer(config.getConditionRequirePlayer());
        if (config.getChainNext() != null) area.setChainNext(config.getChainNext());
        if (config.getChainDelayTicks() != null) area.setChainDelayTicks(config.getChainDelayTicks());

        return area;
    }

    /**
     * Parse game mode string to GameType
     */
    private static GameType parseGameMode(String mode) {
        return GameModeUtils.fromName(mode);
    }

    private static AreaConfig createConfigFromArea(MonitorArea area) {
        AreaConfig config = new AreaConfig();
        config.setDisplayName(area.getDisplayName());
        config.setDimension(area.getDimension());
        config.setEnterMode(area.getEnterMode().getName());
        config.setLeaveMode(area.getLeaveMode().getName());
        config.setEnabled(area.isEnabled());
        config.setWhitelist(area.getWhitelist());

        if (area.getBounds() instanceof MonitorArea.RectangleBounds rect) {
            config.setBoundsType("RECTANGLE");
            config.setMinX(rect.getMinX());
            config.setMaxX(rect.getMaxX());
            config.setMinZ(rect.getMinZ());
            config.setMaxZ(rect.getMaxZ());
        } else if (area.getBounds() instanceof MonitorArea.CircleBounds circle) {
            config.setBoundsType("CIRCLE");
            config.setCenterX(circle.getCenterX());
            config.setCenterZ(circle.getCenterZ());
            config.setRadius(circle.getRadius());
        } else if (area.getBounds() instanceof MonitorArea.PolygonBounds poly) {
            config.setBoundsType("POLYGON");
            int[][] vertArray = new int[poly.getVertices().size()][2];
            for (int i = 0; i < poly.getVertices().size(); i++) {
                vertArray[i][0] = poly.getVertices().get(i).x();
                vertArray[i][1] = poly.getVertices().get(i).z();
            }
            config.setVertices(vertArray);
        }

        // Save protection settings
        config.setProtection(area.getProtection());

        // Save triggers
        if (area.getEnterTrigger() != null) {
            config.setEnterTrigger(area.getEnterTrigger());
        }
        if (area.getLeaveTrigger() != null) {
            config.setLeaveTrigger(area.getLeaveTrigger());
        }

        // Save schedule / condition / chain
        config.setScheduleEnabled(area.isScheduleEnabled());
        config.setScheduleTimeMin(area.getScheduleTimeMin());
        config.setScheduleTimeMax(area.getScheduleTimeMax());
        config.setConditionEnabled(area.isConditionEnabled());
        config.setConditionMinPlayers(area.getConditionMinPlayers());
        config.setConditionRequirePlayer(area.getConditionRequirePlayer());
        config.setChainNext(area.getChainNext());
        config.setChainDelayTicks(area.getChainDelayTicks());

        return config;
    }

    /**
     * Validate area config integrity with enhanced checks.
     */
    private static boolean validateAreaConfig(String areaName, AreaConfig config) {
        if (areaName == null || areaName.trim().isEmpty()) {
            AreaMonitorMod.LOGGER.warn("Area name is null or empty");
            return false;
        }

        // Validate area name length
        if (areaName.length() > 64) {
            AreaMonitorMod.LOGGER.warn("Area {} name is too long (max 64 characters)", areaName);
            return false;
        }

        if (config == null) {
            AreaMonitorMod.LOGGER.warn("Area {} config is null", areaName);
            return false;
        }

        // Validate coordinate range based on bounds type
        String boundsType = config.getBoundsType() != null ? config.getBoundsType() : "RECTANGLE";

        switch (boundsType) {
            case "CIRCLE":
                if (config.getCenterX() == null || config.getCenterZ() == null || config.getRadius() == null) {
                    AreaMonitorMod.LOGGER.warn("Area {} missing required circle config", areaName);
                    return false;
                }
                if (config.getRadius() <= 0) {
                    AreaMonitorMod.LOGGER.warn("Area {} has invalid radius: {}", areaName, config.getRadius());
                    return false;
                }
                break;

            case "POLYGON":
                if (config.getVertices() == null || config.getVertices().length < 3) {
                    AreaMonitorMod.LOGGER.warn("Area {} polygon requires at least 3 vertices", areaName);
                    return false;
                }
                if (config.getVertices().length > 32) {
                    AreaMonitorMod.LOGGER.warn("Area {} polygon has too many vertices: {}", areaName, config.getVertices().length);
                    return false;
                }
                break;

            default: // RECTANGLE — keep existing validation
                if (config.getMinX() == null || config.getMaxX() == null || config.getMinZ() == null || config.getMaxZ() == null) {
                    AreaMonitorMod.LOGGER.warn("Area {} missing required coordinate config", areaName);
                    return false;
                }

                if (config.getMinX() >= config.getMaxX() || config.getMinZ() >= config.getMaxZ()) {
                    AreaMonitorMod.LOGGER.warn("Area {} has invalid coordinate range: minX={}, maxX={}, minZ={}, maxZ={}",
                            areaName, config.getMinX(), config.getMaxX(), config.getMinZ(), config.getMaxZ());
                    return false;
                }

                int worldBorder = 29999984;
                if (Math.abs(config.getMinX()) > worldBorder || Math.abs(config.getMaxX()) > worldBorder ||
                    Math.abs(config.getMinZ()) > worldBorder || Math.abs(config.getMaxZ()) > worldBorder) {
                    AreaMonitorMod.LOGGER.warn("Area {} has coordinates outside world border (±{}): minX={}, maxX={}, minZ={}, maxZ={}",
                            areaName, worldBorder, config.getMinX(), config.getMaxX(), config.getMinZ(), config.getMaxZ());
                    return false;
                }

                long areaWidth = (long) config.getMaxX() - config.getMinX();
                long areaLength = (long) config.getMaxZ() - config.getMinZ();
                long areaSize = areaWidth * areaLength;
                long maxAreaSize = 10000000L;
                if (areaSize > maxAreaSize) {
                    AreaMonitorMod.LOGGER.warn("Area {} is too large ({}x{} = {} blocks, max {}). This may cause performance issues.",
                            areaName, areaWidth, areaLength, areaSize, maxAreaSize);
                }
                break;
        }

        // Validate game mode
        try {
            parseGameMode(config.getEnterMode());
            parseGameMode(config.getLeaveMode());
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.warn("Area {} contains invalid game mode config: enterMode={}, leaveMode={}",
                    areaName, config.getEnterMode(), config.getLeaveMode());
            return false;
        }

        // Validate dimension
        if (config.getDimension() != null && !config.getDimension().isEmpty()) {
            if (!config.getDimension().contains(":")) {
                AreaMonitorMod.LOGGER.warn("Area {} has invalid dimension format: {} (should be namespace:path)",
                        areaName, config.getDimension());
                return false;
            }
        }

        // Validate whitelist
        if (config.getWhitelist() != null && config.getWhitelist().size() > 100) {
            AreaMonitorMod.LOGGER.warn("Area {} has too many whitelisted players ({}), this may impact performance",
                    areaName, config.getWhitelist().size());
        }

        return true;
    }

    /**
     * Area config data class
     */
    public static class AreaConfigData {
        public Map<String, AreaConfig> areas = new HashMap<>();
    }

    public static class AreaConfig {
        private String displayName;
        private String dimension = "minecraft:overworld";
        private Integer minX, maxX, minZ, maxZ;
        private String enterMode = "adventure";
        private String leaveMode = "survival";
        private boolean enabled = true;
        private List<String> whitelist = new ArrayList<>();
        private ProtectionSettings protection;
        private TriggerConfig enterTrigger;
        private TriggerConfig leaveTrigger;
        private String boundsType = "RECTANGLE";
        private int[][] vertices;
        private Integer centerX, centerZ, radius;
        // Schedule / Condition / Chain
        private Boolean scheduleEnabled;
        private Integer scheduleTimeMin, scheduleTimeMax;
        private Boolean conditionEnabled;
        private Integer conditionMinPlayers;
        private String conditionRequirePlayer;
        private String chainNext;
        private Integer chainDelayTicks;

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String v) { this.displayName = v; }
        public String getDimension() { return dimension; }
        public void setDimension(String v) { this.dimension = v; }
        public Integer getMinX() { return minX; }
        public void setMinX(Integer v) { this.minX = v; }
        public Integer getMaxX() { return maxX; }
        public void setMaxX(Integer v) { this.maxX = v; }
        public Integer getMinZ() { return minZ; }
        public void setMinZ(Integer v) { this.minZ = v; }
        public Integer getMaxZ() { return maxZ; }
        public void setMaxZ(Integer v) { this.maxZ = v; }
        public String getEnterMode() { return enterMode; }
        public void setEnterMode(String v) { this.enterMode = v; }
        public String getLeaveMode() { return leaveMode; }
        public void setLeaveMode(String v) { this.leaveMode = v; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public List<String> getWhitelist() { return whitelist; }
        public void setWhitelist(List<String> v) { this.whitelist = v != null ? v : new ArrayList<>(); }
        public ProtectionSettings getProtection() { return protection; }
        public void setProtection(ProtectionSettings v) { this.protection = v; }
        public TriggerConfig getEnterTrigger() { return enterTrigger; }
        public void setEnterTrigger(TriggerConfig v) { this.enterTrigger = v; }
        public TriggerConfig getLeaveTrigger() { return leaveTrigger; }
        public void setLeaveTrigger(TriggerConfig v) { this.leaveTrigger = v; }
        public String getBoundsType() { return boundsType; }
        public void setBoundsType(String v) { this.boundsType = v; }
        public int[][] getVertices() { return vertices; }
        public void setVertices(int[][] v) { this.vertices = v; }
        public Integer getCenterX() { return centerX; }
        public void setCenterX(Integer v) { this.centerX = v; }
        public Integer getCenterZ() { return centerZ; }
        public void setCenterZ(Integer v) { this.centerZ = v; }
        public Integer getRadius() { return radius; }
        public void setRadius(Integer v) { this.radius = v; }

        public Boolean getScheduleEnabled() { return scheduleEnabled; }
        public void setScheduleEnabled(Boolean v) { this.scheduleEnabled = v; }
        public Integer getScheduleTimeMin() { return scheduleTimeMin; }
        public void setScheduleTimeMin(Integer v) { this.scheduleTimeMin = v; }
        public Integer getScheduleTimeMax() { return scheduleTimeMax; }
        public void setScheduleTimeMax(Integer v) { this.scheduleTimeMax = v; }
        public Boolean getConditionEnabled() { return conditionEnabled; }
        public void setConditionEnabled(Boolean v) { this.conditionEnabled = v; }
        public Integer getConditionMinPlayers() { return conditionMinPlayers; }
        public void setConditionMinPlayers(Integer v) { this.conditionMinPlayers = v; }
        public String getConditionRequirePlayer() { return conditionRequirePlayer; }
        public void setConditionRequirePlayer(String v) { this.conditionRequirePlayer = v; }
        public String getChainNext() { return chainNext; }
        public void setChainNext(String v) { this.chainNext = v; }
        public Integer getChainDelayTicks() { return chainDelayTicks; }
        public void setChainDelayTicks(Integer v) { this.chainDelayTicks = v; }
    }

    /**
     * Validate config integrity, should be called after config is loaded
     */
    public static void validateConfig() {
        try {
            Config config = CONFIG;

            AreaMonitorMod.LOGGER.info("Area monitor config validation complete");
            AreaMonitorMod.LOGGER.info("Configure specific monitoring areas in config/areamonitor/areas.json");
        } catch (Exception e) {
            // Log full stack trace for config validation issues
            AreaMonitorMod.LOGGER.warn("Config validation failed, config may not be fully loaded", e);
        }
    }

    /**
     * Validate config file integrity and ensure proper generation
     */
    public static void ensureConfigFiles() {
        try {
            AreaMonitorMod.LOGGER.info("Validating config file integrity...");

            // Ensure config directory exists
            Path configDir = getConfigDir();
            File configDirFile = configDir.toFile();
            if (!configDirFile.exists() && !configDirFile.mkdirs()) {
                AreaMonitorMod.LOGGER.error("Failed to create config directory: {}", configDirFile.getAbsolutePath());
                return;
            }

            // Ensure area config file exists
            if (areasConfigFile == null) {
                areasConfigFile = configDir.resolve("areas.json").toFile();
            }

            if (!areasConfigFile.exists()) {
                AreaMonitorMod.LOGGER.info("Area config file not found, creating default config...");
                createDefaultAreasConfig();
            }

            // Ensure blacklist config file exists
            if (blacklistConfigFile == null) {
                blacklistConfigFile = configDir.resolve("blacklist.json").toFile();
            }

            if (!blacklistConfigFile.exists()) {
                AreaMonitorMod.LOGGER.info("Blacklist config file not found, creating default config...");
                // Call ItemBlacklistManager's create method
                ItemBlacklistManager.createDefaultBlacklistConfig();
            }

            AreaMonitorMod.LOGGER.info("Config file integrity validation complete");
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("Config file integrity validation failed", e);
        }
    }
}
