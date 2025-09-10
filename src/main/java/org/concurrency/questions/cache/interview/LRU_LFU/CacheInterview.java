package org.concurrency.questions.cache.interview.LRU_LFU;

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
 *
 * 📌 Notes:
 * 1. Currently, this design focuses on a single-node in-memory cache.
 * 2. ReadThroughCachePolicy:
 *    → Read from cache; if missed, read from database, update cache, and return the result.
 * 3. WriteThroughCachePolicy:
 *    → Write to both cache and database simultaneously.
 * 4. LRU Eviction Algorithm is implemented as the eviction policy.
 *
 * 📦 Future Action Items:
 * - Make the cache distributed using consistent hashing and support cache replication.
 * - Implement different eviction strategies (e.g., LFU, TTL-based eviction).
 * - Implement Cache Aside read strategy.
 * - Support additional write policies like Write Back and Write Around.
 *
 * 📦 Key Components:
 * - CacheStorage & DBStorage interfaces for cache and database operations.
 * - WritePolicy and read policy interface for different write and read strategies.
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
 * This allows us to swap implementations without changing the cache logic (OCP - Open/Closed Principle).
 */
interface CacheStorage<K, V> {
    void put(K key, V value);
    V get(K key);
    void remove(K key);
    boolean containsKey(K key);
    int size();
    int getCapacity();
    Set<K> keySet();
}

/**
 * ❓ Q: Why do we need DBStorage separately when we already have cache?
 * ✅ A: The DB acts as persistent storage (truth source). Cache is faster but volatile.
 * Keeping them separate enforces abstraction and allows switching between database types easily.
 */
interface DBStorage<K, V> {
    void write(K key, V value);
    V read(K key);
    void delete(K key);
}

/**
 * ❓ Q: Why do we need different WritePolicy strategies?
 * ✅ A: To support multiple policies:
 * - Write-through: write to cache + DB together
 * - Write-back: write to cache first, DB later
 * This is Strategy Pattern in action.
 */
interface WritePolicy<K, V> {
    void write(K key, V value, CacheStorage<K, V> cache, DBStorage<K, V> db);
}

/**
 * ❓ Q: Why do we need a ReadPolicy interface?
 * ✅ A: It decouples how reads are handled.
 * - Read-through: fetch from DB on cache miss
 * - Read-around: bypass cache for some reads
 * This gives flexibility.
 */
interface ReadPolicy<K, V> {
    V read(K key, CacheStorage<K, V> cache, DBStorage<K, V> db);
}

/**
 * ❓ Q: Why do we need an EvictionAlgorithm?
 * ✅ A: Since cache has limited capacity, we need eviction strategies (LRU, LFU, FIFO).
 * This again uses Strategy Pattern for plug-and-play eviction.
 */
interface EvictionAlgorithm<K> {
    void keyAccessed(K key);
    K evictKey();
}

/**
 * ❓ Q: Why use ConcurrentHashMap here instead of HashMap?
 * ✅ A: Cache must be thread-safe since multiple threads may read/write simultaneously.
 * ConcurrentHashMap provides high performance with fine-grained locking.
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
    public Set<K> keySet() { return cache.keySet(); }
}

/**
 * ❓ Q: Why do we keep a DB map here instead of a real DB?
 * ✅ A: This is a mock DB for demonstration.
 * In real systems, it could be replaced with MySQL, MongoDB, etc.
 */
class SimpleDBStorage<K, V> implements DBStorage<K, V> {
    private final Map<K, V> db = new HashMap<>();
    public void write(K key, V value) { db.put(key, value); }
    public V read(K key) { return db.get(key); }
    public void delete(K key) { db.remove(key); }
}

