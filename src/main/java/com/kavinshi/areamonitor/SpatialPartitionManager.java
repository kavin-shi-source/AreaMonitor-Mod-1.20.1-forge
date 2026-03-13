package com.kavinshi.areamonitor;

import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quad-tree spatial partition manager.
 * Used to optimize detection performance for large numbers of regions.
 */
public class SpatialPartitionManager {
    private static final int GRID_SIZE = 256;
    private static final int MAX_REGIONS_PER_GRID = 50;

    private final Map<String, Set<String>> spatialGrid = new ConcurrentHashMap<>();
    private final Map<String, MonitorArea> allRegions = new ConcurrentHashMap<>();

    /**
     * Add region to spatial partition.
     */
    public void addRegion(MonitorArea region) {
        allRegions.put(region.getName(), region);
        addRegionToGrid(region);
    }

    /**
     * Remove region from spatial partition.
     */
    public void removeRegion(String regionName) {
        MonitorArea region = allRegions.remove(regionName);
        if (region != null) {
            removeRegionFromGrid(region);
        }
    }

    /**
     * Update region position in spatial partition.
     */
    public void updateRegion(MonitorArea region) {
        removeRegionFromGrid(region);
        addRegionToGrid(region);
    }

    /**
     * Get potential regions that may intersect with given position.
     */
    public Set<MonitorArea> getPotentialRegions(double x, double z, String dimension) {
        Set<MonitorArea> result = new HashSet<>();

        int gridX = (int) Math.floor(x / GRID_SIZE);
        int gridZ = (int) Math.floor(z / GRID_SIZE);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                String gridKey = getGridKey(gridX + dx, gridZ + dz);
                Set<String> regionNames = spatialGrid.get(gridKey);

                if (regionNames != null) {
                    for (String regionName : regionNames) {
                        MonitorArea region = allRegions.get(regionName);
                        if (region != null && region.getDimension().equals(dimension)) {
                            result.add(region);
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Get all regions.
     */
    public Collection<MonitorArea> getAllRegions() {
        return allRegions.values();
    }

    /**
     * Get region count.
     */
    public int getRegionCount() {
        return allRegions.size();
    }

    /**
     * Get grid statistics.
     */
    public Map<String, Object> getGridStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_regions", allRegions.size());
        stats.put("total_grids", spatialGrid.size());

        int maxRegionsInGrid = 0;
        int totalRegionsInGrids = 0;

        for (Set<String> regions : spatialGrid.values()) {
            int count = regions.size();
            maxRegionsInGrid = Math.max(maxRegionsInGrid, count);
            totalRegionsInGrids += count;
        }

        stats.put("max_regions_per_grid", maxRegionsInGrid);
        stats.put("avg_regions_per_grid", spatialGrid.isEmpty() ? 0 :
                  (double) totalRegionsInGrids / spatialGrid.size());

        return stats;
    }

    /**
     * Clear all data.
     */
    public void clear() {
        spatialGrid.clear();
        allRegions.clear();
    }

    private void addRegionToGrid(MonitorArea region) {
        AABB bounds = region.getBoundingBox();

        int minGridX = (int) Math.floor(bounds.minX / GRID_SIZE);
        int maxGridX = (int) Math.floor(bounds.maxX / GRID_SIZE);
        int minGridZ = (int) Math.floor(bounds.minZ / GRID_SIZE);
        int maxGridZ = (int) Math.floor(bounds.maxZ / GRID_SIZE);

        for (int x = minGridX; x <= maxGridX; x++) {
            for (int z = minGridZ; z <= maxGridZ; z++) {
                String gridKey = getGridKey(x, z);

                spatialGrid.computeIfAbsent(gridKey, k -> ConcurrentHashMap.newKeySet())
                          .add(region.getName());
            }
        }
    }

    private void removeRegionFromGrid(MonitorArea region) {
        String regionName = region.getName();

        spatialGrid.values().forEach(regions -> regions.remove(regionName));

        spatialGrid.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private String getGridKey(int gridX, int gridZ) {
        return gridX + "," + gridZ;
    }
}
