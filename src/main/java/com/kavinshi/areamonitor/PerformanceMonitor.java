package com.kavinshi.areamonitor;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Performance monitoring and optimization system
 *
 * Monitors server TPS, memory usage, and automatically adjusts check intervals
 * to maintain optimal performance while providing area monitoring functionality.
 */
public class PerformanceMonitor {
    private static final int MONITOR_INTERVAL = 100; // Check every 100 ticks
    private static final double TPS_THRESHOLD_LOW = 18.0;
    private static final double TPS_THRESHOLD_CRITICAL = 15.0;
    private static final long MEMORY_THRESHOLD = 85; // 85% memory usage threshold
    private static final int MAX_CHECK_INTERVAL = 20; // Maximum check interval (1 second)
    private static final int MIN_CHECK_INTERVAL = 1; // Minimum check interval (50ms)

    private static int currentCheckInterval = 5; // 当前检查间隔
    private static long lastCheck = 0;
    private static long lastOptimization = 0;
    private static final long OPTIMIZATION_COOLDOWN = 30000; // 30 second cooldown

    // 性能统计数据
    private static final AtomicLong totalChecks = new AtomicLong(0);
    private static final AtomicLong slowChecks = new AtomicLong(0);
    private static final Map<String, PerformanceMetric> metrics = new ConcurrentHashMap<>();

    // TPS计算相关
    private static long[] tickTimes = new long[100];
    private static int tickIndex = 0;
    private static double lastTPS = 20.0;

    // 内存监控
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private static final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

    // 智能缓存
    private static final Map<String, SmartCache<?, ?>> caches = new ConcurrentHashMap<>();

    /**
     * 主监控循环
     */
    public static void onServerTick(MinecraftServer server) {
        long currentTime = System.currentTimeMillis();

        // TPS监控
        updateTPS(currentTime);

        // 定期检查性能
        if (currentTime - lastCheck >= MONITOR_INTERVAL * 50) {
            monitorPerformance(server);
            lastCheck = currentTime;
        }

        // 更新持续显示
        AreaVisualizer.updatePersistentVisualizations();
    }

    /**
     * 更新TPS计算
     */
    private static void updateTPS(long currentTime) {
        tickTimes[tickIndex] = currentTime;
        tickIndex = (tickIndex + 1) % tickTimes.length;

        if (tickTimes[tickIndex] != 0) {
            long elapsed = currentTime - tickTimes[tickIndex];
            if (elapsed > 0) {
                lastTPS = Math.min(20.0, (tickTimes.length * 1000.0) / elapsed);
            }
        }
    }

    /**
     * 性能监控主方法
     */
    private static void monitorPerformance(MinecraftServer server) {
        double currentTPS = getTPS();
        long memoryUsage = getMemoryUsagePercentage();

        // 记录性能指标
        recordMetric("tps", currentTPS);
        recordMetric("memory", memoryUsage);
        recordMetric("check_interval", currentCheckInterval);

        // TPS过低时进行优化
        if (currentTPS < TPS_THRESHOLD_LOW) {
            handleLowTPS(currentTPS);
        }

        // 内存使用过高时进行优化
        if (memoryUsage > MEMORY_THRESHOLD) {
            handleHighMemoryUsage();
        }

        // 记录日志
        if (currentTPS < TPS_THRESHOLD_CRITICAL || memoryUsage > 95) {
            AreaMonitorMod.LOGGER.warn("性能警告 - TPS: {:.1f}, 内存: {}%", currentTPS, memoryUsage);
        }
    }

