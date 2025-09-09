package org.concurrency.questions.cache.interview.distributed;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.net.*;
import java.io.*;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;

/**
 * 🎯 Problem Statement: Low-Level Design - Distributed Cache System
 *
 * Design a distributed thread-safe cache that supports various eviction algorithms,
 * write policies, replication strategies, and ensures consistency between nodes.
 *
 * ✅ Requirements:
 * - Configurable capacity with eviction when full (e.g., LRU).
 * - Write-through strategy for cache and database synchronization.
 * - Handle concurrent access efficiently using per-key execution.
 * - Support for scaling and multiple eviction strategies.
 * - Distributed architecture with consistent hashing
 * - Multiple replication strategies (Synchronous, Quorum, Async)
 * - HTTP-based communication between nodes
 */

/**
 * ❓ Interview Q: Why do we need a CacheStorage interface?
 * ✅ A: It defines the contract for any cache storage implementation (e.g., in-memory, Redis, distributed).
 * This allows us to swap implementations without changing the cache logic (OCP - Open/Closed Principle).
 * It also makes testing easier with different storage backends.
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
 * ❓ Interview Q: Why do we need DBStorage separately when we already have cache?
 * ✅ A: The DB acts as persistent storage (truth source). Cache is faster but volatile.
 * Keeping them separate enforces abstraction and allows switching between database types easily.
 * This follows the Single Responsibility Principle.
 */
interface DBStorage<K, V> {
    void write(K key, V value);
    V read(K key);
    void delete(K key);
}

/**
 * ❓ Interview Q: Why do we need different WritePolicy strategies?
 * ✅ A: To support multiple policies:
 * - Write-through: write to cache + DB together
 * - Write-back: write to cache first, DB later
 * This is Strategy Pattern in action, allowing runtime selection of write behavior.
 */
interface WritePolicy<K, V> {
    void write(K key, V value, CacheStorage<K, V> cache, DBStorage<K, V> db);
}

/**
 * ❓ Interview Q: Why do we need a ReadPolicy interface?
 * ✅ A: It decouples how reads are handled.
 * - Read-through: fetch from DB on cache miss
 * - Read-around: bypass cache for some reads
 * - Cache-aside: application handles cache population
 * This gives flexibility and follows the Strategy Pattern.
 */
interface ReadPolicy<K, V> {
    V read(K key, CacheStorage<K, V> cache, DBStorage<K, V> db);
}

/**
 * ❓ Interview Q: Why do we need an EvictionAlgorithm interface?
 * ✅ A: Since cache has limited capacity, we need eviction strategies (LRU, LFU, FIFO, Random).
 * This again uses Strategy Pattern for plug-and-play eviction.
 * Different workloads benefit from different eviction policies.
 */
interface EvictionAlgorithm<K> {
    void keyAccessed(K key);
    K evictKey();
    void removeKey(K key);
}

/**
 * ❓ Interview Q: Why do we need a ReplicationStrategy interface?
 * ✅ A: In distributed systems, we need different replication approaches:
 * - Synchronous: Wait for all replicas to acknowledge
 * - Asynchronous: Fire and forget
 * - Quorum-based: Wait for majority of replicas
 * This allows flexibility in consistency vs. performance trade-offs.
 */
interface ReplicationStrategy<K, V> {
    CompletableFuture<Boolean> replicate(PutRequest<K, V> request, List<Node> nodes);
    String getStrategyName();
}

/**
 * ❓ Interview Q: Why do we need NodeDiscovery interface?
 * ✅ A: In a distributed system, nodes can join or leave dynamically.
 * This interface abstracts the node discovery mechanism, which could be:
 * - Static configuration
 * - ZooKeeper/etcd for dynamic discovery
 * - AWS EC2 auto-scaling group membership
 */
interface NodeDiscovery {
    List<Node> getActiveNodes();
    void registerNode(Node node);
    void removeNode(Node node);
    void updateNodeHealth(Node node, boolean isHealthy);
}

/**
 * ❓ Interview Q: Why use ConcurrentHashMap for in-memory storage?
 * ✅ A: ConcurrentHashMap provides thread-safe operations with better performance than synchronized maps.
 * It uses fine-grained locking (bucket-level) rather than locking the entire map.
 * This is crucial for high-concurrency cache applications.
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
 * ❓ Interview Q: Why use a mock DB instead of a real database?
 * ✅ A: For demonstration and testing purposes. In production, this would be replaced with:
 * - SQL databases (MySQL, PostgreSQL)
 * - NoSQL databases (MongoDB, Cassandra)
 * - Cloud storage (S3, DynamoDB)
 * The interface allows easy swapping of implementations.
 */
