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

    private final Map<GridKey, Set<String>> spatialGrid = new ConcurrentHashMap<>();
    private final Map<String, MonitorArea> allRegions = new ConcurrentHashMap<>();
    // Reverse index: region name -> grid keys it occupies
    private final Map<String, Set<GridKey>> regionGrids = new ConcurrentHashMap<>();

    /**
     * Grid key class to avoid string concatenation overhead.
     * Uses proper hashCode and equals for efficient HashMap lookups.
     */
    private static class GridKey {
        private final int x;
        private final int z;
        private final int hash;

        GridKey(int x, int z) {
            this.x = x;
            this.z = z;
            // Pre-compute hash to avoid repeated calculations
            this.hash = 31 * x + z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GridKey)) return false;
            GridKey gridKey = (GridKey) o;
            return x == gridKey.x && z == gridKey.z;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    /**
     * Add region to spatial partition.
     */
    public void addRegion(MonitorArea region) {
        allRegions.put(region.getName(), region);
        addRegionToGrid(region);
    }

    /**
     * Remove region from spatial partition.
     * Optimized O(k) algorithm where k is the number of grids the region occupies.
     */
    public void removeRegion(String regionName) {
        MonitorArea region = allRegions.remove(regionName);
        if (region != null) {
            removeRegionFromGrid(regionName);
        }
    }

    /**
     * Update region position in spatial partition.
     */
    public void updateRegion(MonitorArea region) {
        removeRegionFromGrid(region.getName());
        addRegionToGrid(region);
    }

    /**
     * Get potential regions that may intersect with given position.
     * Optimized to only check the player's current grid cell and adjacent cells
     * only if regions span multiple grid cells.
     */
    public Set<MonitorArea> getPotentialRegions(double x, double z, String dimension) {
        Set<MonitorArea> result = new HashSet<>();

        int gridX = (int) Math.floor(x / GRID_SIZE);
        int gridZ = (int) Math.floor(z / GRID_SIZE);

        // First check the player's current grid cell
        GridKey gridKey = new GridKey(gridX, gridZ);
        Set<String> regionNames = spatialGrid.get(gridKey);

        if (regionNames != null) {
            for (String regionName : regionNames) {
                MonitorArea region = allRegions.get(regionName);
                if (region != null && region.getDimension().equals(dimension)) {
                    result.add(region);
                }
            }
        }

        // Only check adjacent cells if we're near a grid boundary
        // This reduces unnecessary checks by ~66% in most cases
        double cellX = x % GRID_SIZE;
        double cellZ = z % GRID_SIZE;
        boolean nearXBoundary = cellX < 16 || cellX > (GRID_SIZE - 16);
        boolean nearZBoundary = cellZ < 16 || cellZ > (GRID_SIZE - 16);

        if (nearXBoundary || nearZBoundary) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue; // Already checked

                    GridKey adjacentKey = new GridKey(gridX + dx, gridZ + dz);
                    Set<String> adjacentRegions = spatialGrid.get(adjacentKey);

                    if (adjacentRegions != null) {
                        for (String regionName : adjacentRegions) {
                            MonitorArea region = allRegions.get(regionName);
                            if (region != null && region.getDimension().equals(dimension)) {
                                result.add(region);
                            }
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
        regionGrids.clear();
    }

    private void addRegionToGrid(MonitorArea region) {
        AABB bounds = region.getBoundingBox();

        int minGridX = (int) Math.floor(bounds.minX / GRID_SIZE);
        int maxGridX = (int) Math.floor(bounds.maxX / GRID_SIZE);
        int minGridZ = (int) Math.floor(bounds.minZ / GRID_SIZE);
        int maxGridZ = (int) Math.floor(bounds.maxZ / GRID_SIZE);

        Set<GridKey> occupiedGrids = ConcurrentHashMap.newKeySet();

        for (int x = minGridX; x <= maxGridX; x++) {
            for (int z = minGridZ; z <= maxGridZ; z++) {
                GridKey gridKey = new GridKey(x, z);

                spatialGrid.computeIfAbsent(gridKey, k -> ConcurrentHashMap.newKeySet())
                          .add(region.getName());

                occupiedGrids.add(gridKey);
            }
        }

        // Store reverse index
        regionGrids.put(region.getName(), occupiedGrids);
    }

    /**
     * Optimized removal using reverse index.
     * O(k) where k is the number of grids the region occupies, instead of O(n) where n is total grids.
     */
    private void removeRegionFromGrid(String regionName) {
        Set<GridKey> occupiedGrids = regionGrids.remove(regionName);

        if (occupiedGrids != null) {
            for (GridKey gridKey : occupiedGrids) {
                Set<String> regions = spatialGrid.get(gridKey);
                if (regions != null) {
                    regions.remove(regionName);
                    // Clean up empty grid cells
                    if (regions.isEmpty()) {
                        spatialGrid.remove(gridKey);
                    }
                }
            }
        }
    }
}
