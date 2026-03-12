package com.kavinshi.areamonitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Smart cache implementation with expiration and LRU eviction.
 * Thread-safe implementation using ConcurrentHashMap.
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public class SmartCache<K, V> {
    private final int maxSize;
    private final long expireTime;
    private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final Object lruLock = new Object();

    public SmartCache(int maxSize, long expireTime) {
        this.maxSize = maxSize;
        this.expireTime = expireTime;
    }

    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            entry.updateAccessTime();
            return entry.getValue();
        }
        cache.remove(key);
        return null;
    }

    public void put(K key, V value) {
        cleanUp();

        if (cache.size() >= maxSize) {
            synchronized (lruLock) {
                if (cache.size() >= maxSize) {
                    K oldestKey = findOldestKey();
                    if (oldestKey != null) {
                        cache.remove(oldestKey);
                    }
                }
            }
        }

        cache.put(key, new CacheEntry<>(value, expireTime));
    }

    private K findOldestKey() {
        synchronized (lruLock) {
            K oldestKey = null;
            long oldestTime = Long.MAX_VALUE;
            
            for (Map.Entry<K, CacheEntry<V>> entry : cache.entrySet()) {
                long accessTime = entry.getValue().getLastAccessTime();
                if (accessTime < oldestTime) {
                    oldestTime = accessTime;
                    oldestKey = entry.getKey();
                }
            }
            
            return oldestKey;
        }
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
     * Cache entry with expiration tracking.
     */
    private static class CacheEntry<V> {
        private final V value;
        private final long expireTime;
        private volatile long lastAccessTime;

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

        public long getLastAccessTime() {
            return lastAccessTime;
        }
    }
}
