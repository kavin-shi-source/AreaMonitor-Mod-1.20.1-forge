package com.kavinshi.areamonitor.model;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerPosition that)) return false;
        return Double.compare(that.x, x) == 0 &&
               Double.compare(that.z, z) == 0 &&
               dimension.equals(that.dimension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z, dimension);
    }
}
