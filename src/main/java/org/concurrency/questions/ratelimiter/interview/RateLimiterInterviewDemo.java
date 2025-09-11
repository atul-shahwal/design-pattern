package org.concurrency.questions.ratelimiter.interview;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 🎯 Problem Statement: Low-Level Design - Rate Limiter
 *
 * Design a rate limiter that controls access frequency using algorithms like Token Bucket.
 * The system supports global and per-user limits while ensuring thread safety and configurability.
 *
 * ✅ Requirements:
 * - Enforce limits globally and per user.
 * - Implement refill logic with configurable rates.
 * - Support pluggable algorithms like Token Bucket.
 * - Ensure thread safety with proper synchronization.
 *
 * 📦 Key Components:
 * - RateLimitingStrategy interface for abstraction.
 * - TokenBucketRateLimitingStrategy for token management.
 * - RateLimiterContext for handling requests and concurrency.
 *
 * 📌 Notes:
 * 1. This design focuses on a single-node in-memory implementation.
 * 2. TokenBucketLimiter:
 *    → Controls request rates using tokens.
 *    → Supports both global and per-user buckets.
 *    → Uses ScheduledExecutorService for periodic refills.
 * 3. ReentrantLock ensures thread safety during token consumption and refill.
 * 4. CompletableFuture in the controller allows async request handling.
 *
 * 📈 Future Scope:
 * 1. ✅ **Factory Pattern Implementation**:
 *    → Introduce a Factory Pattern to dynamically create different rate limiter algorithms
 *      (e.g., Leaky Bucket, Fixed Window, Sliding Window).
 *    → Improves extensibility and modularity for algorithm switching.
 *
 * 2. ✅ **Distributed Rate Limiting**:
 *    → Extend from a single-node to a distributed environment using centralized stores like Redis or etcd.
 *    → Ensure consistency and atomic operations across nodes.
 *
 * 3. ✅ **Persistence & Recovery**:
 *    → Store token states periodically to prevent data loss during restarts.
 *    → Use databases or distributed caches to back up state.
 *
 * 4. ✅ **Monitoring & Analytics**:
 *    → Integrate with monitoring tools to track requests, token refill rates, and limits reached.
 *    → Provide dashboards for real-time visibility.
 *
 * 5. ✅ **Rate Limiter Policies**:
 *    → Support dynamic adjustment of limits based on load, time of day, or user tiers.
 *    → Implement fallback strategies when limits are exceeded.
 *
 * 6. ✅ **Security & Abuse Detection**:
 *    → Detect anomalous usage patterns and throttle suspicious activities.
 *    → Integrate with authentication systems for better access control.
 *
 * 7. ✅ **Testing & Validation Framework**:
 *    → Add unit tests, integration tests, and chaos testing to ensure correctness under concurrency.
 *    → Simulate failure scenarios in distributed setups.
 *
 * 8. ✅ **Configuration Management**:
 *    → Allow limits and refill rates to be dynamically updated via config files or APIs without redeployment.
 */
interface RateLimitingStrategy {
    boolean allowRequest(String key);

    void updateConfig(int capacity, int refillRate);

    void shutdown();
}

/**
 * ✅ Why use an interface?
 * It abstracts implementation details and allows us to extend to other algorithms later.
 */
class TokenBucketRateLimitingStrategy  implements RateLimitingStrategy {

    private final int capacity;
    private volatile int refillRate;
    private final Bucket globalBucket;
    private final ConcurrentHashMap<String, Bucket> userBuckets;
    private final ScheduledExecutorService scheduler;
    private final long refillIntervalMillis;

    public TokenBucketRateLimitingStrategy (int capacity, int refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.refillIntervalMillis = 1000; // refill every second
        this.globalBucket = new Bucket(capacity);
        this.userBuckets = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        startRefillTask();
    }

    /**
     * ✅ Why use an inner Bucket class?
     * It encapsulates token state and locking logic for better cohesion.
     */
    private class Bucket {
        private int tokens;
        private final ReentrantLock lock = new ReentrantLock();