    /**
     * 处理低TPS情况
     */
    private static void handleLowTPS(double currentTPS) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastOptimization < OPTIMIZATION_COOLDOWN) {
            return; // 冷却中
        }

        if (currentTPS < TPS_THRESHOLD_CRITICAL) {
            // 严重性能问题，激进优化
            adjustCheckInterval(currentCheckInterval + 10);
            clearAllCaches();
            triggerGC();

            AreaMonitorMod.LOGGER.warn("严重性能问题! TPS: {:.1f}, 检查间隔调整为: {} tick",
                currentTPS, currentCheckInterval);
        } else {
            // 轻微性能问题，适度优化
            adjustCheckInterval(currentCheckInterval + 2);
            clearUnusedCaches();

            AreaMonitorMod.LOGGER.info("性能优化 - TPS: {:.1f}, 检查间隔调整为: {} tick",
                currentTPS, currentCheckInterval);
        }

        lastOptimization = currentTime;
    }

    /**
     * 处理高内存使用
     */
    private static void handleHighMemoryUsage() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastOptimization < OPTIMIZATION_COOLDOWN) {
            return;
        }

        AreaMonitorMod.LOGGER.warn("内存使用过高: {}%，执行垃圾回收", getMemoryUsagePercentage());

        clearAllCaches();
        triggerGC();

        lastOptimization = currentTime;
    }

    /**
     * 调整检查间隔
     */
    private static void adjustCheckInterval(int newInterval) {
        currentCheckInterval = Math.max(MIN_CHECK_INTERVAL, Math.min(MAX_CHECK_INTERVAL, newInterval));
    }

    /**
     * 触发垃圾回收
     */
    private static void triggerGC() {
        System.gc();
        AreaMonitorMod.LOGGER.debug("已触发垃圾回收");
    }

    /**
     * 获取当前TPS
     */
    public static double getTPS() {
        return lastTPS;
    }

    /**
     * 获取内存使用百分比
     */
    public static long getMemoryUsagePercentage() {
        long used = memoryBean.getHeapMemoryUsage().getUsed();
        long max = memoryBean.getHeapMemoryUsage().getMax();
        return max > 0 ? (used * 100) / max : 0;
    }

    /**
     * 获取当前检查间隔
     */
    public static int getCurrentCheckInterval() {
        return currentCheckInterval;
    }

    /**
     * 记录性能指标
     */
    private static void recordMetric(String name, double value) {
        PerformanceMetric metric = metrics.computeIfAbsent(name, k -> new PerformanceMetric());
        metric.addValue(value);
    }

    /**
     * 获取性能统计信息
     */
    public static Map<String, String> getPerformanceStats() {
        Map<String, String> stats = new HashMap<>();

        stats.put("tps", String.format("%.1f", getTPS()));
        stats.put("memory_percent", String.valueOf(getMemoryUsagePercentage()));
        stats.put("check_interval", String.valueOf(currentCheckInterval));
        stats.put("total_checks", String.valueOf(totalChecks.get()));
        stats.put("slow_checks", String.valueOf(slowChecks.get()));

        // 计算慢检查百分比
        long total = totalChecks.get();
        long slow = slowChecks.get();
        if (total > 0) {
            stats.put("slow_check_percent", String.format("%.1f%%", (slow * 100.0) / total));
        }

        return stats;
    }

    /**
     * 性能测试方法
     */
    public static <T> T measurePerformance(String operationName, Supplier<T> operation) {
        long startTime = System.nanoTime();
        T result = operation.get();
        long endTime = System.nanoTime();

        double duration = (endTime - startTime) / 1_000_000.0; // 转换为毫秒

        totalChecks.incrementAndGet();
        if (duration > 1.0) { // 超过1ms的操作视为慢操作
            slowChecks.incrementAndGet();
        }

        recordMetric(operationName + "_duration", duration);

        return result;
    }

    /**
     * 清除所有缓存
     */
    public static void clearAllCaches() {
        caches.values().forEach(SmartCache::clear);
        AreaManager.getInstance().clearUnusedCaches();
        AreaMonitorMod.LOGGER.debug("已清除所有缓存");
    }

    /**
     * 清除未使用的缓存
     */
    public static void clearUnusedCaches() {
        caches.values().forEach(SmartCache::cleanUp);
        AreaMonitorMod.LOGGER.debug("已清理未使用的缓存");
    }

    /**
     * 注册缓存
     */
    public static <K, V> SmartCache<K, V> createCache(String name, int maxSize, long expireTime) {
        SmartCache<K, V> cache = new SmartCache<>(maxSize, expireTime);
        caches.put(name, cache);
        return cache;
    }

    /**
     * 性能度量数据类
     */
    private static class PerformanceMetric {
        private final List<Double> values = new ArrayList<>();
        private double min = Double.MAX_VALUE;
        private double max = Double.MIN_VALUE;
        private double sum = 0;

        public void addValue(double value) {
            values.add(value);
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;

            // 保持最近100个值
            if (values.size() > 100) {
                double removed = values.remove(0);
                sum -= removed;
            }
        }

        public double getAverage() {
            return values.isEmpty() ? 0 : sum / values.size();
        }

        public double getMin() {
            return values.isEmpty() ? 0 : min;
        }

        public double getMax() {
            return values.isEmpty() ? 0 : max;
        }
    }
}

/**
 * 智能缓存实现
 */
class SmartCache<K, V> {
    private final int maxSize;
    private final long expireTime;
    private final Map<K, CacheEntry<V>> cache = new LinkedHashMap<>();

    public SmartCache(int maxSize, long expireTime) {
        this.maxSize = maxSize;
        this.expireTime = expireTime;
    }

    @SuppressWarnings("unchecked")
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            entry.updateAccessTime();
            // 移动到末尾（LRU）
            cache.remove(key);
            cache.put(key, entry);
            return entry.getValue();
        }
        cache.remove(key);
        return null;
    }

    public void put(K key, V value) {
        cleanUp(); // 清理过期项

        if (cache.size() >= maxSize) {
            // 移除最久未使用的
            K oldestKey = cache.keySet().iterator().next();
            cache.remove(oldestKey);
        }

        cache.put(key, new CacheEntry<>(value, expireTime));
    }

    public void clear() {
        cache.clear();
    }

    public void cleanUp() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public int size() {
        return cache.size();
    }

    public boolean isEmpty() {
        return cache.isEmpty();
    }

    /**
     * 缓存条目
     */
    private static class CacheEntry<V> {
        private final V value;
        private final long expireTime;
        private long lastAccessTime;

        public CacheEntry(V value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
            this.lastAccessTime = System.currentTimeMillis();
        }

        public V getValue() {
            return value;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - lastAccessTime > expireTime;
        }

        public void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
}