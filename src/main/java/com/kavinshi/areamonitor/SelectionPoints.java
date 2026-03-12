package com.kavinshi.areamonitor;

import net.minecraft.core.BlockPos;

/**
 * Data class for storing selection points.
 */
public class SelectionPoints {
    private volatile BlockPos firstPoint;
    private volatile BlockPos secondPoint;

    public boolean hasFirstPoint() {
        return firstPoint != null;
    }

    public boolean hasSecondPoint() {
        return secondPoint != null;
    }

    public boolean isComplete() {
        return firstPoint != null && secondPoint != null;
    }

    public BlockPos getFirstPoint() {
        return firstPoint;
    }

    public BlockPos getSecondPoint() {
        return secondPoint;
    }

    public void setFirstPoint(BlockPos firstPoint) {
        this.firstPoint = firstPoint;
    }

    public void setSecondPoint(BlockPos secondPoint) {
        this.secondPoint = secondPoint;
    }
}
