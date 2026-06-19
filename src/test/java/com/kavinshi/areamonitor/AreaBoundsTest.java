package com.kavinshi.areamonitor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AreaBoundsTest {

    // RectangleBounds tests

    @Test
    void testRectangleBounds_CenterPoint() {
        MonitorArea.RectangleBounds rect = new MonitorArea.RectangleBounds(0, 0, 100, 100);
        assertTrue(rect.contains(50, 50));
    }

    @Test
    void testRectangleBounds_CornerPoints() {
        MonitorArea.RectangleBounds rect = new MonitorArea.RectangleBounds(0, 0, 100, 100);
        assertTrue(rect.contains(0, 0));
        assertTrue(rect.contains(100, 100));
        assertTrue(rect.contains(0, 100));
        assertTrue(rect.contains(100, 0));
    }

    @Test
    void testRectangleBounds_Outside() {
        MonitorArea.RectangleBounds rect = new MonitorArea.RectangleBounds(0, 0, 100, 100);
        assertFalse(rect.contains(-1, 50));
        assertFalse(rect.contains(101, 50));
        assertFalse(rect.contains(50, -1));
        assertFalse(rect.contains(50, 101));
    }

    @Test
    void testRectangleBounds_SwappedMinMax() {
        // Constructor should handle swapped min/max automatically
        MonitorArea.RectangleBounds rect = new MonitorArea.RectangleBounds(100, 100, 0, 0);
        assertTrue(rect.contains(50, 50));
        assertEquals(0, rect.getMinX());
        assertEquals(0, rect.getMinZ());
        assertEquals(100, rect.getMaxX());
        assertEquals(100, rect.getMaxZ());
    }

    @Test
    void testRectangleBounds_NegativeCoordinates() {
        MonitorArea.RectangleBounds rect = new MonitorArea.RectangleBounds(-100, -100, -50, -50);
        assertTrue(rect.contains(-75, -75));
        assertFalse(rect.contains(0, 0));
    }

    @Test
    void testRectangleBounds_Type() {
        MonitorArea.RectangleBounds rect = new MonitorArea.RectangleBounds(0, 0, 100, 100);
        assertEquals(MonitorArea.BoundsType.RECTANGLE, rect.getType());
    }

    @Test
    void testRectangleBounds_BoundingBox() {
        MonitorArea.RectangleBounds rect = new MonitorArea.RectangleBounds(0, 0, 100, 100);
        net.minecraft.world.phys.AABB aabb = rect.getBoundingBox();
        assertTrue(aabb.minX <= aabb.maxX);
        assertTrue(aabb.minZ <= aabb.maxZ);
    }

    // CircleBounds tests

    @Test
    void testCircleBounds_CenterPoint() {
        MonitorArea.CircleBounds circle = new MonitorArea.CircleBounds(50, 50, 30);
        assertTrue(circle.contains(50, 50));
    }

    @Test
    void testCircleBounds_InsideRadius() {
        MonitorArea.CircleBounds circle = new MonitorArea.CircleBounds(50, 50, 30);
        assertTrue(circle.contains(70, 50));
        assertTrue(circle.contains(50, 70));
    }

    @Test
    void testCircleBounds_OnEdge() {
        MonitorArea.CircleBounds circle = new MonitorArea.CircleBounds(50, 50, 30);
        assertTrue(circle.contains(80, 50));
        assertTrue(circle.contains(50, 80));
    }

    @Test
    void testCircleBounds_JustOutside() {
        MonitorArea.CircleBounds circle = new MonitorArea.CircleBounds(50, 50, 30);
        assertFalse(circle.contains(81, 50));
        assertFalse(circle.contains(50, 81));
    }

    @Test
    void testCircleBounds_DiagonalAtRadius() {
        MonitorArea.CircleBounds circle = new MonitorArea.CircleBounds(100, 100, 14);
        // diagonal distance = sqrt(10^2 + 10^2) = sqrt(200) ≈ 14.14 > 14
        assertFalse(circle.contains(110, 110));
        // diagonal distance = sqrt(9^2 + 9^2) = sqrt(162) ≈ 12.73 < 14
        assertTrue(circle.contains(109, 109));
    }

    @Test
    void testCircleBounds_Type() {
        MonitorArea.CircleBounds circle = new MonitorArea.CircleBounds(50, 50, 30);
        assertEquals(MonitorArea.BoundsType.CIRCLE, circle.getType());
    }

    @Test
    void testCircleBounds_Getters() {
        MonitorArea.CircleBounds circle = new MonitorArea.CircleBounds(123, 456, 789);
        assertEquals(123, circle.getCenterX());
        assertEquals(456, circle.getCenterZ());
        assertEquals(789, circle.getRadius());
    }
}
