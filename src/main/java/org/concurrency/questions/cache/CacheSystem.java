package org.concurrency.questions.cache;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

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

// Cache Storage Interface
interface CacheStorage<K, V> {
    void put(K key, V value) throws Exception;
    V get(K key) throws Exception;
    void remove(K key) throws Exception;
    boolean containsKey(K key) throws Exception;
    int size() throws Exception;
    int getCapacity();
}

// Database Storage Interface
interface DBStorage<K, V> {
    void write(K key, V value) throws Exception;
    V read(K key) throws Exception;
    void delete(K key) throws Exception;
}

// Write Policy Interface (Strategy Pattern)
interface WritePolicy<K, V> {
    void write(K key, V value, CacheStorage<K, V> cacheStorage, DBStorage<K, V> dbStorage) throws Exception;
}

// Read Policy Interface (Strategy Pattern)
interface ReadPolicy<K, V> {
    V read(K key, CacheStorage<K, V> cacheStorage, DBStorage<K, V> dbStorage) throws Exception;
}

// Eviction Algorithm Interface (Strategy Pattern)
interface EvictionAlgorithm<K> {
    void keyAccessed(K key) throws Exception;
    K evictKey() throws Exception;
}

// In-Memory Cache Storage Implementation
class InMemoryCacheStorage<K, V> implements CacheStorage<K, V> {
    private final Map<K, V> cache;
    private final int capacity;
    private final Object sizeLock = new Object();

    public InMemoryCacheStorage(int capacity) {
        this.capacity = capacity;
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public void put(K key, V value) throws Exception {
        System.out.println("Cache: Putting key=" + key + ", value=" + value);
        cache.put(key, value);
    }

    @Override
    public V get(K key) throws Exception {
        V value = cache.get(key);
        System.out.println("Cache: Getting key=" + key + ", value=" + value);
        return value;
    }

    @Override
    public void remove(K key) throws Exception {
        System.out.println("Cache: Removing key=" + key);
        cache.remove(key);
    }

    @Override
    public boolean containsKey(K key) throws Exception {
        boolean contains = cache.containsKey(key);
        System.out.println("Cache: Checking if contains key=" + key + ": " + contains);
        return contains;
    }

    @Override
    public int size() throws Exception {
        synchronized (sizeLock) {
            int size = cache.size();
            System.out.println("Cache: Current size=" + size);
            return size;
        }
    }

    @Override
    public int getCapacity() {
        return capacity;
    }
}

// Simple Database Storage Implementation
class SimpleDBStorage<K, V> implements DBStorage<K, V> {
    private final Map<K, V> database = new ConcurrentHashMap<>();

    @Override
    public void write(K key, V value) throws Exception {
        System.out.println("DB: Writing key=" + key + ", value=" + value);
        database.put(key, value);
    }

    @Override
    public V read(K key) throws Exception {
        V value = database.get(key);
        System.out.println("DB: Reading key=" + key + ", value=" + value);
        return value;
    }

    @Override
    public void delete(K key) throws Exception {
        System.out.println("DB: Deleting key=" + key);
        database.remove(key);
    }
}

// Read-Through Policy Implementation
class ReadThroughPolicy<K, V> implements ReadPolicy<K, V> {
    @Override
    public V read(K key, CacheStorage<K, V> cacheStorage, DBStorage<K, V> dbStorage) throws Exception {
        System.out.println("ReadThroughPolicy: Reading key=" + key + " from DB");
        V value = dbStorage.read(key);
        if (value != null) {
            System.out.println("ReadThroughPolicy: Caching key=" + key + " after DB read");
            cacheStorage.put(key, value);
        } else {
            System.out.println("ReadThroughPolicy: Key=" + key + " not found in DB");
        }
        return value;
    }
}

// Write-Through Policy Implementation
class WriteThroughPolicy<K, V> implements WritePolicy<K, V> {
    @Override
    public void write(K key, V value, CacheStorage<K, V> cacheStorage, DBStorage<K, V> dbStorage) throws Exception {
        System.out.println("WriteThroughPolicy: Writing key=" + key + " to cache and DB");

        CompletableFuture<Void> cacheFuture = CompletableFuture.runAsync(() -> {
            try {
                cacheStorage.put(key, value);
            } catch (Exception e) {
                System.err.println("Error writing to cache: " + e.getMessage());
                throw new CompletionException(e);
            }
        });

        CompletableFuture<Void> dbFuture = CompletableFuture.runAsync(() -> {
            try {
                dbStorage.write(key, value);
            } catch (Exception e) {
                System.err.println("Error writing to DB: " + e.getMessage());
                throw new CompletionException(e);
            }
        });

        CompletableFuture.allOf(cacheFuture, dbFuture).join();
        System.out.println("WriteThroughPolicy: Completed writing key=" + key);
    }
}

// Doubly Linked List Node for LRU
class DoublyLinkedListNode<K> {
    private final K value;
    DoublyLinkedListNode<K> prev;
    DoublyLinkedListNode<K> next;

