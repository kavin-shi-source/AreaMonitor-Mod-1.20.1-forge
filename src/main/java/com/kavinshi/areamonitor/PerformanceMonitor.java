package com.kavinshi.areamonitor;

import net.minecraft.server.MinecraftServer;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * Performance monitoring and optimization system.
 *
 * Monitors server TPS, memory usage, and automatically adjusts check intervals
 * to maintain optimal performance while providing area monitoring functionality.
 */
public class PerformanceMonitor {
    private static final int MONITOR_INTERVAL = 100;
    private static final double TPS_THRESHOLD_LOW = 18.0;  // Below 18 TPS indicates performance degradation
    private static final double TPS_THRESHOLD_CRITICAL = 15.0;  // Below 15 TPS is critical
    private static final long MEMORY_THRESHOLD = 85;  // 85% memory usage threshold
    private static final int MAX_CHECK_INTERVAL = 20;
    private static final int MIN_CHECK_INTERVAL = 1;
    private static final int MAX_METRICS_SIZE = 50;  // Limit metrics map size to prevent memory leak

    private static int currentCheckInterval = 5;
    private static long lastCheck = 0;
    private static long lastOptimization = 0;

    private static final Map<String, PerformanceMetric> metrics = new ConcurrentHashMap<>();

    private static final int TICK_TIMES_ARRAY_SIZE = 100;
    private static final long[] tickTimes = new long[TICK_TIMES_ARRAY_SIZE];
    private static final AtomicInteger tickIndex = new AtomicInteger(0);
    private static final AtomicReference<Double> lastTPS = new AtomicReference<>(20.0);

    private static final MemoryMXBean MEMORY_BEAN = ManagementFactory.getMemoryMXBean();

    private PerformanceMonitor() {
    }

    public static void onServerTick(MinecraftServer server) {
        long currentTime = System.currentTimeMillis();

        updateTPS(currentTime);

        if (currentTime - lastCheck >= MONITOR_INTERVAL * 50) {
            monitorPerformance(server);
            lastCheck = currentTime;
        }

    }

    /**
     * Update TPS calculation using lock-free algorithm.
     * Uses AtomicInteger to avoid synchronized block on every tick.
     */
    private static void updateTPS(long currentTime) {
        int currentIndex = tickIndex.getAndIncrement() % TICK_TIMES_ARRAY_SIZE;
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

    private static void monitorPerformance(MinecraftServer server) {
        double currentTPS = getTPS();
        long memoryUsage = getMemoryUsagePercentage();

        recordMetric("tps", currentTPS);
        recordMetric("memory", memoryUsage);
        recordMetric("check_interval", currentCheckInterval);

        if (currentTPS < TPS_THRESHOLD_LOW) {
            handleLowTPS(currentTPS);
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

    private static void recordMetric(String name, double value) {
        // Limit metrics map size to prevent memory leak
        if (metrics.size() >= MAX_METRICS_SIZE && !metrics.containsKey(name)) {
            AreaMonitorMod.LOGGER.debug("Metrics map size limit reached, skipping new metric: {}", name);
            return;
        }
        PerformanceMetric metric = metrics.computeIfAbsent(name, k -> new PerformanceMetric());
        metric.addValue(value);
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
        metrics.clear();
        AreaMonitorMod.LOGGER.debug("All caches and metrics cleared");
    }

    public static void clearUnusedCaches() {
        AreaManager.getInstance().clearUnusedCaches();
        AreaMonitorMod.LOGGER.debug("Unused caches cleaned");
    }

    /**
     * Lock-free performance metric using concurrent data structures.
     * Eliminates synchronized overhead and uses efficient atomic operations.
     */
    private static class PerformanceMetric {
        private final ConcurrentLinkedDeque<Double> values = new ConcurrentLinkedDeque<>();
        private final AtomicReference<Double> min = new AtomicReference<>(Double.MAX_VALUE);
        private final AtomicReference<Double> max = new AtomicReference<>(Double.MIN_VALUE);
        private final LongAdder sum = new LongAdder();
        private final AtomicInteger count = new AtomicInteger(0);

        public void addValue(double value) {
            values.addLast(value);
            count.incrementAndGet();

            // Update min atomically
            min.updateAndGet(current -> Math.min(current, value));

            // Update max atomically
            max.updateAndGet(current -> Math.max(current, value));

            // Add to sum (LongAdder is lock-free and efficient)
            sum.add((long)(value * 1000)); // Store as millis to avoid precision loss

            // Keep only last 100 values
            if (count.get() > 100) {
                Double removed = values.pollFirst();
                if (removed != null) {
                    sum.add(-(long)(removed * 1000));
                    count.decrementAndGet();
                }
            }
        }

        public double getAverage() {
            int size = count.get();
            return size == 0 ? 0 : sum.sum() / 1000.0 / size;
        }

        public double getMin() {
            double minValue = min.get();
            return minValue == Double.MAX_VALUE ? 0 : minValue;
        }

        public double getMax() {
            double maxValue = max.get();
            return maxValue == Double.MIN_VALUE ? 0 : maxValue;
        }
    }
}
