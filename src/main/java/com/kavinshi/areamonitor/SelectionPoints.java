package com.kavinshi.areamonitor;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class SelectionPoints {
    private BlockPos firstPoint;
    private String firstPointDimension;
    private BlockPos secondPoint;
    private String secondPointDimension;
    private final List<BlockPos> vertexPoints = new ArrayList<>();
    private String polygonDimension;
    private boolean isMultiPointMode = false;

    public List<BlockPos> getVertexPoints() { return vertexPoints; }
    public void addVertexPoint(BlockPos pos) { this.vertexPoints.add(pos); }
    public boolean isMultiPointMode() { return isMultiPointMode; }
    public void setMultiPointMode(boolean multiPointMode) { this.isMultiPointMode = multiPointMode; }
    public boolean hasEnoughVerticesForPolygon() { return vertexPoints.size() >= 3; }
    public void clearVertices() { vertexPoints.clear(); polygonDimension = null; }
    public void setPolygonDimension(String dim) { this.polygonDimension = dim; }
    public String getPolygonDimension() { return polygonDimension; }

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

    public String getFirstPointDimension() {
        return firstPointDimension;
    }

    public BlockPos getSecondPoint() {
        return secondPoint;
    }

    public String getSecondPointDimension() {
        return secondPointDimension;
    }

    public void setFirstPoint(BlockPos firstPoint, String dimension) {
        this.firstPoint = firstPoint;
        this.firstPointDimension = dimension;
    }

    public void setSecondPoint(BlockPos secondPoint, String dimension) {
        this.secondPoint = secondPoint;
        this.secondPointDimension = dimension;
    }

    public boolean hasConsistentDimensions() {
        if (firstPointDimension == null || secondPointDimension == null) return true;
        return firstPointDimension.equals(secondPointDimension);
    }
}
