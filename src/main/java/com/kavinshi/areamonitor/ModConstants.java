package com.kavinshi.areamonitor;

import java.util.Arrays;
import java.util.List;

/**
 * 模组常量定义
 */
public class ModConstants {
    /**
     * 游戏模式建议列表
     */
    public static final List<String> GAME_MODE_SUGGESTIONS = Arrays.asList(
        "survival", "creative", "adventure", "spectator"
    );

    /**
     * 维度建议列表
     */
    public static final List<String> DIMENSION_SUGGESTIONS = Arrays.asList(
        "minecraft:overworld",
        "minecraft:the_nether",
        "minecraft:the_end"
    );
}