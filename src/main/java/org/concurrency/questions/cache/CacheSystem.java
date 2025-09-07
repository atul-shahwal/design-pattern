package org.concurrency.questions.cache;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

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

// Eviction Algorithm Interface (Strategy Pattern)
interface EvictionAlgorithm<K> {
    void keyAccessed(K key) throws Exception;
    K evictKey() throws Exception;
}

// In-Memory Cache Storage Implementation
class InMemoryCacheStorage<K, V> implements CacheStorage<K, V> {
    private final Map<K, V> cache;
    private final int capacity;

    public InMemoryCacheStorage(int capacity) {
        this.capacity = capacity;
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public void put(K key, V value) throws Exception {
        cache.put(key, value);
    }

    @Override
    public V get(K key) throws Exception {
        return cache.get(key);
    }

    @Override
    public void remove(K key) throws Exception {
        cache.remove(key);
    }

    @Override
    public boolean containsKey(K key) throws Exception {
        return cache.containsKey(key);
    }

    @Override
    public int size() throws Exception {
        return cache.size();
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
        database.put(key, value);
    }

    @Override
    public V read(K key) throws Exception {
        return database.get(key);
    }

    @Override
    public void delete(K key) throws Exception {
        database.remove(key);
    }
}

// Write-Through Policy Implementation
class WriteThroughPolicy<K, V> implements WritePolicy<K, V> {
    @Override
    public void write(K key, V value, CacheStorage<K, V> cacheStorage, DBStorage<K, V> dbStorage) throws Exception {
        CompletableFuture<Void> cacheFuture = CompletableFuture.runAsync(() -> {
            try {
                cacheStorage.put(key, value);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });

        CompletableFuture<Void> dbFuture = CompletableFuture.runAsync(() -> {
            try {
                dbStorage.write(key, value);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });

        CompletableFuture.allOf(cacheFuture, dbFuture).join();
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

    public LRUEvictionAlgorithm() {
        this.dll = new DoublyLinkedList<>();
        this.keyToNodeMap = new HashMap<>();
    }

    @Override
    public synchronized void keyAccessed(K key) throws Exception {
        if (keyToNodeMap.containsKey(key)) {
            DoublyLinkedListNode<K> node = keyToNodeMap.get(key);
            dll.detachNode(node);
            dll.addNodeAtTail(node);
        } else {
            DoublyLinkedListNode<K> newNode = new DoublyLinkedListNode<>(key);
            dll.addNodeAtTail(newNode);
            keyToNodeMap.put(key, newNode);
        }
    }

    @Override
    public synchronized K evictKey() throws Exception {
        DoublyLinkedListNode<K> nodeToEvict = dll.getHead();
        if (nodeToEvict == null) return null;
        K evictKey = nodeToEvict.getValue();
        dll.removeHead();
        keyToNodeMap.remove(evictKey);
        return evictKey;
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
    private final EvictionAlgorithm<K> evictionAlgorithm;
    private final KeyBasedExecutor keyBasedExecutor;

    public Cache(CacheStorage<K, V> cacheStorage, DBStorage<K, V> dbStorage,
                 WritePolicy<K, V> writePolicy, EvictionAlgorithm<K> evictionAlgorithm,
                 int numExecutors) {
        this.cacheStorage = cacheStorage;
        this.dbStorage = dbStorage;
        this.writePolicy = writePolicy;
        this.evictionAlgorithm = evictionAlgorithm;
        this.keyBasedExecutor = new KeyBasedExecutor(numExecutors);
    }

    public CompletableFuture<V> accessData(K key) {
        return keyBasedExecutor.submitTask(key, () -> {
            try {
                if (!cacheStorage.containsKey(key)) {
                    throw new Exception("Key not found in cache: " + key);
                }
                evictionAlgorithm.keyAccessed(key);
                return cacheStorage.get(key);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    public CompletableFuture<Void> updateData(K key, V value) {
        return keyBasedExecutor.submitTask(key, () -> {
            try {
                if (cacheStorage.containsKey(key)) {
                    writePolicy.write(key, value, cacheStorage, dbStorage);
                    evictionAlgorithm.keyAccessed(key);
                } else {
                    if (cacheStorage.size() >= cacheStorage.getCapacity()) {
                        K evictedKey = evictionAlgorithm.evictKey();
                        if (evictedKey != null) {
                            cacheStorage.remove(evictedKey);
                        }
                    }
                    writePolicy.write(key, value, cacheStorage, dbStorage);
                    evictionAlgorithm.keyAccessed(key);
                }
                return null;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    public void shutdown() {
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
            Cache<String, String> cache = new Cache<>(cacheStorage, dbStorage, writePolicy, evictionAlg, 4);

            cache.updateData("A", "Apple").join();
            cache.updateData("B", "Banana").join();
            cache.updateData("C", "Cherry").join(); // Should evict A

            System.out.println("Accessing B: " + cache.accessData("B").join());
            System.out.println("Accessing C: " + cache.accessData("C").join());

            cache.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
