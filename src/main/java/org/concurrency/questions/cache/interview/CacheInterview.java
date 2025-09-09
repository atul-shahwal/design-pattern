package org.concurrency.questions.cache.interview;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
/**
 * 🎯 Problem Statement: Low-Level Design - Cache System
 *
 * Design a thread-safe cache that supports various eviction algorithms, write policies,
 * and ensures consistency between in-memory cache and persistent storage.
 *
 * ✅ Requirements:
 * - Configurable capacity with eviction when full (e.g., LRU).
 * - Write-through strategy for cache and database synchronization.
 * - Handle concurrent access efficiently using per-key execution.
 * - Support for scaling and multiple eviction strategies.
 *
 * 📦 Key Components:
 * - CacheStorage & DBStorage interfaces for cache and database operations.
 * - WritePolicy interface for different write strategies.
 * - EvictionAlgorithm interface for eviction mechanisms like LRU.
 * - KeyBasedExecutor to ensure per-key thread safety.
 *
 * 🚀 Example Flow:
 * 1. Accessing a key updates its access record.
 * 2. Insertion triggers eviction if capacity exceeded.
 * 3. Writes update both cache and database asynchronously.
 */
/**
 * ❓ Interview Q: Why do we need this interface?
 * ✅ A: It defines the contract for any cache storage implementation (e.g., in-memory, Redis, file-based).
 *     This allows us to swap implementations without changing the cache logic (OCP - Open/Closed Principle).
 */
interface CacheStorage<K, V> {
    void put(K key, V value);
    V get(K key);
    void remove(K key);
    boolean containsKey(K key);
    int size();
    int getCapacity();
}

/**
 * ❓ Q: Why do we need DBStorage separately when we already have cache?
 * ✅ A: The DB acts as persistent storage (truth source). Cache is faster but volatile.
 *     Keeping them separate enforces abstraction and allows switching between database types easily.
 */
interface DBStorage<K, V> {
    void write(K key, V value);
    V read(K key);
    void delete(K key);
}

/**
 * ❓ Q: Why do we need different WritePolicy strategies?
 * ✅ A: To support multiple policies:
 *       - Write-through: write to cache + DB together
 *       - Write-back: write to cache first, DB later
 *     This is Strategy Pattern in action.
 */
interface WritePolicy<K, V> {
    void write(K key, V value, CacheStorage<K, V> cache, DBStorage<K, V> db);
}

/**
 * ❓ Q: Why do we need a ReadPolicy interface?
 * ✅ A: It decouples how reads are handled.
 *     - Read-through: fetch from DB on cache miss
 *     - Read-around: bypass cache for some reads
 *     This gives flexibility.
 */
interface ReadPolicy<K, V> {
    V read(K key, CacheStorage<K, V> cache, DBStorage<K, V> db);
}

/**
 * ❓ Q: Why do we need an EvictionAlgorithm?
 * ✅ A: Since cache has limited capacity, we need eviction strategies (LRU, LFU, FIFO).
 *     This again uses Strategy Pattern for plug-and-play eviction.
 */
interface EvictionAlgorithm<K> {
    void keyAccessed(K key);
    K evictKey();
}

/**
 * ❓ Q: Why use ConcurrentHashMap here instead of HashMap?
 * ✅ A: Cache must be thread-safe since multiple threads may read/write simultaneously.
 *     ConcurrentHashMap provides high performance with fine-grained locking.
 */
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

/**
 * ❓ Q: Why do we keep a DB map here instead of a real DB?
 * ✅ A: This is a mock DB for demonstration.
 *     In real systems, it could be replaced with MySQL, MongoDB, etc.
 */
class SimpleDBStorage<K, V> implements DBStorage<K, V> {
    private final Map<K, V> db = new ConcurrentHashMap<>();
    public void write(K key, V value) { db.put(key, value); }
    public V read(K key) { return db.get(key); }
    public void delete(K key) { db.remove(key); }
}

/**
 * ❓ Q: Why write to both cache and DB together?
 * ✅ A: To ensure consistency. In case of crash, data is already in DB.
 *     Downside: higher write latency compared to write-back.
 */
class WriteThroughPolicy<K, V> implements WritePolicy<K, V> {
    public void write(K key, V value, CacheStorage<K, V> cache, DBStorage<K, V> db) {
        cache.put(key, value);
        db.write(key, value);
    }
}

/**
 * ❓ Q: Why update cache on a read miss?
 * ✅ A: To improve future read performance. This avoids repeated DB hits for the same key.
 */
class ReadThroughPolicy<K, V> implements ReadPolicy<K, V> {
    public V read(K key, CacheStorage<K, V> cache, DBStorage<K, V> db) {
        V value = db.read(key);
        if (value != null) {
            cache.put(key, value);
        }
        return value;
    }
}

