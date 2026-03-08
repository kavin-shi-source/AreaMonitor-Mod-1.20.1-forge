package com.kavinshi.areamonitor;

import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 四叉树空间分区管理器
 * 用于优化大量区域的检测性能
 */
public class SpatialPartitionManager {
    private static final int GRID_SIZE = 256; // 网格大小
    private static final int MAX_REGIONS_PER_GRID = 50; // 每个网格最大区域数

    // 空间网格映射 [x][z] -> 区域集合
    private final Map<String, Set<String>> spatialGrid = new ConcurrentHashMap<>();
    private final Map<String, MonitorArea> allRegions = new ConcurrentHashMap<>();

    /**
     * 添加区域到空间分区
     */
    public void addRegion(MonitorArea region) {
        allRegions.put(region.getName(), region);
        addRegionToGrid(region);
    }

    /**
     * 从空间分区移除区域
     */
    public void removeRegion(String regionName) {
        MonitorArea region = allRegions.remove(regionName);
        if (region != null) {
            removeRegionFromGrid(region);
        }
    }

    /**
     * 更新区域在空间分区中的位置
     */
    public void updateRegion(MonitorArea region) {
        // 先移除旧的位置
        removeRegionFromGrid(region);
        // 重新添加
        addRegionToGrid(region);
    }

    /**
     * 获取可能与给定位置相交的区域
     */
    public Set<MonitorArea> getPotentialRegions(double x, double z, String dimension) {
        Set<MonitorArea> result = new HashSet<>();

        // 获取玩家所在网格及相邻网格
        int gridX = (int) Math.floor(x / GRID_SIZE);
        int gridZ = (int) Math.floor(z / GRID_SIZE);

        // 检查3x3网格范围（包括相邻网格）
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
     * 获取所有区域
     */
    public Collection<MonitorArea> getAllRegions() {
        return allRegions.values();
    }

    /**
     * 获取区域数量
     */
    public int getRegionCount() {
        return allRegions.size();
    }

    /**
     * 获取网格统计信息
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
     * 清除所有数据
     */
    public void clear() {
        spatialGrid.clear();
        allRegions.clear();
    }

    // 私有方法

    private void addRegionToGrid(MonitorArea region) {
        AABB bounds = region.getBoundingBox();

        // 计算区域覆盖的网格范围
        int minGridX = (int) Math.floor(bounds.minX / GRID_SIZE);
        int maxGridX = (int) Math.floor(bounds.maxX / GRID_SIZE);
        int minGridZ = (int) Math.floor(bounds.minZ / GRID_SIZE);
        int maxGridZ = (int) Math.floor(bounds.maxZ / GRID_SIZE);

        // 将区域添加到所有覆盖的网格中
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

        // 从所有网格中移除该区域
        spatialGrid.values().forEach(regions -> regions.remove(regionName));

        // 清理空网格
        spatialGrid.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private String getGridKey(int gridX, int gridZ) {
        return gridX + "," + gridZ;
    }
}