        public Bucket(int initialTokens) {
            this.tokens = initialTokens;
        }

        /**
         * ✅ Why lock here instead of using synchronized?
         * ReentrantLock gives finer-grained control and better performance under contention.
         */
        public boolean tryConsume() {
            lock.lock();
            try {
                if (tokens > 0) {
                    tokens--;
                    return true;
                }
                return false;
            } finally {
                lock.unlock();
            }
        }

        /**
         * ✅ Why use Math.min when refilling tokens?
         * To ensure tokens never exceed the bucket capacity.
         */
        public void refill() {
            lock.lock();
            try {
                tokens = Math.min(capacity, tokens + refillRate);
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * ✅ Why use ScheduledExecutorService for refill tasks?
     * It allows background refill without blocking request threads.
     */
    private void startRefillTask() {
        scheduler.scheduleAtFixedRate(() -> {
            globalBucket.refill();
            for (Bucket bucket : userBuckets.values()) {
                bucket.refill();
            }
        }, 0, refillIntervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean allowRequest(String key) {
        if (key == null || key.isEmpty()) {
            return globalBucket.tryConsume();
        } else {
            Bucket bucket = userBuckets.computeIfAbsent(key, k -> new Bucket(capacity));
            return bucket.tryConsume();
        }
    }

    @Override
    public void updateConfig(int capacity, int refillRate) {
        // Capacity is fixed at runtime; only refill rate is updated
        this.refillRate = refillRate;
    }

    @Override
    public void shutdown() {
        scheduler.shutdownNow();
    }
}

/**
 * ✅ Why a controller class?
 * It provides a unified interface and handles concurrency using executor service.
 */
class RateLimiterController {

    private final RateLimitingStrategy limiter;
    private final ExecutorService executor;

    public RateLimiterController(RateLimitingStrategy limiter, ExecutorService executor) {
        this.limiter = limiter;
        this.executor = executor;
    }

    /**
     * ✅ Why CompletableFuture?
     * It provides asynchronous, non-blocking request processing, simulating real-world scenarios.
     */
    public CompletableFuture<Boolean> processRequest(String key) {
        return CompletableFuture.supplyAsync(() -> {
            boolean allowed = limiter.allowRequest(key);
            System.out.printf("Request [%s]: %s%n", key == null ? "global" : key,
                    allowed ? "✅ Allowed" : "❌ Blocked");
            return allowed;
        }, executor);
    }

    public void updateConfig(int capacity, int refillRate) {
        limiter.updateConfig(capacity, refillRate);
    }

    public void shutdown() {
        limiter.shutdown();
        executor.shutdownNow();
    }
}

/**
 * ✅ Why include a demo in interviews?
 * It helps explain test cases, concurrency handling, and edge cases.
 */
public class RateLimiterInterviewDemo {

    public static void main(String[] args) {
        int capacity = 5;
        int refillRate = 1;
        ExecutorService executor = Executors.newFixedThreadPool(4);

        // Create the limiter without a factory pattern
        TokenBucketRateLimitingStrategy  tokenBucket = new TokenBucketRateLimitingStrategy (capacity, refillRate);
        RateLimiterController controller = new RateLimiterController(tokenBucket, executor);

        testGlobalLimiting(controller);
        testPerUserLimiting(controller);

        controller.shutdown();
    }

    private static void testGlobalLimiting(RateLimiterController controller) {
        System.out.println("=== Global Rate Limiting Test ===");
        sendRequests(controller, 10, null);
    }

    private static void testPerUserLimiting(RateLimiterController controller) {
        System.out.println("\n=== Per-User Rate Limiting Test ===");
        sendRequests(controller, 7, "user1");
        sendRequests(controller, 7, "user2");
    }

    private static void sendRequests(RateLimiterController controller, int count, String key) {
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            futures.add(controller.processRequest(key));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long allowed = futures.stream().filter(CompletableFuture::join).count();
        System.out.printf("Allowed: %d, Blocked: %d%n", allowed, count - allowed);
    }
}

