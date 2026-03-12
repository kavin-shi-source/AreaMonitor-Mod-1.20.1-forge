package com.kavinshi.areamonitor;

import net.minecraft.server.MinecraftServer;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

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
    private static final long MEMORY_THRESHOLD = 85;
    private static final int MAX_CHECK_INTERVAL = 20;
    private static final int MIN_CHECK_INTERVAL = 1;

    private static volatile int currentCheckInterval = 5;
    private static volatile long lastCheck = 0;
    private static volatile long lastOptimization = 0;
    private static final long OPTIMIZATION_COOLDOWN = 30000;

    private static final AtomicLong TOTAL_CHECKS = new AtomicLong(0);
    private static final AtomicLong SLOW_CHECKS = new AtomicLong(0);
    private static final Map<String, PerformanceMetric> metrics = new ConcurrentHashMap<>();

    private static final int TICK_TIMES_ARRAY_SIZE = 100;
    private static final long[] tickTimes = new long[TICK_TIMES_ARRAY_SIZE];
    private static volatile int tickIndex = 0;
    private static volatile double lastTPS = 20.0;

    private static final MemoryMXBean MEMORY_BEAN = ManagementFactory.getMemoryMXBean();
    private static final List<GarbageCollectorMXBean> GC_BEANS = ManagementFactory.getGarbageCollectorMXBeans();

    private static final Map<String, SmartCache<?, ?>> caches = new ConcurrentHashMap<>();

    private PerformanceMonitor() {
    }

    public static void onServerTick(MinecraftServer server) {
        long currentTime = System.currentTimeMillis();

        updateTPS(currentTime);

        if (currentTime - lastCheck >= MONITOR_INTERVAL * 50) {
            monitorPerformance(server);
            lastCheck = currentTime;
        }

        AreaVisualizer.updatePersistentVisualizations();
    }

    private static void updateTPS(long currentTime) {
        synchronized (tickTimes) {
            tickTimes[tickIndex] = currentTime;
            tickIndex = (tickIndex + 1) % tickTimes.length;

            if (tickTimes[tickIndex] != 0) {
                long elapsed = currentTime - tickTimes[tickIndex];
                if (elapsed > 0) {
                    lastTPS = Math.min(20.0, (tickTimes.length * 1000.0) / elapsed);
                }
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
        if (currentTime - lastOptimization < OPTIMIZATION_COOLDOWN) {
            return;
        }

        if (currentTPS < TPS_THRESHOLD_CRITICAL) {
            adjustCheckInterval(currentCheckInterval + 10);
            clearAllCaches();
            triggerGC();

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
        if (currentTime - lastOptimization < OPTIMIZATION_COOLDOWN) {
            return;
        }

        AreaMonitorMod.LOGGER.warn("High memory usage: {}%, clearing caches", getMemoryUsagePercentage());

        clearAllCaches();
        triggerGC();

        lastOptimization = currentTime;
    }

    private static void adjustCheckInterval(int newInterval) {
        currentCheckInterval = Math.max(MIN_CHECK_INTERVAL, Math.min(MAX_CHECK_INTERVAL, newInterval));
    }

    private static void triggerGC() {
        AreaMonitorMod.LOGGER.debug("Memory cleanup request logged (JVM auto-managed)");
    }

    public static double getTPS() {
        return lastTPS;
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
        PerformanceMetric metric = metrics.computeIfAbsent(name, k -> new PerformanceMetric());
        metric.addValue(value);
    }

    public static Map<String, String> getPerformanceStats() {
        Map<String, String> stats = new HashMap<>();

        stats.put("tps", String.format("%.1f", getTPS()));
        stats.put("memory_percent", String.valueOf(getMemoryUsagePercentage()));
        stats.put("check_interval", String.valueOf(currentCheckInterval));
        stats.put("total_checks", String.valueOf(TOTAL_CHECKS.get()));
        stats.put("slow_checks", String.valueOf(SLOW_CHECKS.get()));

        long total = TOTAL_CHECKS.get();
        long slow = SLOW_CHECKS.get();
        if (total > 0) {
            stats.put("slow_check_percent", String.format("%.1f%%", (slow * 100.0) / total));
        }

        return stats;
    }

    public static <T> T measurePerformance(String operationName, Supplier<T> operation) {
        long startTime = System.nanoTime();
        T result = operation.get();
        long endTime = System.nanoTime();

        double duration = (endTime - startTime) / 1_000_000.0;

        TOTAL_CHECKS.incrementAndGet();
        if (duration > 1.0) {
            SLOW_CHECKS.incrementAndGet();
        }

        recordMetric(operationName + "_duration", duration);

        return result;
    }

    public static void clearAllCaches() {
        caches.values().forEach(SmartCache::clear);
        AreaManager.getInstance().clearUnusedCaches();
        metrics.clear();
        AreaMonitorMod.LOGGER.debug("All caches and metrics cleared");
    }

    public static void clearUnusedCaches() {
        caches.values().forEach(SmartCache::cleanUp);
        AreaMonitorMod.LOGGER.debug("Unused caches cleaned");
    }

    public static <K, V> SmartCache<K, V> createCache(String name, int maxSize, long expireTime) {
        SmartCache<K, V> cache = new SmartCache<>(maxSize, expireTime);
        caches.put(name, cache);
        return cache;
    }

    private static class PerformanceMetric {
        private final List<Double> values = Collections.synchronizedList(new ArrayList<>());
        private volatile double min = Double.MAX_VALUE;
        private volatile double max = Double.MIN_VALUE;
        private volatile double sum = 0;

        public synchronized void addValue(double value) {
            values.add(value);
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;

            if (values.size() > 100) {
                double removed = values.remove(0);
                sum -= removed;
            }
        }

        public synchronized double getAverage() {
            return values.isEmpty() ? 0 : sum / values.size();
        }

        public synchronized double getMin() {
            return values.isEmpty() ? 0 : min;
        }

        public synchronized double getMax() {
            return values.isEmpty() ? 0 : max;
        }
    }
}
