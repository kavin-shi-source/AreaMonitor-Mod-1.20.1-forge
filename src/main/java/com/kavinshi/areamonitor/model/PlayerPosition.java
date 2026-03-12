package com.kavinshi.areamonitor.model;

/**
 * Player position data class.
 * 
 * <p>Stores player coordinates and dimension information.</p>
 * 
 * @since 1.0.0
 */
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
