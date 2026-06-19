package com.kavinshi.areamonitor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests - testing multiple components working together.
 */
public class IntegrationTest {

    @BeforeEach
    public void setUp() {
        TestUtils.cleanupTestEnvironment();
    }

    @AfterEach
    public void tearDown() {
        TestUtils.cleanupTestEnvironment();
    }

    @Test
    public void testAreaCreationAndLocalization() {
        LocalizationManager.switchLanguage(LocalizationManager.LANGUAGE_CHINESE);

        MonitorArea area = TestUtils.createTestArea("integration_test", 0, 0, 10, 10);
        AreaManager areaManager = AreaManager.getInstance();
        areaManager.addArea(area);

        MonitorArea retrievedArea = areaManager.getArea("integration_test");
        assertNotNull(retrievedArea, "Area should be created successfully");
        assertEquals("integration_test", retrievedArea.getName(), "Area name should match");

        String gameModeName = LocalizationManager.getGameModeDisplayName(retrievedArea.getEnterMode());
        assertNotNull(gameModeName, "Game mode name should be localized");
        assertFalse(gameModeName.isEmpty(), "Game mode name should not be empty");
    }

    @Test
    public void testConfigToAreaConversion() {
        ConfigManager.AreaConfig config = TestUtils.createTestAreaConfig(0, 0, 20, 20);

        AreaManager areaManager = AreaManager.getInstance();

        MonitorArea area = TestUtils.createTestArea("converted_area", 0, 0, 20, 20);
        area.setDisplayName("Test Config");
        area.setDimension("minecraft:overworld");

        areaManager.addArea(area);

        MonitorArea retrievedArea = areaManager.getArea("converted_area");
        assertNotNull(retrievedArea, "Area should be created successfully");
        assertEquals("converted_area", retrievedArea.getName(), "Area name should match");
        assertEquals("Test Config", retrievedArea.getDisplayName(), "Display name should match");
        assertEquals("minecraft:overworld", retrievedArea.getDimension(), "Dimension should match");

        assertEquals(net.minecraft.world.level.GameType.ADVENTURE, retrievedArea.getEnterMode(), "Enter mode should be set correctly");
        assertEquals(net.minecraft.world.level.GameType.SURVIVAL, retrievedArea.getLeaveMode(), "Leave mode should be set correctly");
    }

    @Test
    public void testLanguageSwitching() {
        String initialLanguage = LocalizationManager.getCurrentLanguage();
        assertEquals(LocalizationManager.LANGUAGE_ENGLISH, initialLanguage, "Initial language should be English");

        LocalizationManager.switchLanguage(LocalizationManager.LANGUAGE_CHINESE);

        String currentLanguage = LocalizationManager.getCurrentLanguage();
        assertEquals(LocalizationManager.LANGUAGE_CHINESE, currentLanguage, "Current language should be Chinese");

        LocalizationManager.switchLanguage(LocalizationManager.LANGUAGE_ENGLISH);

        currentLanguage = LocalizationManager.getCurrentLanguage();
        assertEquals(LocalizationManager.LANGUAGE_ENGLISH, currentLanguage, "Current language should be English");
    }

    @Test
    public void testAreaManagerSingleton() {
        AreaManager instance1 = AreaManager.getInstance();
        AreaManager instance2 = AreaManager.getInstance();

        assertSame(instance1, instance2, "AreaManager should maintain singleton pattern");

        MonitorArea area = TestUtils.createTestArea("singleton_test", 0, 0, 5, 5);
        instance1.addArea(area);

        MonitorArea retrievedArea = instance2.getArea("singleton_test");
        assertNotNull(retrievedArea, "Both instances should share the same data");
        assertEquals("singleton_test", retrievedArea.getName(), "Area should be accessible from both instances");
    }
}
