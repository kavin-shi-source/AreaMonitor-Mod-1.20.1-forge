package com.kavinshi.areamonitor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic functionality tests - verify core mod functionality.
 */
public class BasicFunctionalityTest {

    @Test
    public void testBasicWorkflow() {
        AreaManager areaManager = AreaManager.getInstance();
        assertNotNull(areaManager, "AreaManager should be initialized");

        MonitorArea area = TestUtils.createTestArea("workflow_test", 0, 0, 100, 100);
        assertNotNull(area, "Area should be created");
        assertEquals("workflow_test", area.getName(), "Area name should match");

        areaManager.addArea(area);
        MonitorArea retrievedArea = areaManager.getArea("workflow_test");
        assertNotNull(retrievedArea, "Area should be added successfully");
        assertEquals(area, retrievedArea, "Retrieved area should be the same");

        String currentLanguage = LocalizationManager.getCurrentLanguage();
        assertNotNull(currentLanguage, "Current language should not be null");

        String translation = LocalizationManager.translate("gameMode.survival");
        assertNotNull(translation, "Translation should not be null");
        assertFalse(translation.isEmpty(), "Translation should not be empty");

        String formattedTranslation = LocalizationManager.translate("area.message", "Enter", "Test Area", "Survival Mode");
        assertNotNull(formattedTranslation, "Formatted translation should not be null");
        assertTrue(formattedTranslation.contains("Test Area"), "Formatted translation should contain area name");

        areaManager.removeArea("workflow_test");
        assertNull(areaManager.getArea("workflow_test"), "Area should be removed");
    }

    @Test
    public void testConfigurationWorkflow() {
        ConfigManager.AreaConfig config = TestUtils.createTestAreaConfig(10, 10, 50, 50);
        assertNotNull(config, "Config should be created");

        assertEquals(10, config.getMinX(), "minX should match");
        assertEquals(10, config.getMinZ(), "minZ should match");
        assertEquals(50, config.getMaxX(), "maxX should match");
        assertEquals(50, config.getMaxZ(), "maxZ should match");
        assertEquals("adventure", config.getEnterMode(), "enterMode should be adventure");
        assertEquals("survival", config.getLeaveMode(), "leaveMode should be survival");

        assertTrue(config.getMinX() < config.getMaxX(), "minX should be less than maxX");
        assertTrue(config.getMinZ() < config.getMaxZ(), "minZ should be less than maxZ");
    }

    @Test
    public void testLanguageWorkflow() {
        String initialLanguage = LocalizationManager.getCurrentLanguage();
        assertEquals(LocalizationManager.LANGUAGE_ENGLISH, initialLanguage, "Initial language should be English");

        LocalizationManager.switchLanguage(LocalizationManager.LANGUAGE_CHINESE);

        String chineseLanguage = LocalizationManager.getCurrentLanguage();
        assertEquals(LocalizationManager.LANGUAGE_CHINESE, chineseLanguage, "Current language should be Chinese");

        String chineseTranslation = LocalizationManager.translate("gameMode.creative");
        assertNotNull(chineseTranslation, "Chinese translation should not be null");

        LocalizationManager.switchLanguage(LocalizationManager.LANGUAGE_ENGLISH);

        String englishLanguage = LocalizationManager.getCurrentLanguage();
        assertEquals(LocalizationManager.LANGUAGE_ENGLISH, englishLanguage, "Current language should be English");
    }

    @Test
    public void testAreaManagerWorkflow() {
        AreaManager areaManager = AreaManager.getInstance();

        for (MonitorArea area : areaManager.getAllAreas()) {
            areaManager.removeArea(area.getName());
        }

        assertTrue(areaManager.getAllAreas().isEmpty(), "Initially should have no areas");

        MonitorArea area1 = TestUtils.createTestArea("area1", 0, 0, 10, 10);
        MonitorArea area2 = TestUtils.createTestArea("area2", 20, 20, 30, 30);

        areaManager.addArea(area1);
        areaManager.addArea(area2);

        assertEquals(2, areaManager.getAllAreas().size(), "Should have 2 areas");
        assertNotNull(areaManager.getArea("area1"), "Area1 should exist");
        assertNotNull(areaManager.getArea("area2"), "Area2 should exist");

        areaManager.removeArea("area1");
        assertEquals(1, areaManager.getAllAreas().size(), "Should have 1 area after removal");
        assertNull(areaManager.getArea("area1"), "Area1 should not exist after removal");
        assertNotNull(areaManager.getArea("area2"), "Area2 should still exist");

        areaManager.removeArea("area2");
        assertTrue(areaManager.getAllAreas().isEmpty(), "Should be empty after cleanup");
    }
}
