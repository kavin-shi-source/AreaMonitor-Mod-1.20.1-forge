package com.kavinshi.areamonitor;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Spatial partition manager using a fixed-size grid.
 * Used to optimize detection performance for large numbers of regions.
 *
 * <p>Uses fastutil's {@link Long2ObjectOpenHashMap} with primitive long keys to avoid
 * Long boxing on every grid lookup (hot path: per-player per-tick). A
 * {@link ReentrantReadWriteLock} provides thread safety — reads (grid lookups)
 * proceed concurrently, writes (area add/remove) are exclusive.</p>
 */
public class SpatialPartitionManager {
    private static final int GRID_SIZE = 256;

    private final Long2ObjectOpenHashMap<Set<String>> spatialGrid = new Long2ObjectOpenHashMap<>();
    private final Map<String, MonitorArea> allRegions = new ConcurrentHashMap<>();
    private final Map<String, LongSet> regionGrids = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock gridLock = new ReentrantReadWriteLock();

    /**
     * Pack grid (x, z) into a single primitive long key.
     */
    private static long toKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /**
     * Add region to spatial partition.
     */
    public void addRegion(MonitorArea region) {
        // if a region with the same name already exists (e.g., re-add after bounds
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
        int gridX = (int) Math.floor(x / GRID_SIZE);
        int gridZ = (int) Math.floor(z / GRID_SIZE);

        gridLock.readLock().lock();
        try {
            Set<String> regionNames = spatialGrid.get(toKey(gridX, gridZ));
            if (regionNames == null) return Collections.emptySet();

            Set<MonitorArea> result = new HashSet<>(regionNames.size());
            for (String regionName : regionNames) {
                MonitorArea region = allRegions.get(regionName);
                if (region != null && region.getDimension().equals(dimension)) {
                    result.add(region);
                }
            }
            return result;
        } finally {
            gridLock.readLock().unlock();
        }
    }

    /**
     * Get potential regions that may intersect with an AABB.
     * explosions and area-effect events span multiple grid cells; querying only the
     * center cell misses protected areas whose grid cell does not include the explosion origin
     * but contains affected blocks/entities.
     */
    public Set<MonitorArea> getPotentialRegionsInBox(double minX, double minZ, double maxX, double maxZ, String dimension) {
        int minGridX = (int) Math.floor(minX / GRID_SIZE);
        int maxGridX = (int) Math.floor(maxX / GRID_SIZE);
        int minGridZ = (int) Math.floor(minZ / GRID_SIZE);
        int maxGridZ = (int) Math.floor(maxZ / GRID_SIZE);

        gridLock.readLock().lock();
        try {
            Set<MonitorArea> result = new HashSet<>();
            for (int gx = minGridX; gx <= maxGridX; gx++) {
                for (int gz = minGridZ; gz <= maxGridZ; gz++) {
                    Set<String> regionNames = spatialGrid.get(toKey(gx, gz));
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
        } finally {
            gridLock.readLock().unlock();
        }
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
        gridLock.writeLock().lock();
        try {
            spatialGrid.clear();
            regionGrids.clear();
        } finally {
            gridLock.writeLock().unlock();
        }
        allRegions.clear();
    }

    private void addRegionToGrid(MonitorArea region) {
        AABB bounds = region.getBoundingBox();

        // Calculate grid boundaries. Use Math.ceil(maxX) - 1 to get the inclusive max block coordinate,
        // preventing boundary-aligned areas (like maxX=255.0) from spilling over into the next grid cell redundantly.
        int minGridX = (int) Math.floor(bounds.minX / GRID_SIZE);
        int maxGridX = (int) Math.floor((Math.ceil(bounds.maxX) - 1.0) / GRID_SIZE);
        int minGridZ = (int) Math.floor(bounds.minZ / GRID_SIZE);
        int maxGridZ = (int) Math.floor((Math.ceil(bounds.maxZ) - 1.0) / GRID_SIZE);

        LongSet occupiedGrids = new LongOpenHashSet();

        gridLock.writeLock().lock();
        try {
            for (int x = minGridX; x <= maxGridX; x++) {
                for (int z = minGridZ; z <= maxGridZ; z++) {
                    long gridKey = toKey(x, z);
                    Set<String> regions = spatialGrid.get(gridKey);
                    if (regions == null) {
                        regions = ConcurrentHashMap.newKeySet();
                        spatialGrid.put(gridKey, regions);
                    }
                    regions.add(region.getName());
                    occupiedGrids.add(gridKey);
                }
            }
        } finally {
            gridLock.writeLock().unlock();
        }

        regionGrids.put(region.getName(), occupiedGrids);
    }

    /**
     * Optimized removal using reverse index.
     * O(k) where k is the number of grids the region occupies, instead of O(n) where n is total grids.
     */
    private void removeRegionFromGrid(String regionName) {
        LongSet occupiedGrids = regionGrids.remove(regionName);

        if (occupiedGrids != null) {
            gridLock.writeLock().lock();
            try {
                for (long gridKey : occupiedGrids) {
                    Set<String> regions = spatialGrid.get(gridKey);
                    if (regions != null) {
                        regions.remove(regionName);
                        if (regions.isEmpty()) {
                            spatialGrid.remove(gridKey);
                        }
                    }
                }
            } finally {
                gridLock.writeLock().unlock();
            }
        }
    }
}
