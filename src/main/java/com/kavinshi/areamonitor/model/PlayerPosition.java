package com.kavinshi.areamonitor.model;

public class PlayerPosition {
    private final double x;
    private final double z;
    private final String dimension;

    public PlayerPosition(double x, double z, String dimension) {
        this.x = x;
        this.z = z;
        this.dimension = dimension;
    }

    public double getX() {
        return x;
    }

    public double getZ() {
        return z;
    }

    public String getDimension() {
        return dimension;
    }
}
