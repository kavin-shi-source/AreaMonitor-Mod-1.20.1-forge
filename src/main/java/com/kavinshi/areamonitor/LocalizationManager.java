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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Localization manager for handling multi-language support.
 * Thread-safe implementation using ConcurrentHashMap.
 *
 * <p>Language source: the active language is read from
 * {@code config/areamonitor/common.toml} (field {@code language}) on server start
 * and on {@code /areamonitor config reload}. When a client has the mod installed,
 * the server sends {@link net.minecraft.network.chat.MutableComponent} via
 * {@link Component#translatable(String, Object...)} and the client resolves the
 * text using its own Minecraft language setting, ignoring the server-side language.</p>
 */
public class LocalizationManager {
    private static class InstanceHolder {
        private static final LocalizationManager INSTANCE = new LocalizationManager();
    }

    private final Map<String, String> translations = new ConcurrentHashMap<>();
    private final Map<String, String> formattedCache = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private volatile String currentLanguage = DEFAULT_LANGUAGE;

    /**
     * Supported languages.
     */
    public static final String LANGUAGE_ENGLISH = "en_us";
    public static final String LANGUAGE_CHINESE = "zh_cn";

    /**
     * Default language is English.
     */
    private static final String DEFAULT_LANGUAGE = LANGUAGE_ENGLISH;

    private static final String MOD_ID = "areamonitor";
    private static final String[] SUPPORTED_LANGUAGES = {"en_us", "zh_cn"};

    public static LocalizationManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private LocalizationManager() {
        currentLanguage = DEFAULT_LANGUAGE;
        loadLanguage();
    }

    /**
     * Load language file.
     */
    private void loadLanguage() {
        translations.clear();

        AreaMonitorMod.LOGGER.info("LocalizationManager: Loading language: {}", currentLanguage);

        String resourcePath = String.format("assets/%s/lang/%s.json", MOD_ID, currentLanguage);

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream != null) {
                JsonObject jsonObject = JsonParser.parseReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8)
                ).getAsJsonObject();

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
     * Recursively parse JSON object.
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
     * Load default translations (English).
     */
    private void loadDefaultTranslations() {
        translations.put("gameMode.survival", "Survival Mode");
        translations.put("gameMode.creative", "Creative Mode");
        translations.put("gameMode.adventure", "Adventure Mode");
        translations.put("gameMode.spectator", "Spectator Mode");

        translations.put("area.enter", "§aEnter");
        translations.put("area.leave", "§cLeave");
        translations.put("area.message", "§7%s area: %s | %s");
        translations.put("area.gamemode_changed", "§aSwitched to %s");

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

        translations.put("error.invalid_gamemode", "§cInvalid game mode: %s");
        translations.put("error.area_not_found", "§cArea not found: %s");
        translations.put("error.invalid_coordinates", "§cInvalid coordinates");
        translations.put("error.no_permission", "§cYou don't have permission to use this command");
    }

    /**
     * Get translated text.
     */
    public static String translate(String key, Object... args) {
        String template = InstanceHolder.INSTANCE.translations.getOrDefault(key, key);

        if (args.length == 0) {
            return template;
        }

        String cacheKey = key + Arrays.toString(args);
        String cachedResult = InstanceHolder.INSTANCE.formattedCache.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        try {
            String result = String.format(template, args);
            if (InstanceHolder.INSTANCE.formattedCache.size() < 100) {
                InstanceHolder.INSTANCE.formattedCache.put(cacheKey, result);
            }
            return result;
        } catch (Exception e) {
            // Log full stack trace for localization format errors
            AreaMonitorMod.LOGGER.error("Localization format error for key '{}'", key, e);
            AreaMonitorMod.LOGGER.error("Template: '{}', Args: {}", template, java.util.Arrays.toString(args));
            return template;
        }
    }

    /**
     * Get translated text and convert to Component.
     */
    public static MutableComponent translateComponent(String key, Object... args) {
        return Component.literal(translate(key, args));
    }

    /**
     * Get game mode display name.
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
     * Reload language file.
     */
    public static void reloadLanguage() {
        InstanceHolder.INSTANCE.loadLanguage();
    }

    /**
     * Get current language code.
     */
    public static String getCurrentLanguage() {
        return InstanceHolder.INSTANCE.currentLanguage;
    }

    /**
     * Switch language.
     */
    public static void switchLanguage(String languageCode) {
        if (Arrays.asList(SUPPORTED_LANGUAGES).contains(languageCode)) {
            InstanceHolder.INSTANCE.currentLanguage = languageCode;
            InstanceHolder.INSTANCE.loadLanguage();
        }
    }

    /**
     * Apply language from config file. Reloads translations only when the
     * language code changes. Called on server start and on config reload.
     */
    public static void applyConfigLanguage(String languageCode) {
        if (languageCode == null || !Arrays.asList(SUPPORTED_LANGUAGES).contains(languageCode)) {
            AreaMonitorMod.LOGGER.warn("Unsupported language code from config: {}, keeping current: {}",
                    languageCode, InstanceHolder.INSTANCE.currentLanguage);
            return;
        }
        if (!languageCode.equals(InstanceHolder.INSTANCE.currentLanguage)) {
            AreaMonitorMod.LOGGER.info("Applying language from config: {} -> {}",
                    InstanceHolder.INSTANCE.currentLanguage, languageCode);
            InstanceHolder.INSTANCE.currentLanguage = languageCode;
            InstanceHolder.INSTANCE.loadLanguage();
        }
    }

    /**
     * Get supported languages list.
     */
    public static String[] getSupportedLanguages() {
        return SUPPORTED_LANGUAGES.clone();
    }

    /**
     * Get language display name.
     */
    public static String getLanguageDisplayName(String languageCode) {
        return switch (languageCode) {
            case LANGUAGE_ENGLISH -> "English";
            case LANGUAGE_CHINESE -> "中文";
            default -> languageCode;
        };
    }

    /**
     * Test language detection (for debugging).
     */
    public static void testLanguageDetection() {
        AreaMonitorMod.LOGGER.info("=== LocalizationManager Test ===");
        AreaMonitorMod.LOGGER.info("Current language: {}", getCurrentLanguage());
        AreaMonitorMod.LOGGER.info("Test translation: {}", translate("gameMode.adventure"));
        AreaMonitorMod.LOGGER.info("Test translation: {}", translate("area.enter"));
    }
}
