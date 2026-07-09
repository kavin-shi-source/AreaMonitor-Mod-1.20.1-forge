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

    private final Map<Long, Set<String>> spatialGrid = new ConcurrentHashMap<>();
    private final Map<String, MonitorArea> allRegions = new ConcurrentHashMap<>();
    // Reverse index: region name -> grid keys it occupies
    private final Map<String, Set<Long>> regionGrids = new ConcurrentHashMap<>();

    /**
     * Pack grid (x, z) into a single long key to avoid allocating a GridKey object per lookup.
     * P1-13 fix: replaces the old GridKey class — Long.valueOf reuses cached instances for small ranges
     * and avoids the per-call allocation that produced 600-1000 temporary objects per second.
     */
    private static long toKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /**
     * Add region to spatial partition.
     */
    public void addRegion(MonitorArea region) {
        // P2 #12 fix: if a region with the same name already exists (e.g., re-add after bounds
        // change), the old grid entries would leak and produce phantom matches. Remove the stale
        // grid registration before inserting the new one.
        if (allRegions.containsKey(region.getName())) {
            removeRegionFromGrid(region.getName());
        }
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
     * Get potential regions that may intersect with given position.
     * Only checks the player's current grid cell, because addRegionToGrid
     * registers a region into every grid cell it covers, so any region
     * containing the player's position must be present in the current cell.
     */
    public Set<MonitorArea> getPotentialRegions(double x, double z, String dimension) {
        // P1-13 fix: avoid HashSet allocation when no regions occupy this grid cell (the common path)
        int gridX = (int) Math.floor(x / GRID_SIZE);
        int gridZ = (int) Math.floor(z / GRID_SIZE);

        Long key = Long.valueOf(toKey(gridX, gridZ));
        Set<String> regionNames = spatialGrid.get(key);

        if (regionNames == null) return Collections.emptySet();

        Set<MonitorArea> result = new HashSet<>(regionNames.size());
        for (String regionName : regionNames) {
            MonitorArea region = allRegions.get(regionName);
            if (region != null && region.getDimension().equals(dimension)) {
                result.add(region);
            }
        }
        return result;
    }

    /**
     * Get potential regions that may intersect with an AABB.
     * P2 #5 fix: explosions and area-effect events span multiple grid cells; querying only the
     * center cell misses protected areas whose grid cell does not include the explosion origin
     * but contains affected blocks/entities.
     */
    public Set<MonitorArea> getPotentialRegionsInBox(double minX, double minZ, double maxX, double maxZ, String dimension) {
        int minGridX = (int) Math.floor(minX / GRID_SIZE);
        int maxGridX = (int) Math.floor(maxX / GRID_SIZE);
        int minGridZ = (int) Math.floor(minZ / GRID_SIZE);
        int maxGridZ = (int) Math.floor(maxZ / GRID_SIZE);

        Set<MonitorArea> result = new HashSet<>();
        for (int gx = minGridX; gx <= maxGridX; gx++) {
            for (int gz = minGridZ; gz <= maxGridZ; gz++) {
                Long key = Long.valueOf(toKey(gx, gz));
                Set<String> regionNames = spatialGrid.get(key);
                if (regionNames == null) continue;
                for (String regionName : regionNames) {
                    MonitorArea region = allRegions.get(regionName);
                    if (region != null && region.getDimension().equals(dimension)) {
                        result.add(region);
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
     * Clear all data.
     */
    public void clear() {
        spatialGrid.clear();
        allRegions.clear();
        regionGrids.clear();
    }

    private void addRegionToGrid(MonitorArea region) {
        AABB bounds = region.getBoundingBox();

        // Calculate grid boundaries. Use Math.ceil(maxX) - 1 to get the inclusive max block coordinate,
        // preventing boundary-aligned areas (like maxX=255.0) from spilling over into the next grid cell redundantly.
        int minGridX = (int) Math.floor(bounds.minX / GRID_SIZE);
        int maxGridX = (int) Math.floor((Math.ceil(bounds.maxX) - 1.0) / GRID_SIZE);
        int minGridZ = (int) Math.floor(bounds.minZ / GRID_SIZE);
        int maxGridZ = (int) Math.floor((Math.ceil(bounds.maxZ) - 1.0) / GRID_SIZE);

        Set<Long> occupiedGrids = ConcurrentHashMap.newKeySet();

        for (int x = minGridX; x <= maxGridX; x++) {
            for (int z = minGridZ; z <= maxGridZ; z++) {
                Long gridKey = Long.valueOf(toKey(x, z));

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
        Set<Long> occupiedGrids = regionGrids.remove(regionName);

        if (occupiedGrids != null) {
            for (Long gridKey : occupiedGrids) {
                // Atomically remove region name and drop the cell if empty.
                // Using computeIfPresent prevents the race where another thread adds a new
                // region to this cell between our isEmpty() check and spatialGrid.remove().
                spatialGrid.computeIfPresent(gridKey, (k, regions) -> {
                    regions.remove(regionName);
                    return regions.isEmpty() ? null : regions;
                });
            }
        }
    }
}
