package com.kavinshi.areamonitor.model;

import java.util.Objects;

public class PlayerPosition {
    private final double x;
    // P3 #5: track Y so we can distinguish same X/Z at different heights (e.g. Nether roof vs floor)
    private final double y;
    private final double z;
    private final String dimension;

    public PlayerPosition(double x, double z, String dimension) {
        // Backwards-compatible ctor: Y defaults to 0 (unknown) for callers that don't care.
        this(x, 0.0, z, dimension);
    }

    public PlayerPosition(double x, double y, double z, String dimension) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = Objects.requireNonNull(dimension, "dimension must not be null");
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String getDimension() {
        return dimension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerPosition that)) return false;
        return Double.compare(that.x, x) == 0 &&
               Double.compare(that.y, y) == 0 &&
               Double.compare(that.z, z) == 0 &&
               dimension.equals(that.dimension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, dimension);
    }
}
