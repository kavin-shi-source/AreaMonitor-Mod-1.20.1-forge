package com.kavinshi.areamonitor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 多语言管理器
 * 负责加载和管理模组的语言文件
 */
public class LocalizationManager {
    // 使用静态内部类实现线程安全的单例模式
    private static class InstanceHolder {
        private static final LocalizationManager INSTANCE = new LocalizationManager();
    }

    private final Map<String, String> translations = new HashMap<>();
    private final Map<String, String> formattedCache = new HashMap<>(); // Formatted result cache
    private final Gson gson = new Gson();
    private String currentLanguage = DEFAULT_LANGUAGE;

    /**
     * 支持的语言列表
     */
    public static final String LANGUAGE_ENGLISH = "en_us";
    public static final String LANGUAGE_CHINESE = "zh_cn";

    /**
     * 默认语言设置为英文
     */
    private static final String DEFAULT_LANGUAGE = LANGUAGE_ENGLISH;

    private static final String MOD_ID = "areamonitor";
    private static final String[] SUPPORTED_LANGUAGES = {"en_us", "zh_cn"};

    public static LocalizationManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private LocalizationManager() {
        // 默认使用英文
        currentLanguage = DEFAULT_LANGUAGE;
        loadLanguage();
    }

    /**
     * 加载语言文件
     */
    private void loadLanguage() {
        translations.clear();

        // 不再自动检测语言，使用当前设置的语言
        AreaMonitorMod.LOGGER.info("LocalizationManager: Loading language: {}", currentLanguage);

        // 加载对应语言文件
        String resourcePath = String.format("assets/%s/lang/%s.json", MOD_ID, currentLanguage);

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream != null) {
                JsonObject jsonObject = JsonParser.parseReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8)
                ).getAsJsonObject();

                // 解析JSON到翻译映射
                parseJsonObject(jsonObject, "");
                AreaMonitorMod.LOGGER.info("LocalizationManager: Successfully loaded language file: {}", currentLanguage);
            } else {
                AreaMonitorMod.LOGGER.warn("LocalizationManager: Language file not found: {}, using default English", resourcePath);
                loadDefaultTranslations();
            }
        } catch (Exception e) {
            AreaMonitorMod.LOGGER.error("LocalizationManager: Failed to load language file: {}", resourcePath, e);
            loadDefaultTranslations();
        }
    }

    /**
     * 递归解析JSON对象
     */
    private void parseJsonObject(JsonObject jsonObject, String prefix) {
        for (Map.Entry<String, com.google.gson.JsonElement> entry : jsonObject.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();

            if (entry.getValue().isJsonObject()) {
                parseJsonObject(entry.getValue().getAsJsonObject(), key);
            } else if (entry.getValue().isJsonPrimitive()) {
                translations.put(key, entry.getValue().getAsString());
            }
        }
    }

    /**
     * 加载默认翻译（英文）
     */
    private void loadDefaultTranslations() {
        // 游戏模式相关
        translations.put("gameMode.survival", "Survival Mode");
        translations.put("gameMode.creative", "Creative Mode");
        translations.put("gameMode.adventure", "Adventure Mode");
        translations.put("gameMode.spectator", "Spectator Mode");

        // 区域相关
        translations.put("area.enter", "§aEnter");
        translations.put("area.leave", "§cLeave");
        translations.put("area.message", "§7%s area: %s | %s");
        translations.put("area.gamemode_changed", "§aSwitched to %s");

        // 命令相关
        translations.put("command.areamonitor.toggle.enabled", "§6Area monitoring §aenabled§6, use /areamonitor status to check status");
        translations.put("command.areamonitor.toggle.disabled", "§6Area monitoring §cdisabled§6, use /areamonitor toggle to re-enable");
        translations.put("command.areamonitor.area.create.success", "§aArea '%s' created successfully");
        translations.put("command.areamonitor.area.create.exists", "§cArea '%s' already exists");
        translations.put("command.areamonitor.area.delete.success", "§aArea '%s' deleted");
        translations.put("command.areamonitor.area.delete.notfound", "§cArea '%s' not found");
        translations.put("command.areamonitor.area.list.empty", "§eNo configured areas");
        translations.put("command.areamonitor.area.list.header", "§6=== Area List ===");
        translations.put("command.areamonitor.area.toggle.enabled", "§6Area '%s' §aenabled");
        translations.put("command.areamonitor.area.toggle.disabled", "§6Area '%s' §cdisabled");
        translations.put("command.areamonitor.status.header", "§6=== Area Monitor Status ===");
        translations.put("command.areamonitor.status.enabled", "§aEnabled");
        translations.put("command.areamonitor.status.disabled", "§cDisabled");
        translations.put("command.areamonitor.status.areas_count", "§6Areas: §f%d");
        translations.put("command.areamonitor.status.players_count", "§6Players being monitored: §f%d");
        translations.put("command.areamonitor.reload.success", "§aConfiguration reloaded successfully");
        translations.put("command.areamonitor.reload.error", "§cFailed to reload configuration: %s");
        translations.put("command.areamonitor.save.success", "§aConfiguration saved successfully");
        translations.put("command.areamonitor.save.error", "§cFailed to save configuration: %s");

        // 错误相关
        translations.put("error.invalid_gamemode", "§cInvalid game mode: %s");
        translations.put("error.area_not_found", "§cArea not found: %s");
        translations.put("error.invalid_coordinates", "§cInvalid coordinates");
        translations.put("error.no_permission", "§cYou don't have permission to use this command");
    }

    /**
     * 获取翻译文本
     */
    public static String translate(String key, Object... args) {
        String template = InstanceHolder.INSTANCE.translations.getOrDefault(key, key);

        // 如果没有参数，直接返回模板
        if (args.length == 0) {
            return template;
        }

        // 创建缓存键
        String cacheKey = key + Arrays.toString(args);
        String cachedResult = InstanceHolder.INSTANCE.formattedCache.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        try {
            String result = String.format(template, args);
            // 缓存结果（限制缓存大小）
            if (InstanceHolder.INSTANCE.formattedCache.size() < 100) {
                InstanceHolder.INSTANCE.formattedCache.put(cacheKey, result);
            }
            return result;
        } catch (Exception e) {
            // 如果出现格式化错误，记录错误并返回未格式化的模板
            AreaMonitorMod.LOGGER.error("Localization format error for key '{}': {}", key, e.getMessage());
            AreaMonitorMod.LOGGER.error("Template: '{}', Args: {}", template, java.util.Arrays.toString(args));
            return template; // 返回未格式化的模板作为降级方案
        }
    }

    /**
     * 获取翻译文本并转换为Component
     */
    public static MutableComponent translateComponent(String key, Object... args) {
        return Component.literal(translate(key, args));
    }

    /**
     * 获取游戏模式显示名称
     */
    public static String getGameModeDisplayName(net.minecraft.world.level.GameType gameMode) {
        return switch (gameMode) {
            case CREATIVE -> translate("gameMode.creative");
            case ADVENTURE -> translate("gameMode.adventure");
            case SPECTATOR -> translate("gameMode.spectator");
            default -> translate("gameMode.survival");
        };
    }

    /**
     * 重新加载语言文件
     */
    public static void reload() {
        InstanceHolder.INSTANCE.loadLanguage();
    }

    /**
     * 获取Minecraft客户端语言设置
     */
    private String getMinecraftLanguage() {
        try {
            // 在客户端环境中尝试获取Minecraft语言设置
            if (net.minecraftforge.fml.loading.FMLLoader.getDist().isClient()) {
                // 使用反射来避免在服务器端编译错误
                Class<?> languageClass = Class.forName("net.minecraft.locale.Language");
                java.lang.reflect.Method getInstanceMethod = languageClass.getMethod("getInstance");
                Object languageInstance = getInstanceMethod.invoke(null);
                java.lang.reflect.Method getCodeMethod = languageInstance.getClass().getMethod("getCode");
                return (String) getCodeMethod.invoke(languageInstance);
            }
        } catch (Exception e) {
            // 忽略错误，使用备选方案
        }
        return null;
    }

    /**
     * 获取当前语言代码
     */
    public static String getCurrentLanguage() {
        return InstanceHolder.INSTANCE.currentLanguage;
    }

    /**
     * 切换语言
     */
    public static boolean setLanguage(String languageCode) {
        if (LANGUAGE_ENGLISH.equals(languageCode) || LANGUAGE_CHINESE.equals(languageCode)) {
            InstanceHolder.INSTANCE.currentLanguage = languageCode;
            InstanceHolder.INSTANCE.loadLanguage(); // 重新加载对应语言文件
            AreaMonitorMod.LOGGER.info("LocalizationManager: Language switched to: {}", languageCode);
            return true;
        }
        AreaMonitorMod.LOGGER.warn("LocalizationManager: Unsupported language: {}", languageCode);
        return false;
    }

    /**
     * 获取支持的语言列表
     */
    public static String[] getSupportedLanguages() {
        return new String[]{LANGUAGE_ENGLISH, LANGUAGE_CHINESE};
    }

    /**
     * 获取语言显示名称
     */
    public static String getLanguageDisplayName(String languageCode) {
        return switch (languageCode) {
            case LANGUAGE_ENGLISH -> "English";
            case LANGUAGE_CHINESE -> "中文";
            default -> "Unknown";
        };
    }

    /**
     * 测试语言检测（用于调试）
     */
    public static void testLanguageDetection() {
        AreaMonitorMod.LOGGER.info("=== LocalizationManager Test ===");
        AreaMonitorMod.LOGGER.info("Current language: {}", getCurrentLanguage());
        AreaMonitorMod.LOGGER.info("Test translation: {}", translate("gameMode.adventure"));
        AreaMonitorMod.LOGGER.info("Test translation: {}", translate("area.enter"));
        AreaMonitorMod.LOGGER.info("Test translation: {}", translate("area.message", "Enter", "Test Area", "Adventure Mode"));
        AreaMonitorMod.LOGGER.info("==============================");
    }
}