class SimpleDBStorage<K, V> implements DBStorage<K, V> {
    private final Map<K, V> db = new ConcurrentHashMap<>();
    public void write(K key, V value) { db.put(key, value); }
    public V read(K key) { return db.get(key); }
    public void delete(K key) { db.remove(key); }
}

/**
 * ❓ Interview Q: What are the advantages of Write-Through policy?
 * ✅ A:
 * - Ensures data is always persisted to durable storage
 * - Simplifies cache consistency as DB is always up-to-date
 * - Disadvantages: Higher write latency as both cache and DB must be updated
 */
class WriteThroughPolicy<K, V> implements WritePolicy<K, V> {
    public void write(K key, V value, CacheStorage<K, V> cache, DBStorage<K, V> db) {
        cache.put(key, value);
        db.write(key, value);
    }
}

/**
 * ❓ Interview Q: Why update cache on a read miss?
 * ✅ A: To improve future read performance. This avoids repeated DB hits for the same key.
 * This is called "Read-Through" policy and is common in cache systems.
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
 * ❓ Interview Q: Why LinkedHashMap for LRU implementation?
 * ✅ A: LinkedHashMap maintains insertion/access order, making it easy to evict the least recently used entry.
 * We also use ReentrantReadWriteLock for thread-safety to handle concurrent access.
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
            lruMap.put(key, true); // Touch to update access order
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

    public void removeKey(K key) {
        lock.writeLock().lock();
        try {
            lruMap.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }
}

/**
 * ❓ Interview Q: Why use KeyBasedExecutor instead of a global thread pool?
 * ✅ A: It ensures all operations for the same key are serialized
 * (avoiding race conditions like double-writes).
 * This is crucial for maintaining consistency in concurrent environments.
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
 * ❓ Interview Q: Why does this Cache class return CompletableFuture?
 * ✅ A: To support async operations and non-blocking API.
 * In distributed systems, cache/DB reads may be network calls → async improves performance.
 * It also allows better resource utilization by not blocking threads.
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

    /**
     * ❓ Interview Q: Why is this method package-private instead of public?
     * ✅ A: To maintain encapsulation while still allowing controlled access
     * for distributed cache operations. This follows the Principle of Least Privilege.
     */
    void internalPut(K key, V value) {
        if (cache.size() >= cache.getCapacity()) {
            K keyToEvict = eviction.evictKey();
            if (keyToEvict != null) {
                cache.remove(keyToEvict);
            }
        }
        cache.put(key, value);
        eviction.keyAccessed(key);
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

    // Getters for distributed cache (maintains encapsulation)
    CacheStorage<K, V> getCacheStorage() {
        return cache;
    }

    DBStorage<K, V> getDbStorage() {
        return db;
    }

    EvictionAlgorithm<K> getEvictionAlgorithm() {
        return eviction;
    }
}

// ==================== DISTRIBUTED COMPONENTS ====================

/**
 * ❓ Interview Q: Why do we need a Node class?
 * ✅ A: To represent individual cache nodes in the distributed system.
 * Each node has its own identity (host:port) and health status.
 * This abstraction allows the system to manage nodes dynamically.
 */
class Node {
    private final String id;
    private final String host;
    private final int port;
    private boolean healthy;

    public Node(String host, int port) {
        this.id = host + ":" + port;
        this.host = host;
        this.port = port;
        this.healthy = true;
    }

