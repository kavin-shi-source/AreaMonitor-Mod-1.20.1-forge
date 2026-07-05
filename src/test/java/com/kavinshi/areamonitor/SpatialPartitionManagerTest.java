package com.kavinshi.areamonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SpatialPartitionManagerTest {

    private SpatialPartitionManager spm;

    @BeforeEach
    void setUp() {
        spm = new SpatialPartitionManager();
    }

    @Test
    void testAddRegion() {
        MonitorArea area = new MonitorArea("test_zone");
        area.setBounds(new MonitorArea.RectangleBounds(0, 0, 100, 100));
        spm.addRegion(area);

        assertEquals(1, spm.getRegionCount());
    }

    @Test
    void testRemoveRegion() {
        MonitorArea area = new MonitorArea("test_zone");
        area.setBounds(new MonitorArea.RectangleBounds(0, 0, 100, 100));
        spm.addRegion(area);
        assertEquals(1, spm.getRegionCount());

        spm.removeRegion("test_zone");
        assertEquals(0, spm.getRegionCount());
    }

    @Test
    void testRemoveNonExistentRegion() {
        // Should not throw
        spm.removeRegion("non_existent");
        assertEquals(0, spm.getRegionCount());
    }

    @Test
    void testGetPotentialRegions_InsideRegion() {
        MonitorArea area = new MonitorArea("test_zone");
        area.setBounds(new MonitorArea.RectangleBounds(0, 0, 200, 200));
        spm.addRegion(area);

        Set<MonitorArea> result = spm.getPotentialRegions(100, 100, "minecraft:overworld");
        assertEquals(1, result.size());
        assertTrue(result.contains(area));
    }

    @Test
    void testGetPotentialRegions_OutsideRegion() {
        MonitorArea area = new MonitorArea("test_zone");
        area.setBounds(new MonitorArea.RectangleBounds(0, 0, 200, 200));
        spm.addRegion(area);

        // Position 2000,2000 is in a different grid entirely
        Set<MonitorArea> result = spm.getPotentialRegions(2000, 2000, "minecraft:overworld");
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetPotentialRegions_DifferentDimension() {
        MonitorArea area = new MonitorArea("test_zone");
        area.setBounds(new MonitorArea.RectangleBounds(0, 0, 200, 200));
        spm.addRegion(area);

        Set<MonitorArea> result = spm.getPotentialRegions(100, 100, "minecraft:the_nether");
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetPotentialRegions_DisabledRegion() {
        MonitorArea area = new MonitorArea("test_zone");
        area.setBounds(new MonitorArea.RectangleBounds(0, 0, 200, 200));
        area.setEnabled(false);
        spm.addRegion(area);

        // Note: spatial partition returns all regions (enabled/disabled),
        // filtering is handled by AreaManager.checkPlayer
        Set<MonitorArea> result = spm.getPotentialRegions(100, 100, "minecraft:overworld");
        // The partition returns the region even if disabled; AreaManager handles filtering
        assertFalse(result.isEmpty());
    }

    @Test
    void testMultipleRegions() {
        MonitorArea area1 = new MonitorArea("zone_a");
        area1.setBounds(new MonitorArea.RectangleBounds(0, 0, 200, 200));

        MonitorArea area2 = new MonitorArea("zone_b");
        area2.setBounds(new MonitorArea.RectangleBounds(500, 500, 700, 700));

        spm.addRegion(area1);
        spm.addRegion(area2);

        assertEquals(2, spm.getRegionCount());

        Set<MonitorArea> result = spm.getPotentialRegions(100, 100, "minecraft:overworld");
        assertEquals(1, result.size());
        assertTrue(result.contains(area1));
    }

    @Test
    void testGetAllRegions() {
        spm.addRegion(createArea("a", 0, 0, 100, 100));
        spm.addRegion(createArea("b", 200, 200, 300, 300));

        Collection<MonitorArea> all = spm.getAllRegions();
        assertEquals(2, all.size());
    }

    @Test
    void testClear() {
        spm.addRegion(createArea("a", 0, 0, 100, 100));
        spm.addRegion(createArea("b", 200, 200, 300, 300));

        spm.clear();
        assertEquals(0, spm.getRegionCount());
        assertTrue(spm.getAllRegions().isEmpty());
    }

    @Test
    void testUpdateRegion() {
        MonitorArea area = createArea("move_zone", 0, 0, 100, 100);
        spm.addRegion(area);
        assertEquals(1, spm.getRegionCount());

        // Move to new location — re-add with same name; P2 #12 fix ensures the stale grid
        // entries are cleaned up before re-inserting.
        area.setBounds(new MonitorArea.RectangleBounds(500, 500, 600, 600));
        spm.addRegion(area);

        assertEquals(1, spm.getRegionCount());
        Set<MonitorArea> result = spm.getPotentialRegions(550, 550, "minecraft:overworld");
        assertEquals(1, result.size());
        // Old location should no longer return this region
        Set<MonitorArea> oldResult = spm.getPotentialRegions(50, 50, "minecraft:overworld");
        assertTrue(oldResult.isEmpty());
    }

    @Test
    void testCircleRegion() {
        MonitorArea area = new MonitorArea("circle_zone");
        area.setBounds(new MonitorArea.CircleBounds(500, 500, 100));
        spm.addRegion(area);

        Set<MonitorArea> result = spm.getPotentialRegions(500, 500, "minecraft:overworld");
        assertEquals(1, result.size());
    }

    private MonitorArea createArea(String name, int minX, int minZ, int maxX, int maxZ) {
        MonitorArea area = new MonitorArea(name);
        area.setBounds(new MonitorArea.RectangleBounds(minX, minZ, maxX, maxZ));
        return area;
    }
}
