package org.concurrency.questions.cache.interview;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

interface CacheStorage<K, V> {
    void put(K key, V value);
    V get(K key);
    void remove(K key);
    boolean containsKey(K key);
    int size();
    int getCapacity();
}

interface DBStorage<K, V> {
    void write(K key, V value);
    V read(K key);
    void delete(K key);
}

interface WritePolicy<K, V> {
    void write(K key, V value, CacheStorage<K, V> cache, DBStorage<K, V> db);
}

interface ReadPolicy<K, V> {
    V read(K key, CacheStorage<K, V> cache, DBStorage<K, V> db);
}

interface EvictionAlgorithm<K> {
    void keyAccessed(K key);
    K evictKey();
}

class InMemoryCacheStorage<K, V> implements CacheStorage<K, V> {
    private final Map<K, V> cache = new ConcurrentHashMap<>();
    private final int capacity;

    public InMemoryCacheStorage(int capacity) { this.capacity = capacity; }

    public void put(K key, V value) { cache.put(key, value); }
    public V get(K key) { return cache.get(key); }
    public void remove(K key) { cache.remove(key); }
    public boolean containsKey(K key) { return cache.containsKey(key); }
    public int size() { return cache.size(); }
    public int getCapacity() { return capacity; }
}

class SimpleDBStorage<K, V> implements DBStorage<K, V> {
    private final Map<K, V> db = new ConcurrentHashMap<>();
    public void write(K key, V value) { db.put(key, value); }
    public V read(K key) { return db.get(key); }
    public void delete(K key) { db.remove(key); }
}

class WriteThroughPolicy<K, V> implements WritePolicy<K, V> {
    public void write(K key, V value, CacheStorage<K, V> cache, DBStorage<K, V> db) {
        cache.put(key, value);
        db.write(key, value);
    }
}

class ReadThroughPolicy<K, V> implements ReadPolicy<K, V> {
    public V read(K key, CacheStorage<K, V> cache, DBStorage<K, V> db) {
        V value = db.read(key);
        if (value != null) {
            cache.put(key, value);
        }
        return value;
    }
}

class LRUEvictionAlgorithm<K> implements EvictionAlgorithm<K> {
    private final LinkedHashMap<K, Boolean> lruMap;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public LRUEvictionAlgorithm() {
        this.lruMap = new LinkedHashMap<>(16, 0.75f, true);
    }