    public DoublyLinkedListNode(K value) {
        this.value = value;
        this.prev = null;
        this.next = null;
    }

    public K getValue() {
        return value;
    }
}

// Doubly Linked List for LRU
class DoublyLinkedList<K> {
    private DoublyLinkedListNode<K> head;
    private DoublyLinkedListNode<K> tail;

    public DoublyLinkedList() {
        this.head = null;
        this.tail = null;
    }

    public void addNodeAtTail(DoublyLinkedListNode<K> node) {
        if (tail == null) {
            head = node;
            tail = node;
        } else {
            tail.next = node;
            node.prev = tail;
            tail = node;
        }
        node.next = null;
    }

    public void detachNode(DoublyLinkedListNode<K> node) {
        if (node == null) return;
        if (node.prev != null) node.prev.next = node.next;
        else head = node.next;
        if (node.next != null) node.next.prev = node.prev;
        else tail = node.prev;
        node.prev = null;
        node.next = null;
    }

    public DoublyLinkedListNode<K> getHead() {
        return head;
    }

    public void removeHead() {
        if (head != null) {
            if (head.next != null) {
                head = head.next;
                head.prev = null;
            } else {
                head = null;
                tail = null;
            }
        }
    }
}

// LRU Eviction Algorithm Implementation
class LRUEvictionAlgorithm<K> implements EvictionAlgorithm<K> {
    private final DoublyLinkedList<K> dll;
    private final Map<K, DoublyLinkedListNode<K>> keyToNodeMap;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public LRUEvictionAlgorithm() {
        this.dll = new DoublyLinkedList<>();
        this.keyToNodeMap = new HashMap<>();
    }

