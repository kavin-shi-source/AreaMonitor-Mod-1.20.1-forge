package com.kavinshi.areamonitor;

import net.minecraft.world.level.GameType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

public class ConfigManager {
    private static final ForgeConfigSpec SPEC;
    public static final Config CONFIG;

    static {
        try {
            final Pair<Config, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Config::new);
            SPEC = specPair.getRight();
            CONFIG = specPair.getLeft();
        } catch (Exception e) {
            // 如果配置创建失败，记录错误并抛出
            System.err.println("Failed to create config spec: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static class Config {
        public final ForgeConfigSpec.BooleanValue isEnabled;
        public final ForgeConfigSpec.ConfigValue<String> targetDimension;
        public final ForgeConfigSpec.IntValue minX;
        public final ForgeConfigSpec.IntValue maxX;
        public final ForgeConfigSpec.IntValue minZ;
        public final ForgeConfigSpec.IntValue maxZ;
        public final ForgeConfigSpec.BooleanValue showMessages;
        public final ForgeConfigSpec.ConfigValue<String> enterGameMode;
        public final ForgeConfigSpec.ConfigValue<String> leaveGameMode;

        // 缓存边界值以提高性能
        private Integer cachedMinX, cachedMaxX, cachedMinZ, cachedMaxZ;

        public Config(ForgeConfigSpec.Builder builder) {
            builder.comment("区域监控设置").push("区域监控设置");

            isEnabled = builder
                    .comment("是否启用监控")
                    .define("enabled", true);

            targetDimension = builder
                    .comment("目标维度ID（必须使用完整命名空间格式，如：minecraft:overworld）")
                    .define("dimension", "minecraft:overworld");

            minX = builder
                    .comment("区域最小X坐标")
                    .defineInRange("minX", -100, Integer.MIN_VALUE, Integer.MAX_VALUE);

            maxX = builder
                    .comment("区域最大X坐标")
                    .defineInRange("maxX", 100, Integer.MIN_VALUE, Integer.MAX_VALUE);

            minZ = builder
                    .comment("区域最小Z坐标")
                    .defineInRange("minZ", -100, Integer.MIN_VALUE, Integer.MAX_VALUE);

            maxZ = builder
                    .comment("区域最大Z坐标")
                    .defineInRange("maxZ", 100, Integer.MIN_VALUE, Integer.MAX_VALUE);

            showMessages = builder
                    .comment("是否显示提示消息")
                    .define("showMessages", true);

            enterGameMode = builder
                    .comment("进入区域时的游戏模式 (survival, creative, adventure, spectator)")
                    .define("enterGameMode", "adventure");

            leaveGameMode = builder
                    .comment("离开区域时的游戏模式 (survival, creative, adventure, spectator)")
                    .define("leaveGameMode", "survival");

            builder.pop();
        }

        public boolean isInArea(int x, int z) {
            // 使用缓存的边界值
            if (cachedMinX == null || cachedMaxX == null || cachedMinZ == null || cachedMaxZ == null) {
                cachedMinX = Math.min(minX.get(), maxX.get());
                cachedMaxX = Math.max(minX.get(), maxX.get());
                cachedMinZ = Math.min(minZ.get(), maxZ.get());
                cachedMaxZ = Math.max(minZ.get(), maxZ.get());
            }
            return x >= cachedMinX && x <= cachedMaxX && z >= cachedMinZ && z <= cachedMaxZ;
        }

        /**
         * 清除缓存的边界值，在配置更改时调用
         */
        public void invalidateCache() {
            cachedMinX = null;
            cachedMaxX = null;
            cachedMinZ = null;
            cachedMaxZ = null;
        }

        public GameType getEnterGameMode() {
            return parseGameMode(enterGameMode.get());
        }

        public GameType getLeaveGameMode() {
            return parseGameMode(leaveGameMode.get());
        }

        private GameType parseGameMode(String mode) {
            return switch (mode.toLowerCase()) {
                case "creative" -> GameType.CREATIVE;
                case "adventure" -> GameType.ADVENTURE;
                case "spectator" -> GameType.SPECTATOR;
                default -> GameType.SURVIVAL;
            };
        }
    }

    public static void init() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);

        AreaMonitorMod.LOGGER.info("区域监控配置已注册");
    }

    /**
     * 验证配置完整性，应该在配置加载完成后调用
     */
    public static void validateConfig() {
        try {
            Config config = CONFIG;

            // 检查区域坐标逻辑
            if (config.minX.get() >= config.maxX.get()) {
                AreaMonitorMod.LOGGER.warn("监控区域X坐标配置异常: minX({}) >= maxX({})，区域将无效",
                    config.minX.get(), config.maxX.get());
            }

            if (config.minZ.get() >= config.maxZ.get()) {
                AreaMonitorMod.LOGGER.warn("监控区域Z坐标配置异常: minZ({}) >= maxZ({})，区域将无效",
                    config.minZ.get(), config.maxZ.get());
            }

            // 检查区域大小
            int area = Math.abs(config.maxX.get() - config.minX.get()) *
                       Math.abs(config.maxZ.get() - config.minZ.get());
            if (area > 1000000) {
                AreaMonitorMod.LOGGER.warn("监控区域过大 ({} 方块)，可能影响性能，建议减小区域大小", area);
            }

            // 验证游戏模式
            try {
                config.getEnterGameMode();
                config.getLeaveGameMode();
            } catch (Exception e) {
                AreaMonitorMod.LOGGER.error("游戏模式配置无效", e);
            }

            AreaMonitorMod.LOGGER.info("区域监控配置验证完成");
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.warn("配置验证失败，配置可能尚未完全加载: {}", e.getMessage());
        }
    }
}