    public String getId() { return id; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public boolean isHealthy() { return healthy; }
    public void setHealthy(boolean healthy) { this.healthy = healthy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return port == node.port &&
                this.id.equals(node.id) &&
                this.host.equals(node.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}

/**
 * ❓ Interview Q: Why do we need a PutRequest class?
 * ✅ A: To encapsulate all information needed for a put operation across the network.
 * It includes the key, value, operation ID for deduplication, and timestamp.
 * This follows the Command Pattern for remote operations.
 */
class PutRequest<K, V> {
    private K key;
    private V value;
    private String operationId;
    private long timestamp;

    public PutRequest(K key, V value) {
        this.key = key;
        this.value = value;
        this.operationId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    public K getKey() { return key; }
    public V getValue() { return value; }
    public String getOperationId() { return operationId; }
    public long getTimestamp() { return timestamp; }
}

/**
 * ❓ Interview Q: What is consistent hashing and why is it important?
 * ✅ A: Consistent hashing is a special hashing technique that minimizes reorganization
 * when nodes are added or removed from the distributed system.
 * It reduces key redistribution from O(n) to O(k/n) where k is the number of keys
 * and n is the number of nodes, making the system more scalable.
 */
class ConsistentHashing {
    private final SortedMap<Integer, Node> circle = new TreeMap<>();
    private final int virtualNodeCount;
    private final HashFunction hashFunction;

    public ConsistentHashing(int virtualNodeCount, HashFunction hashFunction) {
        this.virtualNodeCount = virtualNodeCount;
        this.hashFunction = hashFunction;
    }

    public void addNode(Node node) {
        for (int i = 0; i < virtualNodeCount; i++) {
            int hash = hashFunction.hash(node.getId() + "#" + i);
            circle.put(hash, node);
        }
    }

    public void removeNode(Node node) {
        for (int i = 0; i < virtualNodeCount; i++) {
            int hash = hashFunction.hash(node.getId() + "#" + i);
            circle.remove(hash);
        }
    }

    public Node getNode(Object key) {
        if (circle.isEmpty()) return null;
        int hash = hashFunction.hash(key.toString());
        SortedMap<Integer, Node> tailMap = circle.tailMap(hash);
        int nodeHash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        return circle.get(nodeHash);
    }

    public List<Node> getReplicaNodes(Object key, int replicaCount) {
        List<Node> nodes = new ArrayList<>();
        if (circle.isEmpty()) return nodes;

        int hash = hashFunction.hash(key.toString());
        SortedMap<Integer, Node> tailMap = circle.tailMap(hash);
        Iterator<Integer> it = tailMap.keySet().iterator();

        // Get primary node
        if (it.hasNext()) {
            nodes.add(tailMap.get(it.next()));
        }

        // Get replica nodes
        int count = 1;
        while (count < replicaCount && it.hasNext()) {
            nodes.add(tailMap.get(it.next()));
            count++;
        }

        // If we need more replicas, wrap around
        if (count < replicaCount) {
            it = circle.keySet().iterator();
            while (count < replicaCount && it.hasNext()) {
                Node node = circle.get(it.next());
                if (!nodes.contains(node)) {
                    nodes.add(node);
                    count++;
                }
            }
        }
        return nodes;
    }
}

/**
 * ❓ Interview Q: Why do we need a HashFunction interface?
 * ✅ A: To abstract the hashing algorithm used for consistent hashing.
 * This allows switching between different hash functions (MD5, SHA, MurmurHash)
 * without changing the consistent hashing logic.
 */
interface HashFunction {
    int hash(String key);
}

/**
 * ❓ Interview Q: Why use MD5 for hashing?
 * ✅ A: MD5 provides a good distribution of hash values, which is important for consistent hashing.
 * While not cryptographically secure, it's sufficient for distributed caching.
 * Alternatives include SHA-1, MurmurHash, or CityHash.
 */
class MD5HashFunction implements HashFunction {
    public int hash(String key) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes());
            return ((digest[0] & 0xFF) << 24) |
                    ((digest[1] & 0xFF) << 16) |
                    ((digest[2] & 0xFF) << 8) |
                    (digest[3] & 0xFF);
        } catch (Exception e) {
            return key.hashCode();
        }
    }
}

/**
 * ❓ Interview Q: What are the trade-offs of synchronous replication?
 * ✅ A: Synchronous replication provides strong consistency but higher latency.
 * It waits for all replicas to acknowledge before completing the operation.
 * This is suitable for systems where data consistency is critical.
 */
class SynchronousReplication<K, V> implements ReplicationStrategy<K, V> {
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public SynchronousReplication(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public CompletableFuture<Boolean> replicate(PutRequest<K, V> request, List<Node> nodes) {
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (Node node : nodes) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    String url = "http://" + node.getHost() + ":" + node.getPort() + "/cache";
                    String jsonBody = gson.toJson(request);
                    HttpResponse response = httpClient.put(url, jsonBody);
                    return response.getStatusCode() == 200;
                } catch (Exception e) {
                    return false;
                }
            });
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().allMatch(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        return false;
                    }
                }));
    }

    public String getStrategyName() {
        return "SynchronousReplication";
    }
}

/**
 * ❓ Interview Q: What is quorum-based replication?
 * ✅ A: Quorum-based replication requires acknowledgment from a majority of replicas (W > N/2).
 * It provides a balance between consistency and availability.
 * It ensures strong consistency while tolerating some node failures.
 */
