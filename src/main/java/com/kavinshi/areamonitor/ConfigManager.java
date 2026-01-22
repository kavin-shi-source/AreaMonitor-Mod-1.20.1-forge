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
        final Pair<Config, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Config::new);
        SPEC = specPair.getRight();
        CONFIG = specPair.getLeft();
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
            int x1 = Math.min(minX.get(), maxX.get());
            int x2 = Math.max(minX.get(), maxX.get());
            int z1 = Math.min(minZ.get(), maxZ.get());
            int z2 = Math.max(minZ.get(), maxZ.get());
            return x >= x1 && x <= x2 && z >= z1 && z <= z2;
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
    }
}