package org.concurrency.questions.cache.distributed;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// ------------------- STORAGE & POLICY -------------------

interface CacheStorage<K, V> {
    void put(K key, V value);
    V get(K key);
    void remove(K key);
    boolean containsKey(K key);
    int size();
    int getCapacity();
    Set<K> keySet();
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

// ------------------- IN-MEMORY & DB -------------------

class InMemoryCacheStorage<K, V> implements CacheStorage<K, V> {
    private final Map<K, V> cache = new ConcurrentHashMap<>();
    private final int capacity;

    public InMemoryCacheStorage(int capacity) { this.capacity = capacity; }

    @Override public void put(K key, V value) { cache.put(key, value); }
    @Override public V get(K key) { return cache.get(key); }
    @Override public void remove(K key) { cache.remove(key); }
    @Override public boolean containsKey(K key) { return cache.containsKey(key); }
    @Override public int size() { return cache.size(); }
    @Override public int getCapacity() { return capacity; }
    @Override public Set<K> keySet() { return cache.keySet(); }
}

class SimpleDBStorage<K, V> implements DBStorage<K, V> {
    private final Map<K, V> db = new ConcurrentHashMap<>();
    @Override public void write(K key, V value) { db.put(key, value); }
    @Override public V read(K key) { return db.get(key); }
    @Override public void delete(K key) { db.remove(key); }
}

// ------------------- POLICIES -------------------

class WriteThroughPolicy<K, V> implements WritePolicy<K, V> {
    public void write(K key, V value, CacheStorage<K, V> cache, DBStorage<K, V> db) {
        cache.put(key, value);
        db.write(key, value);
    }
}

class ReadThroughPolicy<K, V> implements ReadPolicy<K, V> {
    public V read(K key, CacheStorage<K, V> cache, DBStorage<K, V> db) {
        V value = cache.get(key);
        if (value == null) {
            value = db.read(key);
            if (value != null) cache.put(key, value);
        }
        return value;
    }
}

// ------------------- LRU EVICTION -------------------

class LRUEvictionAlgorithm<K> implements EvictionAlgorithm<K> {
    private final LinkedHashMap<K, Boolean> lruMap = new LinkedHashMap<>(16, 0.75f, true);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void keyAccessed(K key) {
        lock.writeLock().lock();
        try {
            lruMap.put(key, true); // touch or add
        } finally { lock.writeLock().unlock(); }
    }

    public K evictKey() {
        lock.writeLock().lock();
        try {
            if (lruMap.isEmpty()) return null;
            Iterator<K> it = lruMap.keySet().iterator();
            K key = it.next();
            it.remove();
            return key;
        } finally { lock.writeLock().unlock(); }
    }
}

// ------------------- KEY EXECUTOR -------------------

class KeyBasedExecutor {
    private final ExecutorService[] executors;

    public KeyBasedExecutor(int pools) {
        executors = new ExecutorService[pools];
        for (int i = 0; i < pools; i++) {
            final int threadIndex = i;
            executors[i] = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r);
                t.setName("KeyExecutor-" + threadIndex);
                return t;
            });
        }
    }

    public CompletableFuture<Void> submit(Runnable task, Object key) {
        int index = Math.abs(key.hashCode() % executors.length);
        return CompletableFuture.runAsync(task, executors[index]);
    }

    public <V> CompletableFuture<V> submit(Callable<V> task, Object key) {
        int index = Math.abs(key.hashCode() % executors.length);
        ExecutorService executorService = executors[index];
        return CompletableFuture.supplyAsync(() -> {
            try { return task.call(); }
            catch (Exception e) { throw new CompletionException(e); }
        }, executorService);
    }

    public void shutdown() { for (ExecutorService ex : executors) ex.shutdown(); }
}

// ------------------- CACHE -------------------

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
        return executor.submit(() -> {
            V value;
            if (cache.containsKey(key)) {
                value = cache.get(key);
                eviction.keyAccessed(key);
            } else {
                value = readPolicy.read(key, cache, db);
                if (value != null) eviction.keyAccessed(key);
            }
            return value;
        }, key);
    }

    public CompletableFuture<Void> put(K key, V value) {
        return executor.submit(() -> {
            if (cache.size() >= cache.getCapacity()) {
                K evictKey = eviction.evictKey();
                if (evictKey != null) cache.remove(evictKey);
            }
            writePolicy.write(key, value, cache, db);
            eviction.keyAccessed(key);
        }, key);
    }

    public void shutdown() { executor.shutdown(); }

    public Map<K, V> internalState() {
        Map<K, V> map = new HashMap<>();
        for (K key : cache.keySet()) map.put(key, cache.get(key));
        return map;
    }
}

// ------------------- DISTRIBUTED NODES -------------------

class Node {
    private final String id;
    public Node(String host, int port) { this.id = host + ":" + port; }
    public String getId() { return id; }
    @Override public String toString() { return id; }
}

class NodeCache<K, V> {
    private final Cache<K, V> master;
    private final List<Cache<K, V>> replicas;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public NodeCache(Cache<K, V> master, List<Cache<K, V>> replicas) {
        this.master = master;
        this.replicas = replicas;
    }