/**
 * ❓ Q: Why LinkedHashMap for LRU?
 * ✅ A: LinkedHashMap maintains insertion/access order, making it easy to evict the least recently used entry.
 *     We also use ReentrantReadWriteLock for thread-safety.
 */
class LRUEvictionAlgorithm<K> implements EvictionAlgorithm<K> {
    private final LinkedHashMap<K, Boolean> lruMap;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public LRUEvictionAlgorithm() {
        this.lruMap = new LinkedHashMap<>(16, 0.75f, true);
    }

    public void keyAccessed(K key) {
        lock.writeLock().lock();
        try {
            if (lruMap.containsKey(key)) {
                lruMap.get(key); // Touch to update access order
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
            if (lruMap.isEmpty()) return null;

            Iterator<K> it = lruMap.keySet().iterator();
            K keyToEvict = it.next();
            it.remove();
            return keyToEvict;
        } finally {
            lock.writeLock().unlock();
        }
    }
}

/**
 * ❓ Q: Why use KeyBasedExecutor instead of a global thread pool?
 * ✅ A: It ensures all operations for the same key are serialized
 *     (avoiding race conditions like double-writes).
 */
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

/**
 * ❓ Q: Why does this Cache class bring everything together?
 * ✅ A: This is the main façade for users.
 *     It hides complexity (storage, eviction, executor, read/write policy).
 *     This design follows the Facade + Strategy patterns.
 */
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

    /**
     * ❓ Q: Why return CompletableFuture instead of direct value?
     * ✅ A: To support async operations.
     *     In distributed systems, cache/DB reads may be network calls → async improves performance.
     */
    public CompletableFuture<V> get(K key) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                V value;
                if (cache.containsKey(key)) {
                    value = cache.get(key);
                    eviction.keyAccessed(key);
                } else {
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

    /**
     * ❓ Q: Why evict before writing new entry?
     * ✅ A: To ensure cache never exceeds its capacity.
     */
    public CompletableFuture<Void> put(K key, V value) {
        return executor.submit(() -> {
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

/**
 * ❓ Q: Why is main() needed in interview?
 * ✅ A: To demonstrate working example of the designed system.
 *     Interviewers usually ask to show test cases.
 */
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

        // Initialize DB
        db.write("1", "Apple");
        db.write("2", "Banana");
        db.write("3", "Cherry");
        db.write("4", "Date");

        System.out.println("=== Testing Read-Through Cache (Capacity: 2) ===");
        System.out.println("Initial DB contents: {1=Apple, 2=Banana, 3=Cherry, 4=Date}");
        System.out.println();

        System.out.println("1. First access to key '1' (Cache Miss → DB)");
        System.out.println("   Result: " + cacheSystem.get("1").join());
        System.out.println("   Cache state: " + getCacheState(cache));
        System.out.println();

        System.out.println("2. First access to key '2' (Cache Miss → DB)");
        System.out.println("   Result: " + cacheSystem.get("2").join());
        System.out.println("   Cache state: " + getCacheState(cache));
        System.out.println();

        System.out.println("3. Access key '1' again (Cache Hit)");
        System.out.println("   Result: " + cacheSystem.get("1").join());
        System.out.println("   Cache state: " + getCacheState(cache));
        System.out.println();

        System.out.println("4. Access key '3' (Cache Full → Evict LRU '2')");
        System.out.println("   Result: " + cacheSystem.get("3").join());
        System.out.println("   Cache state: " + getCacheState(cache));
        System.out.println();

        System.out.println("5. Access key '2' again (Miss → DB, Evict '1')");
        System.out.println("   Result: " + cacheSystem.get("2").join());
        System.out.println("   Cache state: " + getCacheState(cache));
        System.out.println();

        System.out.println("6. Access non-existent key '5'");
        System.out.println("   Result: " + cacheSystem.get("5").join());
        System.out.println("   Cache state: " + getCacheState(cache));
        System.out.println();

        System.out.println("=== Testing Write-Through Cache ===");
        System.out.println("7. Update key '1' (Write Cache & DB, Evict '3')");
        cacheSystem.put("1", "Avocado").join();
        System.out.println("   Result: " + cacheSystem.get("1").join());
        System.out.println("   Cache state: " + getCacheState(cache));
        System.out.println();

        System.out.println("8. Add new key '4' (Write Cache & DB, Evict '2')");
        cacheSystem.put("4", "Date").join();
        System.out.println("   Result: " + cacheSystem.get("4").join());
        System.out.println("   Cache state: " + getCacheState(cache));
        System.out.println();

        System.out.println("Final DB contents: {1=Avocado, 2=Banana, 3=Cherry, 4=Date}");
        cacheSystem.shutdown();
    }

    private static String getCacheState(CacheStorage<String, String> cache) {
        return "Cache contains " + cache.size() + " items";
    }
}
