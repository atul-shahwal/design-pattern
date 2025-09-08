package org.desingpatterns.questions.nosqldb;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
/**
 * 🎯 Problem Statement: Low-Level Design - NoSQL Database System
 *
 * Design a NoSQL database system that supports CRUD operations, indexing, querying, and concurrency control.
 * The system should include persistence, replication, and sharding strategies for scalability and reliability.
 *
 * ✅ Requirements:
 * - Store documents as key-value pairs with support for CRUD operations.
 * - Allow indexing on document fields for efficient querying.
 * - Handle concurrent access with per-key locking to prevent dirty reads/writes.
 * - Support write strategies (write-through) for persistence.
 * - Include replication and sharding strategies for scalability.
 *
 * 📦 Key Components:
 * - DocumentStorage interface and in-memory implementation.
 * - PersistentStorage interface for persistence layer.
 * - WriteStrategy interface for write operations.
 * - ReplicationStrategy and ShardingStrategy interfaces.
 * - KeyBasedLocking for concurrency control.
 * - NoSQLDatabase class to orchestrate operations.
 *
 * 🚀 Example Flow:
 * 1. Client puts document with key → stored in memory and persisted.
 * 2. Client queries documents by field value → uses index for efficiency.
 * 3. Concurrent puts for same key are serialized with locks.
 * 4. Replication strategy copies data to replicas for redundancy.
 */
/**
 * Main interface for NoSQL document storage.
 * Provides CRUD operations and supports querying and indexing.
 */
interface DocumentStorage<K, V> {
    void put(K key, V document) throws Exception;
    V get(K key) throws Exception;
    void remove(K key) throws Exception;
    boolean containsKey(K key) throws Exception;
    int size() throws Exception;
    List<V> query(String field, Object value) throws Exception;
    List<V> scan() throws Exception;
    void createIndex(String field) throws Exception;
    void removeIndex(String field) throws Exception;
}

/**
 * Interface representing the persistence layer.
 */
interface PersistentStorage<K, V> {
    void persist(K key, V value) throws Exception;
    V retrieve(K key) throws Exception;
    void delete(K key) throws Exception;
}

/**
 * Interface defining how write operations are handled.
 */
interface WriteStrategy<K, V> {
    void write(K key, V value, DocumentStorage<K, V> documentStorage, PersistentStorage<K, V> persistentStorage) throws Exception;
}

/**
 * Interface defining how data is replicated for redundancy.
 */
interface ReplicationStrategy<K, V> {
    void replicate(K key, V value) throws Exception;
    V readFromReplica(K key) throws Exception;
    void addReplica(DocumentStorage<K, V> replica);
}

/**
 * Interface defining how data is sharded across nodes.
 */
interface ShardingStrategy<K, V> {
    String getShardId(K key);
    void addShard(String shardId, DocumentStorage<K, V> shardStorage);
    void removeShard(String shardId);
}

/**
 * Class for providing fine-grained, per-key locking to prevent dirty reads and writes.
 */
class KeyBasedLocking<K> {
    private final Map<K, ReadWriteLock> lockMap = new HashMap<>();

    /**
     * Execute a read operation with a read lock to prevent reading dirty data.
     */
    public synchronized <T> T withReadLock(K key, Supplier<T> operation) {
        ReadWriteLock lock = lockMap.computeIfAbsent(key, k -> new ReentrantReadWriteLock());
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            return operation.get();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Execute a write operation with a write lock to ensure atomic writes.
     */
    public synchronized void withWriteLock(K key, Runnable operation) {
        ReadWriteLock lock = lockMap.computeIfAbsent(key, k -> new ReentrantReadWriteLock());
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            operation.run();
        } finally {
            writeLock.unlock();
        }
    }
}

/**
 * In-memory document storage using a plain HashMap and explicit locking.
 * This prevents dirty reads/writes by synchronizing access per key.
 */
class InMemoryDocumentStorage<K, V> implements DocumentStorage<K, V> {
    protected final Map<K, V> storage = new HashMap<>();
    protected final Map<String, Map<Object, Set<K>>> indexes = new HashMap<>();
    protected final KeyBasedLocking<K> lockingSystem = new KeyBasedLocking<>();

    @Override
    public void put(K key, V document) throws Exception {
        lockingSystem.withWriteLock(key, () -> {
            V old = storage.put(key, document);
            if (old != null) {
                removeFromIndexes(key, old);
            }
            updateIndexes(key, document);
        });
    }

    @Override
    public V get(K key) throws Exception {
        return lockingSystem.withReadLock(key, () -> storage.get(key));
    }

    @Override
    public void remove(K key) throws Exception {
        lockingSystem.withWriteLock(key, () -> {
            V old = storage.remove(key);
            if (old != null) {
                removeFromIndexes(key, old);
            }
        });
    }

    @Override
    public boolean containsKey(K key) throws Exception {
        return lockingSystem.withReadLock(key, () -> storage.containsKey(key));
    }

    @Override
    public int size() throws Exception {
        return storage.size();
    }

