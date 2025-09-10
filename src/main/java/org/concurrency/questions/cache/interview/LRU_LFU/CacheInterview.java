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

    public InMemoryCacheStorage(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public void put(K key, V value) {
        System.out.println("Cache: Putting key " + key + " with value " + value);
        cache.put(key, value);
    }

    @Override
    public V get(K key) {
        V value = cache.get(key);
        System.out.println("Cache: Getting key " + key + " -> " + value);
        return value;
    }

    @Override
    public void remove(K key) {
        System.out.println("Cache: Removing key " + key);
        cache.remove(key);
    }

    @Override
    public boolean containsKey(K key) {
        boolean exists = cache.containsKey(key);
        System.out.println("Cache: Contains key " + key + " -> " + exists);
        return exists;
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public Set<K> keySet() {
        return cache.keySet();
    }
}


/**
 * ❓ Q: Why do we keep a DB map here instead of a real DB?
 * ✅ A: This is a mock DB for demonstration.
 * In real systems, it could be replaced with MySQL, MongoDB, etc.
 */
class SimpleDBStorage<K, V> implements DBStorage<K, V> {
    private final Map<K, V> db = new HashMap<>();

    @Override
    public void write(K key, V value) {
        System.out.println("DB: Writing key " + key + " with value " + value);
        db.put(key, value);
    }

    @Override
    public V read(K key) {
        V value = db.get(key);
        System.out.println("DB: Reading key " + key + " -> " + value);
        return value;
    }

    @Override
    public void delete(K key) {
        System.out.println("DB: Deleting key " + key);
        db.remove(key);
    }
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
 * ✅ A: To ensure that all tasks for the same key are executed in order,
 * avoiding concurrency issues such as race conditions, inconsistent updates,
 * or double-writes. A global thread pool would allow tasks for the same key
 * to run in parallel, leading to unpredictable state changes.
 *
 * This design maps keys to specific threads (or queues) so that operations
 * affecting the same data are serialized while still allowing tasks for
 * different keys to run concurrently.
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

    public <V> CompletableFuture<V> submit(Callable<V> task, Object key) {
        int index = Math.abs(key.hashCode() % executors.length);
        ExecutorService executorService = executors[index];
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executorService);
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
        return executor.submit(() -> {
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
        }, key);
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
        // Uncomment this block to test LRU eviction

        CacheStorage<String, String> cache = new InMemoryCacheStorage<>(2);
        DBStorage<String, String> db = new SimpleDBStorage<>();
        WritePolicy<String, String> writePolicy = new WriteThroughPolicy<>();
        ReadPolicy<String, String> readPolicy = new ReadThroughPolicy<>();
        EvictionAlgorithm<String> lru = new LRUEvictionAlgorithm<>();

        Cache<String, String> cacheSystem = new Cache<>(
                cache, db, writePolicy, readPolicy, lru, 4
        );

        db.write("1", "Apple");
        db.write("2", "Banana");
        db.write("3", "Cherry");

        System.out.println("=== Testing LRU Eviction (Capacity: 2) ===");
        printSystemState(cache, db);

        System.out.println("Result: " + cacheSystem.get("1").join());
        printSystemState(cache, db);

        System.out.println("Result: " + cacheSystem.get("2").join());
        printSystemState(cache, db);

        System.out.println("Result: " + cacheSystem.get("1").join());
        printSystemState(cache, db);

        System.out.println("Result: " + cacheSystem.get("3").join());
        printSystemState(cache, db);

        System.out.println("Result: " + cacheSystem.get("2").join());
        printSystemState(cache, db);

        System.out.println("Result: " + cacheSystem.get("5").join());
        printSystemState(cache, db);

        String newKey = "10";
        String newValue = "Watermelon";
        cacheSystem.put(newKey, newValue).join();

        System.out.println("Cache check: " + ((InMemoryCacheStorage<String, String>) cache).containsKey(newKey));
        System.out.println("DB check: " + (db.read(newKey) != null));
        System.out.println("DB value: " + db.read(newKey));
        printSystemState(cache, db);

        cacheSystem.shutdown();

//        // LFU Test case
//        CacheStorage<String, String> cache = new InMemoryCacheStorage<>(2);
//        DBStorage<String, String> db = new SimpleDBStorage<>();
//        WritePolicy<String, String> writePolicy = new WriteThroughPolicy<>();
//        ReadPolicy<String, String> readPolicy = new ReadThroughPolicy<>();
//        EvictionAlgorithm<String> lfu = new LFUEvictionAlgorithm<>();
//
//        Cache<String, String> cacheSystem = new Cache<>(
//                cache, db, writePolicy, readPolicy, lfu, 4
//        );
//
//        // Initialize database with some entries
//        db.write("1", "Apple");
//        db.write("2", "Banana");
//        db.write("3", "Cherry");
//
//        System.out.println("=== Testing LFU Eviction (Capacity: 2) ===");
//        printSystemState(cache, db);
//
//        System.out.println("Result: " + cacheSystem.get("1").join());
//        printSystemState(cache, db);
//
//        System.out.println("Result: " + cacheSystem.get("2").join());
//        printSystemState(cache, db);
//
//        System.out.println("Result: " + cacheSystem.get("1").join());
//        printSystemState(cache, db);
//
//        System.out.println("Result: " + cacheSystem.get("3").join());
//        printSystemState(cache, db);
//
//        System.out.println("Result: " + cacheSystem.get("1").join());
//        printSystemState(cache, db);
//
//        System.out.println("Result: " + cacheSystem.get("3").join());
//        printSystemState(cache, db);
//
//        System.out.println("Result: " + cacheSystem.get("2").join());
//        printSystemState(cache, db);
//
//        System.out.println("=== Final Verification ===");
//        System.out.println(cache.containsKey("1"));
//        System.out.println(cache.containsKey("2"));
//        System.out.println(cache.containsKey("3"));
//
//        cacheSystem.shutdown();
    }

    private static void printSystemState(CacheStorage<String, String> cache, DBStorage<String, String> db) {
        System.out.println("Cache state: " + ((InMemoryCacheStorage<String, String>) cache).keySet());
        System.out.println();
    }
}
