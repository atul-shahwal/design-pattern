package org.concurrency.questions.boundedblockingqueue;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * BoundedBlockingQueue implementation using ReentrantLock and Condition variables
 *
 * Interview Questions and Answers:
 *
 * Q: Why did you choose ReentrantLock over synchronized blocks?
 * A: ReentrantLock provides more flexibility than synchronized blocks, including:
 *    1. Ability to use multiple Condition variables (notFull, notEmpty)
 *    2. Support for fair locking policies to prevent thread starvation
 *    3. Try-lock functionality with timeout support
 *    4. Better monitoring and debugging capabilities
 *
 * Q: What is the difference between signal() and signalAll()?
 * A: signal() wakes up one waiting thread, while signalAll() wakes up all waiting threads.
 *    In our implementation, we use signal() because we know that only one thread can make progress
 *    after each operation (one producer can add after a consumer removes, or vice versa).
 *
 * Q: Why use while loops instead of if conditions to check queue state?
 * A: While loops prevent "spurious wakeups" - when a thread wakes up from await() without being signaled.
 *    The loop rechecks the condition to ensure it's still valid before proceeding with the operation.
 *
 * Q: What is the purpose of the finally block in each method?
 * A: To ensure the lock is always released, even if an exception occurs during the operation,
 *    preventing deadlocks and ensuring other threads can acquire the lock.
 */
class BoundedBlockingQueue {
    private final Queue<Integer> queue;
    private final int capacity;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    /**
     * Constructor for BoundedBlockingQueue
     *
     * Q: Why is the capacity parameter important?
     * A: The capacity defines the maximum size of the queue, which:
     *    1. Prevents memory exhaustion by limiting queue growth
     *    2. Provides backpressure to producers when consumers can't keep up
     *    3. Helps balance throughput between producers and consumers
     */
    public BoundedBlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.queue = new LinkedList<>();
    }

    /**
     * Adds an element to the queue, blocking if the queue is full
     */
    public void enqueue(int element) throws InterruptedException {
        lock.lock();
        try {
            // Wait while queue is full
            while (queue.size() == capacity) {
                notFull.await();
            }
            // Add element to queue
            queue.offer(element);
            // Signal that queue is no longer empty
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes and returns an element from the queue, blocking if the queue is empty
     *
     * Q: Why does dequeue() return a primitive int rather than an Integer?
     * A: Since we're using a blocking queue, we guarantee the queue won't be empty
     *    when we dequeue (thanks to the await/signal mechanism), so we can safely
     *    return a primitive without worrying about null values.
     */
    public int dequeue() throws InterruptedException {
        lock.lock();
        try {
            // Wait while queue is empty
            while (queue.isEmpty()) {
                notEmpty.await();
            }
            // Remove element from queue
            int element = queue.poll();
            // Signal that queue is no longer full
            notFull.signal();
            return element;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the current number of elements in the queue
     *
     * Q: Why is the size() method synchronized with a lock?
     * A: To ensure thread-safe access to the queue size. Without the lock, we might
     *    get an inconsistent view of the queue size if other threads are modifying
     *    the queue concurrently.
     */
    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Optional: Non-blocking enqueue attempt with timeout
     *
     * Q: Why would you add a timeout version of enqueue?
     * A: In real-world systems, we often need to avoid indefinite blocking. Timeouts
     *    help prevent deadlocks and allow for graceful degradation when the system
     *    is under heavy load.
     */
    public boolean tryEnqueue(int element, long timeoutMs) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeoutMs;
        lock.lock();
        try {
            while (queue.size() == capacity) {
                long remaining = endTime - System.currentTimeMillis();
                if (remaining <= 0) {
                    return false; // Timeout expired
                }
                notFull.await(remaining, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
            queue.offer(element);
            notEmpty.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }
}

public class BoundedBlockingQueueTest {
    public static void main(String[] args) {
        BoundedBlockingQueue queue = new BoundedBlockingQueue(5);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger atomicInt = new AtomicInteger(1);

        // Producer threads
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    int element = atomicInt.getAndIncrement();
                    queue.enqueue(element);
                    System.out.println(Thread.currentThread().getName() + " enqueued: " + element);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Consumer threads
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    int element = queue.dequeue();
                    System.out.println(Thread.currentThread().getName() + " dequeued: " + element);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();

        // Wait for all tasks to complete
        while (!executor.isTerminated()) {
            // Busy wait (not ideal for production, but fine for this demo)
        }

        System.out.println("Final queue size: " + queue.size());
    }
}