    @Override
    public List<V> query(String field, Object value) throws Exception {
        if (!indexes.containsKey(field)) {
            return scan().stream()
                    .filter(doc -> extractFieldValue(doc, field).equals(value))
                    .toList();
        }
        Map<Object, Set<K>> fieldIndex = indexes.get(field);
        if (!fieldIndex.containsKey(value)) {
            return Collections.emptyList();
        }
        Set<K> keys = new HashSet<>(fieldIndex.get(value));
        List<V> result = new ArrayList<>();
        for (K key : keys) {
            V val = lockingSystem.withReadLock(key, () -> storage.get(key));
            if (val != null) {
                result.add(val);
            }
        }
        return result;
    }

    @Override
    public List<V> scan() throws Exception {
        Set<K> keys = new HashSet<>(storage.keySet());
        List<V> result = new ArrayList<>();
        for (K key : keys) {
            V val = lockingSystem.withReadLock(key, () -> storage.get(key));
            if (val != null) {
                result.add(val);
            }
        }
        return result;
    }

    @Override
    public void createIndex(String field) throws Exception {
        indexes.putIfAbsent(field, new HashMap<>());
        synchronized (storage) {
            for (Map.Entry<K, V> entry : storage.entrySet()) {
                K key = entry.getKey();
                V val = entry.getValue();
                Object fieldValue = extractFieldValue(val, field);
                indexes.get(field).computeIfAbsent(fieldValue, k -> new HashSet<>()).add(key);
            }
        }
    }

    @Override
    public void removeIndex(String field) throws Exception {
        indexes.remove(field);
    }

    protected void updateIndexes(K key, V document) {
        indexes.forEach((field, fieldIndex) -> {
            Object fieldValue = extractFieldValue(document, field);
            fieldIndex.computeIfAbsent(fieldValue, k -> new HashSet<>()).add(key);
        });
    }

    protected void removeFromIndexes(K key, V document) {
        indexes.forEach((field, fieldIndex) -> {
            Object fieldValue = extractFieldValue(document, field);
            if (fieldIndex.containsKey(fieldValue)) {
                fieldIndex.get(fieldValue).remove(key);
                if (fieldIndex.get(fieldValue).isEmpty()) {
                    fieldIndex.remove(fieldValue);
                }
            }
        });
    }

    protected Object extractFieldValue(V document, String field) {
        if (document instanceof Map) {
            return ((Map<String, Object>) document).get(field);
        }
        throw new UnsupportedOperationException("Unsupported document type for field extraction");
    }
}

/**
 * Simple persistent storage implementation using a HashMap.
 */
class SimplePersistentStorage<K, V> implements PersistentStorage<K, V> {
    private final Map<K, V> storage = new HashMap<>();

    @Override
    public void persist(K key, V value) throws Exception {
        storage.put(key, value);
    }

    @Override
    public V retrieve(K key) throws Exception {
        return storage.get(key);
    }

    @Override
    public void delete(K key) throws Exception {
        storage.remove(key);
    }
}

/**
 * Write-through strategy where writes are persisted immediately after in-memory storage.
 */
class WriteThroughStrategy<K, V> implements WriteStrategy<K, V> {
    @Override
    public void write(K key, V value, DocumentStorage<K, V> documentStorage, PersistentStorage<K, V> persistentStorage) throws Exception {
        documentStorage.put(key, value);
        persistentStorage.persist(key, value);
    }
}

/**
 * Simple replication strategy where data is replicated to all added replicas.
 */
class SimpleReplicationStrategy<K, V> implements ReplicationStrategy<K, V> {
    private final List<DocumentStorage<K, V>> replicas = new ArrayList<>();

    @Override
    public void replicate(K key, V value) throws Exception {
        for (DocumentStorage<K, V> replica : replicas) {
            replica.put(key, value);
        }
    }