class QuorumReplication<K, V> implements ReplicationStrategy<K, V> {
    private final HttpClient httpClient;
    private final int writeQuorum;
    private final Gson gson = new Gson();

    public QuorumReplication(HttpClient httpClient, int writeQuorum) {
        this.httpClient = httpClient;
        this.writeQuorum = writeQuorum;
    }

    public CompletableFuture<Boolean> replicate(PutRequest<K, V> request, List<Node> nodes) {
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (Node node : nodes) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    String url = "http://" + node.getHost() + ":" + node.getPort() + "/cache";
                    String jsonBody = gson.toJson(request);
                    HttpResponse response = httpClient.put(url, jsonBody);
                    return response.getStatusCode() == 200;
                } catch (Exception e) {
                    return false;
                }
            });
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    int successCount = 0;
                    for (CompletableFuture<Boolean> f : futures) {
                        try {
                            if (f.get()) successCount++;
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    return successCount >= writeQuorum;
                });
    }

    public String getStrategyName() {
        return "QuorumReplication(" + writeQuorum + ")";
    }
}

/**
 * ❓ Interview Q: When would you use asynchronous replication?
 * ✅ A: Asynchronous replication provides better performance but weaker consistency.
 * It's suitable for systems where performance is critical and eventual consistency is acceptable.
 * Examples include logging systems, metrics collection, and recommendation engines.
 */
class AsyncReplication<K, V> implements ReplicationStrategy<K, V> {
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public AsyncReplication(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public CompletableFuture<Boolean> replicate(PutRequest<K, V> request, List<Node> nodes) {
        for (Node node : nodes) {
            CompletableFuture.runAsync(() -> {
                try {
                    String url = "http://" + node.getHost() + ":" + node.getPort() + "/cache";
                    String jsonBody = gson.toJson(request);
                    httpClient.putAsync(url, jsonBody);
                } catch (Exception e) {
                    // Log error but don't fail the operation
                }
            });
        }

        return CompletableFuture.completedFuture(true);
    }

    public String getStrategyName() {
        return "AsyncReplication";
    }
}

/**
 * ❓ Interview Q: Why do we need node discovery in distributed systems?
 * ✅ A: Node discovery allows the system to dynamically adapt to changes in cluster membership.
 * Nodes can join or leave without manual configuration.
 * This is essential for scalability, fault tolerance, and auto-scaling.
 */
class SimpleNodeDiscovery implements NodeDiscovery {
    private final List<Node> nodes = new CopyOnWriteArrayList<>();
    private final Map<Node, Boolean> nodeHealth = new ConcurrentHashMap<>();

    public List<Node> getActiveNodes() {
        List<Node> activeNodes = new ArrayList<>();
        for (Node node : nodes) {
            if (nodeHealth.getOrDefault(node, true)) {
                activeNodes.add(node);
            }
        }
        return activeNodes;
    }

    public void registerNode(Node node) {
        if (!nodes.contains(node)) {
            nodes.add(node);
            nodeHealth.put(node, true);
        }
    }

    public void removeNode(Node node) {
        nodes.remove(node);
        nodeHealth.remove(node);
    }

    public void updateNodeHealth(Node node, boolean isHealthy) {
        nodeHealth.put(node, isHealthy);
    }
}

// HTTP client for inter-node communication
/**
 * ❓ Interview Q: Why use HTTP for inter-node communication?
 * ✅ A: HTTP is a widely supported protocol with good tooling and libraries.
 * It works well through firewalls and load balancers.
 * Alternatives include gRPC (for performance) or custom TCP protocols.
 */
class HttpClient {
    public HttpResponse put(String url, String body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = body.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int statusCode = connection.getResponseCode();
        String responseBody = null;
        if (statusCode == 200) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                responseBody = response.toString();
            }
        }

        return new HttpResponse(statusCode, responseBody);
    }

    public HttpResponse get(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        int statusCode = connection.getResponseCode();
        String responseBody = null;
        if (statusCode == 200) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                responseBody = response.toString();
            }
        }

        return new HttpResponse(statusCode, responseBody);
    }

    public void putAsync(String url, String body) {
        CompletableFuture.runAsync(() -> {
            try {
                put(url, body);
            } catch (Exception e) {
                // Log error
            }
        });
    }
}

class HttpResponse {
    private final int statusCode;
    private final String body;

    public HttpResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    public int getStatusCode() { return statusCode; }
    public String getBody() { return body; }
}