    public void keyAccessed(K key) {
        lock.writeLock().lock();
        try {
            // If key exists, access it to update its position
            // If key doesn't exist, add it to the map
            if (lruMap.containsKey(key)) {
                lruMap.get(key); // Access to update order
            } else {
                lruMap.put(key, true);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public K evictKey() {
        lock.writeLock().lock();
        try {
            if (lruMap.isEmpty()) {
                return null;
            }

            // The first element is the least recently used
            Iterator<K> it = lruMap.keySet().iterator();
            K keyToEvict = it.next();
            it.remove();
            return keyToEvict;
        } finally {
            lock.writeLock().unlock();
        }
    }
}

class KeyBasedExecutor {
    private final ExecutorService[] executors;

    public KeyBasedExecutor(int pools) {
        executors = new ExecutorService[pools];
        for (int i = 0; i < pools; i++)
            executors[i] = Executors.newSingleThreadExecutor();
    }

    public CompletableFuture<Void> submit(Runnable task, Object key) {
        int index = Math.abs(key.hashCode() % executors.length);
        return CompletableFuture.runAsync(task, executors[index]);
    }

    public void shutdown() {
        for (ExecutorService ex : executors) ex.shutdown();
    }
}

class Cache<K, V> {
    private final CacheStorage<K, V> cache;
    private final DBStorage<K, V> db;
    private final WritePolicy<K, V> writePolicy;
    private final ReadPolicy<K, V> readPolicy;
    private final EvictionAlgorithm<K> eviction;
    private final KeyBasedExecutor executor;

    public Cache(CacheStorage<K, V> cache, DBStorage<K, V> db,
                 WritePolicy<K, V> writePolicy, ReadPolicy<K, V> readPolicy,
                 EvictionAlgorithm<K> eviction, int pools) {
        this.cache = cache;
        this.db = db;
        this.writePolicy = writePolicy;
        this.readPolicy = readPolicy;
        this.eviction = eviction;
        this.executor = new KeyBasedExecutor(pools);
    }

    public CompletableFuture<V> get(K key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                V value;
                if (cache.containsKey(key)) {
                    // Cache hit
                    value = cache.get(key);
                    eviction.keyAccessed(key);
                } else {
                    // Cache miss - read through to database
                    value = readPolicy.read(key, cache, db);
                    if (value != null) {
                        eviction.keyAccessed(key);
                        checkEviction();
                    }
                }
                return value;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    public CompletableFuture<Void> put(K key, V value) {
        return executor.submit(() -> {
            // Check if we need to evict before adding new item
            if (cache.size() >= cache.getCapacity()) {
                K keyToEvict = eviction.evictKey();
                if (keyToEvict != null) {
                    System.out.println("Evicting key: " + keyToEvict);
                    cache.remove(keyToEvict);
                }
            }

            writePolicy.write(key, value, cache, db);
            eviction.keyAccessed(key);
        }, key);
    }

    private void checkEviction() {
        if (cache.size() > cache.getCapacity()) {
            K keyToEvict = eviction.evictKey();
            if (keyToEvict != null) {
                System.out.println("Evicting key: " + keyToEvict);
                cache.remove(keyToEvict);
            }
        }
    }

    public void shutdown() { executor.shutdown(); }
}

public class CacheInterview {
    public static void main(String[] args) {
        CacheStorage<String, String> cache = new InMemoryCacheStorage<>(2);
        DBStorage<String, String> db = new SimpleDBStorage<>();
        WritePolicy<String, String> writePolicy = new WriteThroughPolicy<>();
        ReadPolicy<String, String> readPolicy = new ReadThroughPolicy<>();
        EvictionAlgorithm<String> lru = new LRUEvictionAlgorithm<>();

        Cache<String, String> cacheSystem = new Cache<>(
                cache, db, writePolicy, readPolicy, lru, 4
        );

        // Initialize database
        db.write("1", "Apple");
        db.write("2", "Banana");
        db.write("3", "Cherry");
        db.write("4", "Date"); // Added for completeness

        System.out.println("=== Testing Read-Through Cache (Capacity: 2) ===");
        System.out.println("Initial DB contents: {1=Apple, 2=Banana, 3=Cherry, 4=Date}");
        System.out.println();

        // Test read-through functionality
        System.out.println("1. First access to key '1' (Cache Miss → Read from DB)");
        System.out.println("   Result: " + cacheSystem.get("1").join());
        System.out.println("   Cache state: " + getCacheState(cache));
        System.out.println();

        System.out.println("2. First access to key '2' (Cache Miss → Read from DB)");
        System.out.println("   Result: " + cacheSystem.get("2").join());
        System.out.println("   Cache state: " + getCacheState(cache));
        System.out.println();

        System.out.println("3. Access to key '1' again (Cache Hit)");
        System.out.println("   Result: " + cacheSystem.get("1").join());
        System.out.println("   Cache state: " + getCacheState(cache));
        System.out.println();

        System.out.println("4. First access to key '3' (Cache Full → Evict LRU key '2')");
        System.out.println("   Result: " + cacheSystem.get("3").join());
        System.out.println("   Cache state: " + getCacheState(cache));
        System.out.println();

        System.out.println("5. Access to key '2' again (Cache Miss → Read from DB, Evict LRU key '1')");
        System.out.println("   Result: " + cacheSystem.get("2").join());
        System.out.println("   Cache state: " + getCacheState(cache));
        System.out.println();

        System.out.println("6. Access to non-existent key '5' (Not in DB)");
        System.out.println("   Result: " + cacheSystem.get("5").join());
        System.out.println("   Cache state: " + getCacheState(cache));
        System.out.println();

        System.out.println("=== Testing Write-Through Cache ===");
        System.out.println("7. Updating existing key '1' (Write to Cache & DB, Evict LRU key '3')");
        cacheSystem.put("1", "Avocado").join();
        System.out.println("   Result: " + cacheSystem.get("1").join());
        System.out.println("   Cache state: " + getCacheState(cache));
        System.out.println();

        System.out.println("8. Adding new key '4' (Write to Cache & DB, Evict LRU key '2')");
        cacheSystem.put("4", "Date").join();
        System.out.println("   Result: " + cacheSystem.get("4").join());
        System.out.println("   Cache state: " + getCacheState(cache));
        System.out.println();

        System.out.println("Final DB contents: {1=Avocado, 2=Banana, 3=Cherry, 4=Date}");
        cacheSystem.shutdown();
    }

    // Helper method to get the current state of the cache
    private static String getCacheState(CacheStorage<String, String> cache) {
        try {
            // This is a bit of a hack since we don't have a way to iterate through the cache
            // In a real implementation, we might add a method to get all keys
            return "Cache contains " + cache.size() + " items";
        } catch (Exception e) {
            return "Error getting cache state";
        }
    }
}