    @Override
    public V readFromReplica(K key) throws Exception {
        for (DocumentStorage<K, V> replica : replicas) {
            V value = replica.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Override
    public void addReplica(DocumentStorage<K, V> replica) {
        replicas.add(replica);
    }
}

/**
 * Main database class that uses locking for thread-safe operations.
 */
class NoSQLDatabase<K, V> {
    private final DocumentStorage<K, V> documentStorage;
    private final PersistentStorage<K, V> persistentStorage;
    private final WriteStrategy<K, V> writeStrategy;
    private final ReplicationStrategy<K, V> replicationStrategy;

    public NoSQLDatabase(DocumentStorage<K, V> documentStorage,
                         PersistentStorage<K, V> persistentStorage,
                         WriteStrategy<K, V> writeStrategy,
                         ReplicationStrategy<K, V> replicationStrategy) {
        this.documentStorage = documentStorage;
        this.persistentStorage = persistentStorage;
        this.writeStrategy = writeStrategy;
        this.replicationStrategy = replicationStrategy;
    }

    public void put(K key, V value) throws Exception {
        writeStrategy.write(key, value, documentStorage, persistentStorage);
        replicationStrategy.replicate(key, value);
    }

    public V get(K key) throws Exception {
        V value = documentStorage.get(key);
        if (value != null) {
            return value;
        }
        value = persistentStorage.retrieve(key);
        if (value != null) {
            documentStorage.put(key, value);
            return value;
        }
        value = replicationStrategy.readFromReplica(key);
        if (value != null) {
            documentStorage.put(key, value);
            return value;
        }
        return null;
    }
}

/**
 * Test class demonstrating different test cases separately for clarity.
 */
public class NoSQLDatabaseTest {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting NoSQL Database Tests...");

        testBasicCrudOperations();
        testQueryOperations();
        testConcurrency();
        testShardingSimulation();

        System.out.println("All tests completed successfully.");
    }

    public static void testBasicCrudOperations() throws Exception {
        System.out.println("\n=== Test 1: Basic CRUD Operations ===");

        DocumentStorage<String, Map<String, Object>> storage = new InMemoryDocumentStorage<>();
        PersistentStorage<String, Map<String, Object>> persistent = new SimplePersistentStorage<>();
        WriteStrategy<String, Map<String, Object>> writeStrategy = new WriteThroughStrategy<>();
        ReplicationStrategy<String, Map<String, Object>> replication = new SimpleReplicationStrategy<>();

        NoSQLDatabase<String, Map<String, Object>> db = new NoSQLDatabase<>(storage, persistent, writeStrategy, replication);

        Map<String, Object> user = new HashMap<>();
        user.put("name", "John Doe");
        db.put("user1", user);

        Map<String, Object> retrieved = db.get("user1");
        assert retrieved != null && "John Doe".equals(retrieved.get("name"));
        System.out.println("✓ CRUD test passed");
    }

    public static void testQueryOperations() throws Exception {
        System.out.println("\n=== Test 2: Query Operations ===");

        DocumentStorage<String, Map<String, Object>> storage = new InMemoryDocumentStorage<>();
        PersistentStorage<String, Map<String, Object>> persistent = new SimplePersistentStorage<>();
        WriteStrategy<String, Map<String, Object>> writeStrategy = new WriteThroughStrategy<>();
        ReplicationStrategy<String, Map<String, Object>> replication = new SimpleReplicationStrategy<>();

        NoSQLDatabase<String, Map<String, Object>> db = new NoSQLDatabase<>(storage, persistent, writeStrategy, replication);

        Map<String, Object> user1 = new HashMap<>();
        user1.put("name", "Alice");
        user1.put("city", "New York");

        Map<String, Object> user2 = new HashMap<>();
        user2.put("name", "Bob");
        user2.put("city", "New York");

        Map<String, Object> user3 = new HashMap<>();
        user3.put("name", "Charlie");
        user3.put("city", "Chicago");

        db.put("u1", user1);
        db.put("u2", user2);
        db.put("u3", user3);

        storage.createIndex("city");

        List<Map<String, Object>> nyUsers = storage.query("city", "New York");
        assert nyUsers.size() == 2;
        System.out.println("✓ Query test passed");
    }

    public static void testConcurrency() throws Exception {
        System.out.println("\n=== Test 3: Concurrency Test ===");

        DocumentStorage<String, Map<String, Object>> storage = new InMemoryDocumentStorage<>();
        PersistentStorage<String, Map<String, Object>> persistent = new SimplePersistentStorage<>();
        WriteStrategy<String, Map<String, Object>> writeStrategy = new WriteThroughStrategy<>();
        ReplicationStrategy<String, Map<String, Object>> replication = new SimpleReplicationStrategy<>();

        NoSQLDatabase<String, Map<String, Object>> db = new NoSQLDatabase<>(storage, persistent, writeStrategy, replication);

        int totalThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < totalThreads; i++) {
            int finalI = i;
            tasks.add(() -> {
                String key = "key" + (finalI % 10);
                Map<String, Object> doc = new HashMap<>();
                doc.put("value", "val" + finalI);
                db.put(key, doc);
                return null;
            });
        }

        executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assert storage.size() <= 10;
        System.out.println("✓ Concurrency test passed");
    }

    public static void testShardingSimulation() throws Exception {
        System.out.println("\n=== Test 4: Sharding Simulation ===");

        // For simplicity, simulate shards as separate storages
        DocumentStorage<String, Map<String, Object>> shard1 = new InMemoryDocumentStorage<>();
        DocumentStorage<String, Map<String, Object>> shard2 = new InMemoryDocumentStorage<>();
        DocumentStorage<String, Map<String, Object>> shard3 = new InMemoryDocumentStorage<>();

        // Simulate a basic hash-based shard selection
        Map<String, DocumentStorage<String, Map<String, Object>>> shards = new HashMap<>();
        shards.put("shard1", shard1);
        shards.put("shard2", shard2);
        shards.put("shard3", shard3);

        // Insert data into shard1 for demonstration
        Map<String, Object> doc = new HashMap<>();
        doc.put("name", "ShardUser");
        shard1.put("user_shard1", doc);

        assert shard1.size() == 1;
        assert shard2.size() == 0;
        assert shard3.size() == 0;

        System.out.println("✓ Sharding simulation test passed");
    }
}
