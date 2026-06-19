package com.kavinshi.areamonitor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AreaManagerTest {

    private AreaManager areaManager;
    private final String testPrefix = "test_" + UUID.randomUUID().toString().substring(0, 8) + "_";

    @BeforeEach
    void setUp() {
        areaManager = AreaManager.getInstance();
    }

    @AfterEach
    void tearDown() {
        // Clean up areas added by this test instance
        for (String name : areaManager.getAreaNames()) {
            if (name.startsWith(testPrefix)) {
                areaManager.removeArea(name);
            }
        }
    }

    @Test
    void testAddArea() {
        String name = testPrefix + "add";
        MonitorArea area = new MonitorArea(name);
        area.setBounds(new MonitorArea.RectangleBounds(0, 0, 100, 100));
        areaManager.addArea(area);

        assertNotNull(areaManager.getArea(name));
        assertEquals(name, areaManager.getArea(name).getName());
    }

    @Test
    void testRemoveArea() {
        String name = testPrefix + "remove";
        MonitorArea area = new MonitorArea(name);
        areaManager.addArea(area);
        assertNotNull(areaManager.getArea(name));

        areaManager.removeArea(name);
        assertNull(areaManager.getArea(name));
    }

    @Test
    void testRemoveNonExistentArea() {
        areaManager.removeArea("non_existent_area_name");
    }

    @Test
    void testGetAreaNames_ContainsAddedAreas() {
        String a = testPrefix + "ga1";
        String b = testPrefix + "ga2";
        String c = testPrefix + "ga3";
        areaManager.addArea(new MonitorArea(a));
        areaManager.addArea(new MonitorArea(b));
        areaManager.addArea(new MonitorArea(c));

        Set<String> names = areaManager.getAreaNames();
        assertTrue(names.contains(a));
        assertTrue(names.contains(b));
        assertTrue(names.contains(c));
    }

    @Test
    void testGetAllAreas_ContainsAddedAreas() {
        String a = testPrefix + "all1";
        String b = testPrefix + "all2";
        areaManager.addArea(new MonitorArea(a));
        areaManager.addArea(new MonitorArea(b));

        assertTrue(areaManager.getAllAreas().size() >= 2);
    }

    @Test
    void testClearPlayerData() {
        UUID playerId = UUID.randomUUID();
        String name = testPrefix + "cpd";
        MonitorArea area = new MonitorArea(name);
        areaManager.addArea(area);

        // Verify clearing doesn't throw for non-existent player
        areaManager.clearPlayerData(playerId);

        // Verify area still exists after clearing unrelated player
        assertNotNull(areaManager.getArea(name));
    }

    @Test
    void testClearAllData_KeepsAreas() {
        String a = testPrefix + "cda1";
        String b = testPrefix + "cda2";
        areaManager.addArea(new MonitorArea(a));
        areaManager.addArea(new MonitorArea(b));

        areaManager.clearAllData();

        // clearAllData only clears player data, not areas
        assertNotNull(areaManager.getArea(a));
        assertNotNull(areaManager.getArea(b));
    }

    @Test
    void testRebuildSpatialPartition() {
        String name = testPrefix + "rsp";
        MonitorArea area = new MonitorArea(name);
        area.setBounds(new MonitorArea.RectangleBounds(0, 0, 100, 100));
        areaManager.addArea(area);

        areaManager.rebuildSpatialPartition();
        assertNotNull(areaManager.getArea(name));
    }

    @Test
    void testMultipleAreasWithSameName_Overwrites() {
        String name = testPrefix + "same";
        MonitorArea area1 = new MonitorArea(name);
        MonitorArea area2 = new MonitorArea(name);
        // Mark them differently to verify overwrite
        area2.setEnabled(false);
        areaManager.addArea(area1);
        areaManager.addArea(area2);

        // Second add should overwrite the first (enabled should be false)
        MonitorArea retrieved = areaManager.getArea(name);
        assertNotNull(retrieved);
        assertFalse(retrieved.isEnabled());
    }
}
