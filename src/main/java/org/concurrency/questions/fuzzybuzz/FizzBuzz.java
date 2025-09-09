package org.concurrency.questions.fuzzybuzz;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.IntConsumer;

/**
 * 🎯 Problem Statement: FizzBuzz Multithreaded
 *
 * This is a classic concurrency problem where four threads need to print the numbers
 * from 1 to n in a specific order:
 * - Thread 1 prints "Fizz" for numbers divisible by 3.
 * - Thread 2 prints "Buzz" for numbers divisible by 5.
 * - Thread 3 prints "FizzBuzz" for numbers divisible by both 3 and 5.
 * - Thread 4 prints the number itself for all other cases.
 * The core challenge is coordinating the threads to ensure the output is in the correct sequence.
 *
 * ✅ Approach: Semaphore-based Coordination
 *
 * We use a set of Semaphores to control the flow of execution.
 * - The 'number' thread acts as the central coordinator, or "leader."
 * - It acquires a semaphore and then decides which of the other three threads should proceed next
 * by releasing the appropriate semaphore.
 * - Each of the other threads ('fizz', 'buzz', 'fizzbuzz') waits for its specific semaphore to be released
 * before printing its output.
 *
 * 📦 Key Components:
 * - `n`: The limit for the FizzBuzz sequence.
 * - `numberSemaphore`: Acts as the initial gate, allowing the coordinator to proceed.
 * - `fizzSemaphore`, `buzzSemaphore`, `fizzBuzzSemaphore`: Permits for the "worker" threads,
 * initialized to zero, so they start in a blocked state.
 *
 * 🚀 Example Flow:
 * 1. The 'number' thread acquires its permit.
 * 2. It checks the number 'i' and releases a permit for the correct worker thread (e.g., 'fizzbuzz').
 * 3. The 'fizzbuzz' thread wakes up, acquires its permit, prints "FizzBuzz", and then
 * releases the `numberSemaphore` to signal the coordinator to continue to the next number.
 */
class FizzBuzz {
    // The upper limit for the FizzBuzz sequence.
    private int n;

    // 🚦 Semaphore to allow only the correct thread to print numbers.
    // The number thread is the "leader" and holds the main permit.
    private Semaphore numberSemaphore;

    // 🚦 Semaphore for the "Fizz" thread. It starts with 0 permits,
    // so the thread will be blocked until the number thread signals it.
    private Semaphore fizzSemaphore;

    // 🚦 Semaphore for the "Buzz" thread. It also starts with 0 permits.
    private Semaphore buzzSemaphore;

    // 🚦 Semaphore for the "FizzBuzz" thread. It also starts with 0 permits.
    private Semaphore fizzBuzzSemaphore;

    /**
     * Constructs the FizzBuzz object, initializing the semaphores and the number limit.
     *
     * @param n The number up to which FizzBuzz needs to be calculated.
     */
    public FizzBuzz(int n) {
        this.n = n;

        // The number thread is the leader and starts with the only available permit.
        this.numberSemaphore = new Semaphore(1);

        // The worker threads start with no permits and must wait for the leader to signal them.
        this.fizzSemaphore = new Semaphore(0);
        this.buzzSemaphore = new Semaphore(0);
        this.fizzBuzzSemaphore = new Semaphore(0);
    }

    /**
     * This method is called by a worker thread to print "Fizz".
     * It waits for a permit from the coordinator thread before proceeding.
     *
     * @param printFizz A Runnable to print "Fizz".
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    public void fizz(Runnable printFizz) throws InterruptedException {
        for (int i = 1; i <= n; i++) {
            if (i % 3 == 0 && i % 5 != 0) {
                // Wait for a permit from the number thread to proceed.
                fizzSemaphore.acquire();
                printFizz.run();
                // Release the number semaphore to allow the coordinator to continue.
                numberSemaphore.release();
            }
        }
    }

    /**
     * This method is called by a worker thread to print "Buzz".
     * It waits for a permit from the coordinator thread before proceeding.
     *
     * @param printBuzz A Runnable to print "Buzz".
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    public void buzz(Runnable printBuzz) throws InterruptedException {
        for (int i = 1; i <= n; i++) {
            if (i % 3 != 0 && i % 5 == 0) {
                // Wait for a permit from the number thread to proceed.
                buzzSemaphore.acquire();
                printBuzz.run();
                // Release the number semaphore to allow the coordinator to continue.
                numberSemaphore.release();
            }
        }
    }

    /**
     * This method is called by a worker thread to print "FizzBuzz".
     * It waits for a permit from the coordinator thread before proceeding.
     *
     * @param printFizzBuzz A Runnable to print "FizzBuzz".
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    public void fizzbuzz(Runnable printFizzBuzz) throws InterruptedException {
        for (int i = 1; i <= n; i++) {
            if (i % 3 == 0 && i % 5 == 0) {
                // Wait for a permit from the number thread to proceed.
                fizzBuzzSemaphore.acquire();
                printFizzBuzz.run();
                // Release the number semaphore to allow the coordinator to continue.
                numberSemaphore.release();
            }
        }
    }

    /**
     * This method is the central coordinator for the FizzBuzz sequence.
     * It iterates through numbers, determines the correct output, and signals the
     * appropriate worker thread to proceed.
     *
     * @param printNumber An IntConsumer to print the number.
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    public void number(IntConsumer printNumber) throws InterruptedException {
        for (int i = 1; i <= n; i++) {
            // Wait for a permit from the previous handoff.
            numberSemaphore.acquire();

            // Check divisibility to determine which worker thread to signal.
            if (i % 3 == 0 && i % 5 == 0) {
                fizzBuzzSemaphore.release();
            } else if (i % 3 == 0) {
                fizzSemaphore.release();
            } else if (i % 5 == 0) {
                buzzSemaphore.release();
            } else {
                // If it's a regular number, print it and immediately release
                // the number semaphore for the next iteration.
                printNumber.accept(i);
                numberSemaphore.release();
            }
        }
    }

    public static void main(String[] args) {
        int n = 20;
        FizzBuzz fizzBuzz = new FizzBuzz(n);
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        // Use a try-with-resources block for the ExecutorService to ensure it's shut down automatically.
        try {

            // Submit all four tasks to the thread pool.
            executorService.submit(() -> {
                try {
                    fizzBuzz.fizz(() -> System.out.print("fizz "));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            executorService.submit(() -> {
                try {
                    fizzBuzz.buzz(() -> System.out.print("buzz "));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            executorService.submit(() -> {
                try {
                    fizzBuzz.fizzbuzz(() -> System.out.print("fizzbuzz "));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            executorService.submit(() -> {
                try {
                    fizzBuzz.number(num -> System.out.print(num + " "));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}