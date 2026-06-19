package com.kavinshi.areamonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * LocalizationManager unit tests.
 */
public class LocalizationManagerTest {

    @BeforeEach
    public void setUp() {
        LocalizationManager.getInstance();
    }

    @Test
    public void testEnglishTranslation() {
        LocalizationManager.switchLanguage(LocalizationManager.LANGUAGE_ENGLISH);

        String currentLang = LocalizationManager.getCurrentLanguage();
        assertEquals(LocalizationManager.LANGUAGE_ENGLISH, currentLang, "Current language should be English");

        String gameModeName = LocalizationManager.getGameModeDisplayName(net.minecraft.world.level.GameType.CREATIVE);
        assertNotNull(gameModeName, "Game mode name should not be null");
        assertTrue(gameModeName.contains("Creative") || gameModeName.contains("创造"), "Should return valid game mode name");
    }

    @Test
    public void testChineseTranslation() {
        LocalizationManager.switchLanguage(LocalizationManager.LANGUAGE_CHINESE);

        String currentLang = LocalizationManager.getCurrentLanguage();
        assertEquals(LocalizationManager.LANGUAGE_CHINESE, currentLang, "Current language should be Chinese");
    }

    @Test
    public void testTranslationWithArgs() {
        String result = LocalizationManager.translate("area.gamemode_changed", "Creative Mode");
        assertNotNull(result, "Translation result should not be null");
        assertTrue(result.contains("Creative Mode"), "Should contain the provided argument");
    }

    @Test
    public void testTranslationWithoutArgs() {
        String result = LocalizationManager.translate("gameMode.creative");
        assertNotNull(result, "Translation result should not be null");
        assertFalse(result.isEmpty(), "Translation result should not be empty");
    }

    @Test
    public void testInvalidTranslationKey() {
        String result = LocalizationManager.translate("invalid.key");
        assertEquals("invalid.key", result, "Should return the key itself for invalid keys");
    }

    @Test
    public void testLanguageDisplayName() {
        String englishName = LocalizationManager.getLanguageDisplayName(LocalizationManager.LANGUAGE_ENGLISH);
        assertEquals("English", englishName, "English display name should be 'English'");

        String chineseName = LocalizationManager.getLanguageDisplayName(LocalizationManager.LANGUAGE_CHINESE);
        assertEquals("中文", chineseName, "Chinese display name should be '中文'");

        String unknownName = LocalizationManager.getLanguageDisplayName("unknown");
        assertEquals("unknown", unknownName, "Unknown language should return the language code");
    }
}
