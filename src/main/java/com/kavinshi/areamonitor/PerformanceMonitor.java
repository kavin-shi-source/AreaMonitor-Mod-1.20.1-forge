package com.kavinshi.areamonitor;


import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Performance monitoring and optimization system.
 *
 * Monitors server TPS, memory usage, and automatically adjusts check intervals
 * to maintain optimal performance while providing area monitoring functionality.
 */
public class PerformanceMonitor {
    private static final int MONITOR_INTERVAL = 100;
    private static final double TPS_THRESHOLD_LOW = 18.0;
    private static final double TPS_THRESHOLD_CRITICAL = 15.0;
    private static final double TPS_THRESHOLD_HEALTHY = 19.5;
    private static final int HEALTHY_TICKS_REQUIRED_FOR_RECOVERY = 600;
    private static final long MEMORY_THRESHOLD = 85;
    private static final int MAX_CHECK_INTERVAL = 20;
    private static final int MIN_CHECK_INTERVAL = 1;

    private static volatile int currentCheckInterval = 5;
    private static volatile long lastCheck = 0;
    private static volatile long lastOptimization = 0;
    private static volatile int consecutiveHealthyTicks = 0;

    private static final int TICK_TIMES_ARRAY_SIZE = 100;
    private static final long[] tickTimes = new long[TICK_TIMES_ARRAY_SIZE];
    private static final AtomicInteger tickIndex = new AtomicInteger(0);
    private static final AtomicReference<Double> lastTPS = new AtomicReference<>(20.0);

    private static final MemoryMXBean MEMORY_BEAN = ManagementFactory.getMemoryMXBean();

    private PerformanceMonitor() {
    }

    public static void onServerTick() {
        long currentTime = System.currentTimeMillis();

        updateTPS(currentTime);

        if (currentTime - lastCheck >= MONITOR_INTERVAL * 50) {
            monitorPerformance();
            lastCheck = currentTime;
        }

    }

    /**
     * Update TPS calculation using lock-free algorithm.
     * Uses AtomicInteger to avoid synchronized block on every tick.
     */
    private static void updateTPS(long currentTime) {
        int currentIndex = Math.floorMod(tickIndex.getAndIncrement(), TICK_TIMES_ARRAY_SIZE);
        tickTimes[currentIndex] = currentTime;

        int nextIndex = (currentIndex + 1) % TICK_TIMES_ARRAY_SIZE;
        long oldTime = tickTimes[nextIndex];

        if (oldTime != 0) {
            long elapsed = currentTime - oldTime;
            if (elapsed > 0) {
                double tps = Math.min(20.0, (TICK_TIMES_ARRAY_SIZE * 1000.0) / elapsed);
                lastTPS.set(tps);
            }
        }
    }

    private static void monitorPerformance() {
        double currentTPS = getTPS();
        long memoryUsage = getMemoryUsagePercentage();

        if (currentTPS < TPS_THRESHOLD_LOW) {
            handleLowTPS(currentTPS);
            consecutiveHealthyTicks = 0;
        } else if (currentTPS >= TPS_THRESHOLD_HEALTHY && currentCheckInterval > MIN_CHECK_INTERVAL) {
            consecutiveHealthyTicks++;
            if (consecutiveHealthyTicks >= HEALTHY_TICKS_REQUIRED_FOR_RECOVERY) {
                int newInterval = Math.max(MIN_CHECK_INTERVAL, currentCheckInterval / 2);
                adjustCheckInterval(newInterval);
                consecutiveHealthyTicks = 0;
                AreaMonitorMod.LOGGER.info("TPS recovered to {}, reducing check interval to {}",
                    String.format("%.1f", currentTPS), currentCheckInterval);
            }
        } else {
            consecutiveHealthyTicks = 0;
        }

        if (memoryUsage > MEMORY_THRESHOLD) {
            handleHighMemoryUsage();
        }

        if (currentTPS < TPS_THRESHOLD_CRITICAL || memoryUsage > 95) {
            AreaMonitorMod.LOGGER.warn("Performance warning - TPS: {}, Memory: {}%",
                String.format("%.1f", currentTPS), memoryUsage);
        }
    }

    private static void handleLowTPS(double currentTPS) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastOptimization < ConfigManager.CONFIG.optimizationCooldownMs.get()) {
            return;
        }

        if (currentTPS < TPS_THRESHOLD_CRITICAL) {
            adjustCheckInterval(currentCheckInterval + 10);
            clearAllCaches();

            AreaMonitorMod.LOGGER.warn("Critical performance issue! TPS: {}, Check interval adjusted to: {} tick",
                String.format("%.1f", currentTPS), currentCheckInterval);
        } else {
            adjustCheckInterval(currentCheckInterval + 2);
            clearUnusedCaches();

            AreaMonitorMod.LOGGER.info("Performance optimization - TPS: {}, Check interval adjusted to: {} tick",
                String.format("%.1f", currentTPS), currentCheckInterval);
        }

        lastOptimization = currentTime;
    }

    private static void handleHighMemoryUsage() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastOptimization < ConfigManager.CONFIG.optimizationCooldownMs.get()) {
            return;
        }

        AreaMonitorMod.LOGGER.warn("High memory usage: {}%, clearing caches", getMemoryUsagePercentage());

        clearAllCaches();

        lastOptimization = currentTime;
    }

    private static void adjustCheckInterval(int newInterval) {
        currentCheckInterval = Math.max(MIN_CHECK_INTERVAL, Math.min(MAX_CHECK_INTERVAL, newInterval));
    }

    public static double getTPS() {
        return lastTPS.get();
    }

    public static long getMemoryUsagePercentage() {
        long used = MEMORY_BEAN.getHeapMemoryUsage().getUsed();
        long max = MEMORY_BEAN.getHeapMemoryUsage().getMax();
        return max > 0 ? (used * 100) / max : 0;
    }

    public static int getCurrentCheckInterval() {
        return currentCheckInterval;
    }

    public static Map<String, String> getPerformanceStats() {
        Map<String, String> stats = new HashMap<>();

        stats.put("tps", String.format("%.1f", getTPS()));
        stats.put("memory_percent", String.valueOf(getMemoryUsagePercentage()));
        stats.put("check_interval", String.valueOf(currentCheckInterval));

        return stats;
    }

    public static void clearAllCaches() {
        AreaManager.getInstance().clearUnusedCaches();
        // Reset performance tracking state
        Arrays.fill(tickTimes, 0);
        tickIndex.set(0);
        lastTPS.set(20.0);
        consecutiveHealthyTicks = 0;
        currentCheckInterval = 5;
        lastCheck = 0;
        lastOptimization = 0;
        AreaMonitorMod.LOGGER.debug("All caches cleared and performance state reset");
    }

    public static void clearUnusedCaches() {
        AreaManager.getInstance().clearUnusedCaches();
        AreaMonitorMod.LOGGER.debug("Unused caches cleaned");
    }
}
