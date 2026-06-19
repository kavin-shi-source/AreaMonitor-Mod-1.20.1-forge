package com.kavinshi.areamonitor;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class SelectionPoints {
    private BlockPos firstPoint;
    private BlockPos secondPoint;
    private final List<BlockPos> vertexPoints = new ArrayList<>();
    private boolean isMultiPointMode = false;

    public List<BlockPos> getVertexPoints() { return vertexPoints; }
    public void addVertexPoint(BlockPos pos) { this.vertexPoints.add(pos); }
    public boolean isMultiPointMode() { return isMultiPointMode; }
    public void setMultiPointMode(boolean multiPointMode) { this.isMultiPointMode = multiPointMode; }
    public boolean hasEnoughVerticesForPolygon() { return vertexPoints.size() >= 3; }
    public void clearVertices() { vertexPoints.clear(); }

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