/**
 * ❓ Q: Why write to both cache and DB together?
 * ✅ A: To ensure consistency. In case of crash, data is already in DB.
 * Downside: higher write latency compared to write-back.
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
        V value = cache.get(key);
        if (value == null) {
            // Cache miss – read from database
            value = db.read(key);
            if (value != null) {
                // Update cache with value from database
                cache.put(key, value);
            }
        }
        return value;
    }
}

/**
 * ❓ Q: Why LinkedHashMap for LRU?
 * ✅ A: LinkedHashMap maintains insertion/access order, making it easy to evict the least recently used entry.
 * We also use ReentrantReadWriteLock for thread-safety.
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
// only Implement when asked
class LFUEvictionAlgorithm<K> implements EvictionAlgorithm<K> {
    /**
     * ❓ Q: Why use multiple data structures (keyToFreq, freqToKeys, TreeSet) for LFU?
     * ✅ A:
     * - keyToFreq: Tracks current frequency of each key for O(1) access.
     * - freqToKeys: Groups keys by frequency to quickly access all keys at a certain frequency.
     * - TreeSet: Maintains frequencies in sorted order to efficiently find the minimum frequency for eviction.
     * This structure ensures O(1) access for key updates and O(log n) eviction due to the TreeSet.
     */
    private final Map<K, Integer> keyToFreq;
    private final Map<Integer, LinkedHashSet<K>> freqToKeys;
    private final TreeSet<Integer> frequencies;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public LFUEvictionAlgorithm() {
        keyToFreq = new HashMap<>();
        freqToKeys = new HashMap<>();
        frequencies = new TreeSet<>();
    }



    /**
     * ❓ Q: How does key access update frequency while maintaining O(1) complexity?
     * ✅ A:
     * 1. For existing keys:
     *    - Remove key from current frequency group (O(1) using hash map lookup).
     *    - Increment frequency and add to new group (O(1) for map operations, O(log n) for TreeSet update).
     * 2. For new keys: Initialize frequency to 1 and add to corresponding groups (O(1) for maps, O(log n) for TreeSet).
     */
    @Override
    public void keyAccessed(K key) {
        lock.writeLock().lock();
        try {
            if (keyToFreq.containsKey(key)) {
                // Existing key: update frequency
                int oldFreq = keyToFreq.get(key);
                int newFreq = oldFreq + 1;
                keyToFreq.put(key, newFreq);

                // Remove from old frequency set
                LinkedHashSet<K> oldSet = freqToKeys.get(oldFreq);
                oldSet.remove(key);
                if (oldSet.isEmpty()) {
                    freqToKeys.remove(oldFreq);
                    frequencies.remove(oldFreq);
                }

                // Add to new frequency set
                freqToKeys.computeIfAbsent(newFreq, k -> new LinkedHashSet<>()).add(key);
                frequencies.add(newFreq);
            } else {
                // New key: initialize frequency
                keyToFreq.put(key, 1);
                freqToKeys.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(key);
                frequencies.add(1);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * ❓ Q: Why evict from the smallest frequency group, and how is LRU handled within the same frequency?
     * ✅ A:
     * - LFU evicts keys with the smallest frequency (frequencies.first() from TreeSet).
     * - Within the same frequency, LinkedHashSet preserves insertion order. The first element in the set (iterator.next()) is the least recently used in that frequency group.
     */
    @Override
    public K evictKey() {
        lock.writeLock().lock();
        try {
            if (frequencies.isEmpty()) {
                return null;
            }

            int minFreq = frequencies.first();
            LinkedHashSet<K> set = freqToKeys.get(minFreq);
            K keyToEvict = set.iterator().next();

            // Remove key from all structures
            set.remove(keyToEvict);
            keyToFreq.remove(keyToEvict);
            if (set.isEmpty()) {
                freqToKeys.remove(minFreq);
                frequencies.remove(minFreq);
            }
            return keyToEvict;
        } finally {
            lock.writeLock().unlock();
        }
    }
}

/**
 * ❓ Q: Why use KeyBasedExecutor instead of a global thread pool?
 * ✅ A: It ensures all operations for the same key are serialized
 * (avoiding race conditions like double-writes).
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
 * It hides complexity (storage, eviction, executor, read/write policy).
 * This design follows the Facade + Strategy patterns.
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
     * In distributed systems, cache/DB reads may be network calls → async improves performance.
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
 * Interviewers usually ask to show test cases.
 */
public class CacheInterview {
    public static void main(String[] args) {
        // Lru test case
//        CacheStorage<String, String> cache = new InMemoryCacheStorage<>(2);
//        DBStorage<String, String> db = new SimpleDBStorage<>();
//        WritePolicy<String, String> writePolicy = new WriteThroughPolicy<>();
//        ReadPolicy<String, String> readPolicy = new ReadThroughPolicy<>();
//        EvictionAlgorithm<String> lru = new LRUEvictionAlgorithm<>();
//
//        Cache<String, String> cacheSystem = new Cache<>(
//                cache, db, writePolicy, readPolicy, lru, 4
//        );
//
//        // Initialize DB with some data
//        db.write("1", "Apple");
//        db.write("2", "Banana");
//        db.write("3", "Cherry");
//
//        System.out.println("=== Testing Read-Through & Eviction (Capacity: 2) ===");
//        printSystemState(cache, db);
//
//        System.out.println("1. First access to key '1' (Cache Miss -> DB)");
//        System.out.println("   Result: " + cacheSystem.get("1").join());
//        printSystemState(cache, db);
//
//        System.out.println("2. First access to key '2' (Cache Miss -> DB)");
//        System.out.println("   Result: " + cacheSystem.get("2").join());
//        printSystemState(cache, db);
//
//        System.out.println("3. Access key '1' again (Cache Hit)");
//        System.out.println("   Result: " + cacheSystem.get("1").join());
//        printSystemState(cache, db);
//
//        System.out.println("4. Access key '3' (Cache Full -> Evict LRU '2')");
//        System.out.println("   Result: " + cacheSystem.get("3").join());
//        printSystemState(cache, db);
//
//        System.out.println("5. Access key '2' again (Miss -> DB, Evict '1')");
//        System.out.println("   Result: " + cacheSystem.get("2").join());
//        printSystemState(cache, db);
//
//        System.out.println("6. Access non-existent key '5' (Read Miss, not in DB)");
//        System.out.println("   Result: " + cacheSystem.get("5").join());
//        printSystemState(cache, db);
//
//        System.out.println("=== Testing Write-Through on New Key ===");
//        // The key '10' does not exist in the database initially
//        String newKey = "10";
//        String newValue = "Watermelon";
//        System.out.println("7. Put new key '" + newKey + "' into cache...");
//        cacheSystem.put(newKey, newValue).join();
//
//        System.out.println("   Verifying key '" + newKey + "' exists in cache and DB...");
//        System.out.println("   Cache check: " + ((InMemoryCacheStorage) cache).containsKey(newKey));
//        System.out.println("   DB check: " + (db.read(newKey) != null));
//        System.out.println("   DB value: " + db.read(newKey));
//        printSystemState(cache, db);

//        cacheSystem.shutdown();


        // LFU Test
        CacheStorage<String, String> cache = new InMemoryCacheStorage<>(2);
        DBStorage<String, String> db = new SimpleDBStorage<>();
        WritePolicy<String, String> writePolicy = new WriteThroughPolicy<>();
        ReadPolicy<String, String> readPolicy = new ReadThroughPolicy<>();
        EvictionAlgorithm<String> lfu = new LFUEvictionAlgorithm<>();

        Cache<String, String> cacheSystem = new Cache<>(
                cache, db, writePolicy, readPolicy, lfu, 4
        );

        // Initialize DB with some data
        db.write("1", "Apple");
        db.write("2", "Banana");
        db.write("3", "Cherry");

        System.out.println("=== Testing LFU Eviction (Capacity: 2) ===");
        printSystemState(cache, db);

        System.out.println("1. Access key '1' (Cache Miss -> DB, freq=1)");
        System.out.println("   Result: " + cacheSystem.get("1").join());
        printSystemState(cache, db);

        System.out.println("2. Access key '2' (Cache Miss -> DB, freq=1)");
        System.out.println("   Result: " + cacheSystem.get("2").join());
        printSystemState(cache, db);

        System.out.println("3. Access key '1' again (Cache Hit, freq=2)");
        System.out.println("   Result: " + cacheSystem.get("1").join());
        printSystemState(cache, db);

        System.out.println("4. Access key '3' (Cache Full -> Evict '2' (freq=1))");
        System.out.println("   Result: " + cacheSystem.get("3").join());
        printSystemState(cache, db);

        System.out.println("5. Access key '1' again (Cache Hit, freq=3)");
        System.out.println("   Result: " + cacheSystem.get("1").join());
        printSystemState(cache, db);

        System.out.println("6. Access key '3' again (Cache Hit, freq=2)");
        System.out.println("   Result: " + cacheSystem.get("3").join());
        printSystemState(cache, db);

        System.out.println("7. Access key '2' again (Miss -> DB, Cache Full -> Evict '3' (freq=2))");
        System.out.println("   Result: " + cacheSystem.get("2").join());
        printSystemState(cache, db);

        System.out.println("=== Final Verification ===");
        System.out.println("Cache contains key '1': " + cache.containsKey("1")); // true
        System.out.println("Cache contains key '2': " + cache.containsKey("2")); // true
        System.out.println("Cache contains key '3': " + cache.containsKey("3")); // false

        cacheSystem.shutdown();
    }

    private static void printSystemState(CacheStorage<String, String> cache, DBStorage<String, String> db) {
        System.out.println("   Cache state: " + ((InMemoryCacheStorage) cache).keySet());
        // SimpleDBStorage doesn't have a public keySet method, so we will not display its contents.
        // It is checked in the explicit test case
        System.out.println();
    }
}