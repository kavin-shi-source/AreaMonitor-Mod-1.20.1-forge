package com.kavinshi.areamonitor.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerPositionTest {

    @Test
    void testEquals_SameValues() {
        PlayerPosition p1 = new PlayerPosition(100.5, 200.5, "minecraft:overworld");
        PlayerPosition p2 = new PlayerPosition(100.5, 200.5, "minecraft:overworld");
        assertEquals(p1, p2);
    }

    @Test
    void testEquals_SameObject() {
        PlayerPosition p1 = new PlayerPosition(100.5, 200.5, "minecraft:overworld");
        assertEquals(p1, p1);
    }

    @Test
    void testEquals_DifferentX() {
        PlayerPosition p1 = new PlayerPosition(100.5, 200.5, "minecraft:overworld");
        PlayerPosition p2 = new PlayerPosition(999.0, 200.5, "minecraft:overworld");
        assertNotEquals(p1, p2);
    }

    @Test
    void testEquals_DifferentZ() {
        PlayerPosition p1 = new PlayerPosition(100.5, 200.5, "minecraft:overworld");
        PlayerPosition p2 = new PlayerPosition(100.5, 999.0, "minecraft:overworld");
        assertNotEquals(p1, p2);
    }

    @Test
    void testEquals_DifferentDimension() {
        PlayerPosition p1 = new PlayerPosition(100.5, 200.5, "minecraft:overworld");
        PlayerPosition p2 = new PlayerPosition(100.5, 200.5, "minecraft:the_nether");
        assertNotEquals(p1, p2);
    }

    @Test
    void testEquals_Null() {
        PlayerPosition p1 = new PlayerPosition(100.5, 200.5, "minecraft:overworld");
        assertNotEquals(null, p1);
    }

    @Test
    void testEquals_DifferentType() {
        PlayerPosition p1 = new PlayerPosition(100.5, 200.5, "minecraft:overworld");
        assertNotEquals("not a PlayerPosition", p1);
    }

    @Test
    void testHashCode_SameValues() {
        PlayerPosition p1 = new PlayerPosition(100.5, 200.5, "minecraft:overworld");
        PlayerPosition p2 = new PlayerPosition(100.5, 200.5, "minecraft:overworld");
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void testHashCode_DifferentValues() {
        PlayerPosition p1 = new PlayerPosition(100.5, 200.5, "minecraft:overworld");
        PlayerPosition p2 = new PlayerPosition(999.0, 999.0, "minecraft:the_nether");
        assertNotEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void testGetters() {
        PlayerPosition p = new PlayerPosition(123.45, 678.90, "minecraft:overworld");
        assertEquals(123.45, p.getX(), 0.001);
        assertEquals(678.90, p.getZ(), 0.001);
        assertEquals("minecraft:overworld", p.getDimension());
    }

    @Test
    void testNegativeCoordinates() {
        PlayerPosition p1 = new PlayerPosition(-150.0, -300.0, "minecraft:overworld");
        PlayerPosition p2 = new PlayerPosition(-150.0, -300.0, "minecraft:overworld");
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }
}