// Distributed cache that extends the original cache
/**
 * ❓ Interview Q: How does the distributed cache maintain consistency?
 * ✅ A: It uses a combination of consistent hashing for data distribution and
 * configurable replication strategies for data consistency.
 * The specific consistency model depends on the chosen replication strategy.
 */
class DistributedCache<K, V> {
    private final Cache<K, V> localCache;
    private final ConsistentHashing consistentHashing;
    private final ReplicationStrategy<K, V> replicationStrategy;
    private final NodeDiscovery nodeDiscovery;
    private final Node localNode;
    private final int replicationFactor;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public DistributedCache(Cache<K, V> localCache,
                            ConsistentHashing consistentHashing,
                            ReplicationStrategy<K, V> replicationStrategy,
                            NodeDiscovery nodeDiscovery, Node localNode,
                            int replicationFactor, HttpClient httpClient) {
        this.localCache = localCache;
        this.consistentHashing = consistentHashing;
        this.replicationStrategy = replicationStrategy;
        this.nodeDiscovery = nodeDiscovery;
        this.localNode = localNode;
        this.replicationFactor = replicationFactor;
        this.httpClient = httpClient;

        // Register local node
        nodeDiscovery.registerNode(localNode);

        // Add all known nodes to consistent hashing
        for (Node node : nodeDiscovery.getActiveNodes()) {
            consistentHashing.addNode(node);
        }
    }

    public CompletableFuture<V> get(K key) {
        // Determine which node should have this key
        Node primaryNode = consistentHashing.getNode(key);

        // If local node is responsible, get from local cache
        if (primaryNode.equals(localNode)) {
            return localCache.get(key);
        }

        // Otherwise, fetch from remote node using GET request (FIXED)
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = "http://" + primaryNode.getHost() + ":" + primaryNode.getPort() + "/cache?key=" + key;
                HttpResponse response = httpClient.get(url); // Fixed: using GET instead of PUT
                if (response.getStatusCode() == 200) {
                    return gson.fromJson(response.getBody(), (Class<V>) Object.class);
                }
                return null;
            } catch (Exception e) {
                // Fallback to database or other nodes
                return localCache.get(key).join();
            }
        });
    }

    public CompletableFuture<Void> put(K key, V value) {
        // Determine which nodes should store this key
        List<Node> nodes = consistentHashing.getReplicaNodes(key, replicationFactor);

        // If local node is responsible, update local cache
        if (nodes.contains(localNode)) {
            // Use the internalPut method instead of direct access to cache storage
            localCache.internalPut(key, value);
        }

        // Replicate to other nodes
        PutRequest<K, V> request = new PutRequest<>(key, value);
        return replicationStrategy.replicate(request, nodes)
                .thenAccept(success -> {
                    if (!success) {
                        // Handle replication failure
                        System.err.println("Replication failed for key: " + key);
                    }
                });
    }

    public void handlePutRequest(PutRequest<K, V> request) {
        // Update local cache using the internalPut method
        localCache.internalPut(request.getKey(), request.getValue());
    }

    public void shutdown() {
        localCache.shutdown();
    }
}

// HTTP server for handling inter-node communication
/**
 * ❓ Interview Q: Why implement an HTTP server in the cache?
 * ✅ A: To handle inter-node communication for distributed operations.
 * The server receives put requests from other nodes and updates the local cache.
 * This allows the cache nodes to form a cohesive distributed system.
 */
class CacheServer {
    private final DistributedCache<String, String> cache;
    private final int port;
    private HttpServer server;
    private final Gson gson;