    public void put(K key, V value) {
        try {
            master.put(key, value).get();
            for (int i = 0; i < replicas.size(); i++) {
                final int idx = i;
                executor.submit(() -> {
                    try { replicas.get(idx).put(key, value).get(); }
                    catch (Exception e) { e.printStackTrace(); }
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public V get(K key) {
        try { return master.get(key).get(); }
        catch (Exception e) {
            e.printStackTrace();
            for (Cache<K, V> replica : replicas) {
                try { V val = replica.get(key).get(); if (val != null) return val; }
                catch (Exception ignored) {}
            }
            return null;
        }
    }

    public Map<K, V> internalState() {
        Map<K, V> map = new HashMap<>();
        map.putAll(master.internalState());
        for (int i = 0; i < replicas.size(); i++) {
            map.putAll(replicas.get(i).internalState());
        }
        return map;
    }

    public List<Cache<K, V>> getReplicas() { return replicas; }
    public void shutdown() {
        master.shutdown();
        replicas.forEach(Cache::shutdown);
        executor.shutdown();
    }
}

// ------------------- CONSISTENT HASHING -------------------

interface HashFunction { int hash(String key); }

class MD5HashFunction implements HashFunction {
    public int hash(String key) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes());
            return ((digest[0] & 0xFF) << 24) |
                    ((digest[1] & 0xFF) << 16) |
                    ((digest[2] & 0xFF) << 8) |
                    (digest[3] & 0xFF);
        } catch (Exception e) { return key.hashCode(); }
    }
}

class ConsistentHashing {
    private final SortedMap<Integer, Node> circle = new TreeMap<>();
    private final int virtualNodeCount;
    private final HashFunction hashFunction;

    public ConsistentHashing(int virtualNodeCount, HashFunction hashFunction) {
        this.virtualNodeCount = virtualNodeCount;
        this.hashFunction = hashFunction;
    }

    public void addNode(Node node) {
        for (int i = 0; i < virtualNodeCount; i++)
            circle.put(hashFunction.hash(node.getId() + "#" + i), node);
    }

    public Node getNode(Object key) {
        if (circle.isEmpty()) return null;
        int hash = hashFunction.hash(key.toString());
        SortedMap<Integer, Node> tailMap = circle.tailMap(hash);
        int nodeHash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        return circle.get(nodeHash);
    }
}

// ------------------- EXAMPLE -------------------

public class DistributedCacheExample {
    public static void main(String[] args) throws Exception {
        // Create 2 masters and 2 replicas each
        Node master1 = new Node("localhost", 8087);
        Node master2 = new Node("localhost", 8030);

        NodeCache<String, String> node1 = new NodeCache<>(
                new Cache<>(new InMemoryCacheStorage<>(10), new SimpleDBStorage<>(),
                        new WriteThroughPolicy<>(), new ReadThroughPolicy<>(), new LRUEvictionAlgorithm<>(), 4),
                List.of(
                        new Cache<>(new InMemoryCacheStorage<>(10), new SimpleDBStorage<>(),
                                new WriteThroughPolicy<>(), new ReadThroughPolicy<>(), new LRUEvictionAlgorithm<>(), 2),
                        new Cache<>(new InMemoryCacheStorage<>(10), new SimpleDBStorage<>(),
                                new WriteThroughPolicy<>(), new ReadThroughPolicy<>(), new LRUEvictionAlgorithm<>(), 2)
                )
        );

        NodeCache<String, String> node2 = new NodeCache<>(
                new Cache<>(new InMemoryCacheStorage<>(10), new SimpleDBStorage<>(),
                        new WriteThroughPolicy<>(), new ReadThroughPolicy<>(), new LRUEvictionAlgorithm<>(), 4),
                List.of(
                        new Cache<>(new InMemoryCacheStorage<>(10), new SimpleDBStorage<>(),
                                new WriteThroughPolicy<>(), new ReadThroughPolicy<>(), new LRUEvictionAlgorithm<>(), 2),
                        new Cache<>(new InMemoryCacheStorage<>(10), new SimpleDBStorage<>(),
                                new WriteThroughPolicy<>(), new ReadThroughPolicy<>(), new LRUEvictionAlgorithm<>(), 2)
                )
        );

        Map<Node, NodeCache<String, String>> nodes = Map.of(master1, node1, master2, node2);

        ConsistentHashing ch = new ConsistentHashing(3, new MD5HashFunction());
        nodes.keySet().forEach(ch::addNode);

        // Put some keys
        List<String> keys = List.of("name", "city", "country", "language");
        List<String> values = List.of("Atul", "Pune", "India", "Java");

        for (int i = 0; i < keys.size(); i++) {
            Node node = ch.getNode(keys.get(i));
            System.out.println("PUT key: " + keys.get(i) + " -> routed to node: " + node);
            nodes.get(node).put(keys.get(i), values.get(i));
        }

        Thread.sleep(2000); // wait replication

        // Get keys
        for (String key : keys) {
            Node node = ch.getNode(key);
            System.out.println("GET key: " + key + " -> routed to node: " + node);
            String value = nodes.get(node).get(key);
            System.out.println("GET " + key + ": " + value);
        }

        // Print node states including replicas
        System.out.println("\n--- Node Internal States ---");
        nodes.forEach((node, nc) -> {
            System.out.println(node + " -> Master: " + nc.internalState());
            List<Cache<String, String>> replicas = nc.getReplicas();
            for (int i = 0; i < replicas.size(); i++) {
                System.out.println("  Replica " + i + ": " + replicas.get(i).internalState());
            }
        });

        // Shutdown
        nodes.values().forEach(NodeCache::shutdown);
    }
}
