package org.concurrency.questions.ratelimiter;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * 🎯 Problem Statement: Low-Level Design - Rate Limiter
 *
 * Design a rate limiter that controls access frequency using algorithms like Token Bucket.
 * The system should support global and per-user limits while ensuring thread safety and configurability.
 *
 * ✅ Requirements:
 * - Enforce limits based on requests per user or globally.
 * - Implement refill logic with configurable rates.
 * - Support pluggable algorithms like Token Bucket, Fixed Window.
 * - Ensure thread safety with proper synchronization.
 *
 * 📦 Key Components:
 * - IRateLimiter interface for algorithm abstraction.
 * - TokenBucketStrategy for managing tokens.
 * - RateLimiterFactory for creating strategies.
 * - RateLimiterController for handling requests.
 *
 * 🚀 Example Flow:
 * 1. Incoming request is checked against available tokens.
 * 2. Tokens are consumed if available; otherwise, access is blocked.
 * 3. Scheduled task refills tokens at fixed intervals.
 */

interface IRateLimiter {
    boolean giveAccess(String rateLimitKey);
    void updateConfiguration(Map<String, Object> config);
    void shutdown();
}

// ===============================
// 2. RateLimiterType Enum
// ===============================
/**
 * Enumeration of supported rate limiter types.
 */
enum RateLimiterType {
    TOKEN_BUCKET,
    FIXED_WINDOW,
    SLIDING_WINDOW,
    LEAKY_BUCKET
}

// ===============================
// 3. TokenBucketStrategy Class (Strategy Implementation)
// ===============================
/**
 * Token Bucket algorithm implementation.
 * Supports both global and per-user rate limiting.
 */
class TokenBucketStrategy implements IRateLimiter {
    private final int bucketCapacity;
    private volatile int refreshRate;
    private final Bucket globalBucket;
    private final ConcurrentHashMap<String, Bucket> userBuckets;
    private final ScheduledExecutorService scheduler;
    private final long refillIntervalMillis;

    /**
     * Inner class representing an individual token bucket.
     * Uses ReentrantLock for thread-safe operations.
     */
    private class Bucket {
        private int tokens;
        private final ReentrantLock lock = new ReentrantLock();

        public Bucket(int initialTokens) {
            this.tokens = initialTokens;
        }

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

        public void refill() {
            lock.lock();
            try {
                tokens = Math.min(bucketCapacity, tokens + refreshRate);
            } finally {
                lock.unlock();
            }
        }
    }

    public TokenBucketStrategy(int bucketCapacity, int refreshRate) {
        this.bucketCapacity = bucketCapacity;
        this.refreshRate = refreshRate;
        this.refillIntervalMillis = 1000;
        this.globalBucket = new Bucket(bucketCapacity);
        this.userBuckets = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        startRefillTask();
    }

    private void startRefillTask() {
        scheduler.scheduleAtFixedRate(() -> {
            globalBucket.refill();
            for (Bucket bucket : userBuckets.values()) {
                bucket.refill();
            }
        }, 0, refillIntervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean giveAccess(String rateLimitKey) {
        if (rateLimitKey != null && !rateLimitKey.isEmpty()) {
            Bucket bucket = userBuckets.computeIfAbsent(
                    rateLimitKey, key -> new Bucket(bucketCapacity));
            return bucket.tryConsume();
        } else {
            return globalBucket.tryConsume();
        }
    }

    @Override
    public void updateConfiguration(Map<String, Object> config) {
        if (config.containsKey("refreshRate")) {
            this.refreshRate = (int) config.get("refreshRate");
        }
    }

    @Override
    public void shutdown() {
        scheduler.shutdownNow();
    }
}

// ===============================
// 4. RateLimiterFactory Class (Factory Pattern)
// ===============================
/**
 * Factory class that creates rate limiter instances.
 */
class RateLimiterFactory {
    private static final Map<RateLimiterType, Function<Map<String, Object>, IRateLimiter>> limiterFactories = new HashMap<>();

    static {
        limiterFactories.put(RateLimiterType.TOKEN_BUCKET, config -> {
            int capacity = (int) config.getOrDefault("capacity", 10);
            int refreshRate;
            if (config.containsKey("refreshRate")) {
                refreshRate = (int) config.get("refreshRate");
            } else {
                double tokensPerSecond = (double) config.getOrDefault("tokensPerSecond", 10.0);
                refreshRate = (int) Math.round(tokensPerSecond);
            }
            return new TokenBucketStrategy(capacity, refreshRate);
        });
    }

    public static IRateLimiter createLimiter(RateLimiterType type, Map<String, Object> config) {
        Function<Map<String, Object>, IRateLimiter> factory = limiterFactories.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("Unsupported rate limiter type: " + type);
        }
        return factory.apply(config);
    }

    public static void registerLimiterFactory(RateLimiterType type,
                                              Function<Map<String, Object>, IRateLimiter> factory) {
        limiterFactories.put(type, factory);
    }
}

// ===============================
// 5. RateLimiterController Class (Facade Pattern)
// ===============================
/**
 * Controller that delegates incoming requests to a rate limiter.
 */
class RateLimiterController {
    private final IRateLimiter rateLimiter;
    private final ExecutorService executor;

    public RateLimiterController(RateLimiterType type, Map<String, Object> config,
                                 ExecutorService executorService) {
        this.rateLimiter = RateLimiterFactory.createLimiter(type, config);
        this.executor = executorService;
    }

    public CompletableFuture<Boolean> processRequest(String rateLimitKey) {
        return CompletableFuture.supplyAsync(() -> {
            boolean allowed = rateLimiter.giveAccess(rateLimitKey);
            System.out.printf("Request [%s]: %s%n",
                    rateLimitKey != null ? rateLimitKey : "global",
                    allowed ? "✅ Allowed" : "❌ Blocked");
            return allowed;
        }, executor);
    }

    public void updateConfiguration(Map<String, Object> config) {
        rateLimiter.updateConfiguration(config);
    }

    public void shutdown() {
        rateLimiter.shutdown();
        executor.shutdownNow();
    }
}

// ===============================
// 6. Main Demo Class
// ===============================
public class RateLimiterDemo {
    public static void main(String[] args) {
        // Configuration
        Map<String, Object> config = new HashMap<>();
        config.put("capacity", 5);
        config.put("refreshRate", 1);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        RateLimiterController controller = new RateLimiterController(
                RateLimiterType.TOKEN_BUCKET, config, executor);

        // Demo scenarios
        demoGlobalLimiting(controller);
        demoPerUserLimiting(controller);
        demoConcurrentRequests(controller);

        controller.shutdown();
        executor.shutdown();
    }

    private static void demoGlobalLimiting(RateLimiterController controller) {
        System.out.println("=== Global Rate Limiting (10 requests) ===");
        sendRequests(controller, 10, null);
    }

    private static void demoPerUserLimiting(RateLimiterController controller) {
        System.out.println("\n=== Per-User Rate Limiting ===");
        Arrays.asList("user1", "user2").forEach(user -> {
            System.out.println("User: " + user);
            sendRequests(controller, 7, user);
        });
    }

    private static void demoConcurrentRequests(RateLimiterController controller) {
        System.out.println("\n=== Concurrent Requests (20 requests) ===");
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            futures.add(controller.processRequest(null));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long allowed = futures.stream().filter(CompletableFuture::join).count();
        System.out.printf("Result: %d allowed, %d blocked%n", allowed, 20 - allowed);
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