    public CacheServer(DistributedCache<String, String> cache, int port, Gson gson) {
        this.cache = cache;
        this.port = port;
        this.gson = gson;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/cache", exchange -> {
            try {
                if ("PUT".equals(exchange.getRequestMethod())) {
                    // Handle PUT request
                    InputStream is = exchange.getRequestBody();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String body = reader.lines().reduce("", (a, b) -> a + b);

                    PutRequest<String, String> request = gson.fromJson(body, PutRequest.class);

                    cache.handlePutRequest(request);

                    exchange.sendResponseHeaders(200, 0);
                    exchange.getResponseBody().close();
                } else if ("GET".equals(exchange.getRequestMethod())) {
                    // Handle GET request
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && query.startsWith("key=")) {
                        String key = query.split("=")[1];
                        String value = cache.get(key).join();

                        if (value != null) {
                            String response = gson.toJson(value);
                            exchange.getResponseHeaders().set("Content-Type", "application/json");
                            exchange.sendResponseHeaders(200, response.getBytes().length);
                            try (OutputStream os = exchange.getResponseBody()) {
                                os.write(response.getBytes());
                            }
                        } else {
                            exchange.sendResponseHeaders(404, 0);
                            exchange.getResponseBody().close();
                        }
                    } else {
                        exchange.sendResponseHeaders(400, 0); // Bad request
                        exchange.getResponseBody().close();
                    }
                }
            } catch (Exception e) {
                exchange.sendResponseHeaders(500, 0);
                exchange.getResponseBody().close();
            }
        });

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("Cache server started on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}

public class DistributedCacheExample {
    public static void main(String[] args) throws IOException {
        // Create shared components for the cluster
        Gson gson = new Gson();
        HttpClient httpClient = new HttpClient();
        NodeDiscovery nodeDiscovery = new SimpleNodeDiscovery();
        HashFunction hashFunction = new MD5HashFunction();
        int replicationFactor = 2;
        int virtualNodes = 100;

        // Define cluster nodes
        Node node1 = new Node("localhost", 8080);
        Node node2 = new Node("localhost", 8081);

        // Register all nodes with the discovery service
        nodeDiscovery.registerNode(node1);
        nodeDiscovery.registerNode(node2);

        // --- Setup and start server for Node 1 ---
        CacheStorage<String, String> cacheStorage1 = new InMemoryCacheStorage<>(10);
        DBStorage<String, String> dbStorage1 = new SimpleDBStorage<>();
        WritePolicy<String, String> writePolicy1 = new WriteThroughPolicy<>();
        ReadPolicy<String, String> readPolicy1 = new ReadThroughPolicy<>();
        EvictionAlgorithm<String> eviction1 = new LRUEvictionAlgorithm<>();
        Cache<String, String> localCache1 = new Cache<>(cacheStorage1, dbStorage1, writePolicy1, readPolicy1, eviction1, 4);

        ConsistentHashing consistentHashing1 = new ConsistentHashing(virtualNodes, hashFunction);
        ReplicationStrategy<String, String> replicationStrategy1 = new QuorumReplication<>(httpClient, 2);
        DistributedCache<String, String> distributedCache1 = new DistributedCache<>(localCache1, consistentHashing1, replicationStrategy1, nodeDiscovery, node1, replicationFactor, httpClient);

        CacheServer server1 = new CacheServer(distributedCache1, 8080, gson);
        server1.start();

        // --- Setup and start server for Node 2 ---
        CacheStorage<String, String> cacheStorage2 = new InMemoryCacheStorage<>(10);
        DBStorage<String, String> dbStorage2 = new SimpleDBStorage<>();
        WritePolicy<String, String> writePolicy2 = new WriteThroughPolicy<>();
        ReadPolicy<String, String> readPolicy2 = new ReadThroughPolicy<>();
        EvictionAlgorithm<String> eviction2 = new LRUEvictionAlgorithm<>();
        Cache<String, String> localCache2 = new Cache<>(cacheStorage2, dbStorage2, writePolicy2, readPolicy2, eviction2, 4);

        ConsistentHashing consistentHashing2 = new ConsistentHashing(virtualNodes, hashFunction);
        ReplicationStrategy<String, String> replicationStrategy2 = new QuorumReplication<>(httpClient, 2);
        DistributedCache<String, String> distributedCache2 = new DistributedCache<>(localCache2, consistentHashing2, replicationStrategy2, nodeDiscovery, node2, replicationFactor, httpClient);

        CacheServer server2 = new CacheServer(distributedCache2, 8081, gson);
        server2.start();

        System.out.println("=== Testing Distributed Cache with 2 nodes ===");

        // Get the cache instance for Node 1 to perform operations
        // All operations will be routed and replicated as per Consistent Hashing
        DistributedCache<String, String> cache = distributedCache1;

        // Add some initial data to the database, to simulate an existing truth source
        dbStorage1.write("1", "Apple");
        dbStorage1.write("2", "Banana");
        dbStorage1.write("3", "Cherry");

        // Test the cache
        System.out.println("Getting key '1': " + cache.get("1").join());
        System.out.println("Getting key '2': " + cache.get("2").join());

        // Put a new value, which will be distributed and replicated
        System.out.println("\nPutting key '4'...");
        cache.put("4", "Date").join();

        // This get might hit the local node or a remote one, depending on the hash
        System.out.println("Getting key '4': " + cache.get("4").join());

        // Wait a bit to ensure async operations complete before shutdown
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Shut down servers gracefully
        server1.stop();
        server2.stop();
        cache.shutdown();
    }
}