    @Override
    public void keyAccessed(K key) throws Exception {
        lock.writeLock().lock();
        try {
            System.out.println("LRU: Key accessed: " + key);
            if (keyToNodeMap.containsKey(key)) {
                DoublyLinkedListNode<K> node = keyToNodeMap.get(key);
                dll.detachNode(node);
                dll.addNodeAtTail(node);
                System.out.println("LRU: Moved key=" + key + " to MRU position");
            } else {
                DoublyLinkedListNode<K> newNode = new DoublyLinkedListNode<>(key);
                dll.addNodeAtTail(newNode);
                keyToNodeMap.put(key, newNode);
                System.out.println("LRU: Added key=" + key + " to MRU position");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public K evictKey() throws Exception {
        lock.writeLock().lock();
        try {
            DoublyLinkedListNode<K> nodeToEvict = dll.getHead();
            if (nodeToEvict == null) {
                System.out.println("LRU: No keys to evict");
                return null;
            }
            K evictKey = nodeToEvict.getValue();
            dll.removeHead();
            keyToNodeMap.remove(evictKey);
            System.out.println("LRU: Evicting key=" + evictKey);
            return evictKey;
        } finally {
            lock.writeLock().unlock();
        }
    }
}

// Key-Based Executor for Thread Affinity
class KeyBasedExecutor {
    private final ExecutorService[] executors;
    private final int numExecutors;

    public KeyBasedExecutor(int numExecutors) {
        this.numExecutors = numExecutors;
        this.executors = new ExecutorService[numExecutors];
        for (int i = 0; i < numExecutors; i++) {
            executors[i] = Executors.newSingleThreadExecutor();
        }
    }

    public <T> CompletableFuture<T> submitTask(Object key, Supplier<T> task) {
        int index = getExecutorIndexForKey(key);
        ExecutorService executor = executors[index];
        System.out.println("KeyBasedExecutor: Processing key=" + key + " on executor-" + index);
        return CompletableFuture.supplyAsync(task, executor);
    }

    public int getExecutorIndexForKey(Object key) {
        return Math.abs(key.hashCode() % numExecutors);
    }

    public void shutdown() {
        for (ExecutorService executor : executors) {
            executor.shutdown();
        }
    }
}

// Main Cache Class
class Cache<K, V> {
    private final CacheStorage<K, V> cacheStorage;
    private final DBStorage<K, V> dbStorage;
    private final WritePolicy<K, V> writePolicy;
    private final ReadPolicy<K, V> readPolicy;
    private final EvictionAlgorithm<K> evictionAlgorithm;
    private final KeyBasedExecutor keyBasedExecutor;

    public Cache(CacheStorage<K, V> cacheStorage, DBStorage<K, V> dbStorage,
                 WritePolicy<K, V> writePolicy, ReadPolicy<K, V> readPolicy,
                 EvictionAlgorithm<K> evictionAlgorithm, int numExecutors) {
        this.cacheStorage = cacheStorage;
        this.dbStorage = dbStorage;
        this.writePolicy = writePolicy;
        this.readPolicy = readPolicy;
        this.evictionAlgorithm = evictionAlgorithm;
        this.keyBasedExecutor = new KeyBasedExecutor(numExecutors);
        System.out.println("Cache: Initialized with capacity=" + cacheStorage.getCapacity());
    }

    public CompletableFuture<V> accessData(K key) {
        System.out.println("Cache: Accessing key=" + key);
        return keyBasedExecutor.submitTask(key, () -> {
            try {
                V value;
                if (cacheStorage.containsKey(key)) {
                    System.out.println("Cache: Cache hit for key=" + key);
                    value = cacheStorage.get(key);
                    evictionAlgorithm.keyAccessed(key);
                } else {
                    System.out.println("Cache: Cache miss for key=" + key);
                    value = readPolicy.read(key, cacheStorage, dbStorage);
                    if (value != null) {
                        evictionAlgorithm.keyAccessed(key);
                    } else {
                        System.out.println("Cache: Key not found: " + key);
                        throw new Exception("Key not found: " + key);
                    }
                }
                System.out.println("Cache: Returning value=" + value + " for key=" + key);
                return value;
            } catch (Exception e) {
                System.err.println("Cache: Error accessing key=" + key + ": " + e.getMessage());
                throw new CompletionException(e);
            }
        });
    }

    public CompletableFuture<Void> updateData(K key, V value) {
        return keyBasedExecutor.submitTask(key, () -> {
            try {
                // Check if we need to evict before adding new item
                if (cacheStorage.size() >= cacheStorage.getCapacity()) {
                    System.out.println("Cache: Capacity reached (" + cacheStorage.size() +
                            "/" + cacheStorage.getCapacity() + "), evicting key");
                    K evictedKey = evictionAlgorithm.evictKey();
                    if (evictedKey != null) {
                        System.out.println("Cache: Evicting key=" + evictedKey);
                        cacheStorage.remove(evictedKey);
                    }
                }

                // Write through to both cache and database
                writePolicy.write(key, value, cacheStorage, dbStorage);

                // Update access record
                evictionAlgorithm.keyAccessed(key);

                System.out.println("Cache: Update completed for key=" + key);
                return null;
            } catch (Exception e) {
                System.err.println("Cache: Error updating key=" + key + ": " + e.getMessage());
                throw new CompletionException(e);
            }
        });
    }

    public void shutdown() {
        System.out.println("Cache: Shutting down");
        keyBasedExecutor.shutdown();
    }
}

// Main Class
public class CacheSystem {
    public static void main(String[] args) {
        try {
            CacheStorage<String, String> cacheStorage = new InMemoryCacheStorage<>(2);
            DBStorage<String, String> dbStorage = new SimpleDBStorage<>();
            WritePolicy<String, String> writePolicy = new WriteThroughPolicy<>();
            EvictionAlgorithm<String> evictionAlg = new LRUEvictionAlgorithm<>();
            ReadPolicy<String, String> readPolicy = new ReadThroughPolicy<>();

            // Initialize database with some values
            System.out.println("=== Initializing Database ===");
            dbStorage.write("A", "Apple");
            dbStorage.write("B", "Banana");
            dbStorage.write("C", "Cherry");
            dbStorage.write("D","Dead");

            Cache<String, String> cache = new Cache<>(
                    cacheStorage, dbStorage, writePolicy, readPolicy, evictionAlg, 4);

            System.out.println("\n=== Testing Read-Through Cache ===");

            // Test read-through functionality
            System.out.println("\n1. First access to A (should load from DB):");
            System.out.println("Result: " + cache.accessData("A").join());

            System.out.println("\n2. First access to B (should load from DB):");
            System.out.println("Result: " + cache.accessData("B").join());

            System.out.println("\n3. Access to A again (should be cache hit):");
            System.out.println("Result: " + cache.accessData("A").join());

            // This should trigger eviction as cache capacity is 2
            System.out.println("\n4. First access to C (should load from DB and evict B):");
            System.out.println("Result: " + cache.accessData("C").join());

            // Now B should be loaded from DB again
            System.out.println("\n5. Access to B again (should load from DB again):");
            System.out.println("Result: " + cache.accessData("B").join());

            System.out.println("\n6. Access to non-existent key:");

            System.out.println("Result: " + cache.accessData("D").join());
            System.out.println("\n=== Testing Write-Through Cache ===");
            System.out.println("7. Updating existing key A:");
            cache.updateData("A", "Avocado").join();
            System.out.println("Result: " + cache.accessData("A").join());

            System.out.println("8. Adding new key D:");
            cache.updateData("D", "Date").join();
            System.out.println("Result: " + cache.accessData("D").join());
            cache.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}