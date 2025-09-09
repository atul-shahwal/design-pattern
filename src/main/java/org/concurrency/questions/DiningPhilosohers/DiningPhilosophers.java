package org.concurrency.questions.DiningPhilosohers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * 🎯 Problem Statement: Dining Philosophers
 *
 * This is a classic concurrency problem to illustrate the dangers of deadlock and starvation.
 * The goal is to design a protocol where five philosophers, who need two forks to eat,
 * can do so without getting stuck in a circular wait (deadlock).
 *
 * ✅ Requirements:
 * - Prevent deadlock, where no philosopher can proceed.
 * - Ensure fairness and prevent starvation, where a philosopher can never eat.
 * - The solution must be thread-safe.
 *
 * 📦 Key Components:
 * - A main Semaphore to limit concurrent access.
 * - An array of Semaphores, one for each fork.
 * - The 'wantsToEat' method that encapsulates the eating protocol.
 *
 * 🚀 Example Flow:
 * 1. A philosopher wants to eat and acquires a permit from the main semaphore.
 * 2. They acquire their left and right forks (semaphores).
 * 3. They eat, then release both forks.
 * 4. They release the permit from the main semaphore, allowing another philosopher to proceed.
 */
public class DiningPhilosophers {
    // 🚦 Semaphore to limit the number of philosophers trying to eat simultaneously.
    // By allowing at most N-1 philosophers (4 in this case) to attempt to eat,
    // we guarantee that at least one philosopher can always pick up both forks,
    // thus preventing deadlock.
    private final Semaphore semaphore;

    // 🥢 An array of Semaphores to control access to each fork.
    // Each fork is a shared resource, and its semaphore acts as a binary lock (permit=1).
    private final Semaphore[] forkSemaphore;

    public DiningPhilosophers() {
        // Initialize the main semaphore with N-1 permits (5-1=4).
        semaphore = new Semaphore(4);

        // Initialize semaphores for each of the 5 forks, each with a single permit.
        forkSemaphore = new Semaphore[5];
        for (int i = 0; i < 5; i++) {
            forkSemaphore[i] = new Semaphore(1);
        }
    }

    /**
     * The core method that a philosopher calls when they want to eat.
     * This method implements the protocol to acquire and release resources safely.
     *
     * @param philosopher   The ID of the philosopher (0 to 4).
     * @param pickLeftFork  A Runnable to execute when picking up the left fork.
     * @param pickRightFork A Runnable to execute when picking up the right fork.
     * @param eat           A Runnable to execute for the eating action.
     * @param putLeftFork   A Runnable to execute when putting down the left fork.
     * @param putRightFork  A Runnable to execute when putting down the right fork.
     * @throws InterruptedException if the thread is interrupted while acquiring permits.
     */
    public void wantsToEat(int philosopher, Runnable pickLeftFork, Runnable pickRightFork,
                           Runnable eat, Runnable putLeftFork, Runnable putRightFork) throws InterruptedException {

        // Step 1: Acquire a permit to enter the 'eating' section.
        // This ensures a max of 4 philosophers can try to eat at the same time.
        semaphore.acquire();

        // Step 2: Identify the forks needed by this philosopher.
        int left = philosopher;
        int right = (philosopher + 1) % 5;

        Semaphore leftForkSemaphore = forkSemaphore[left];
        Semaphore rightForkSemaphore = forkSemaphore[right];

        // Step 3: Acquire both forks (resources).
        // This is a blocking call that waits until the forks are available.
        leftForkSemaphore.acquire();
        rightForkSemaphore.acquire();

        // Step 4: Perform the eating process.
        pickLeftFork.run();
        pickRightFork.run();
        eat.run();

        // Step 5: Put down the forks and release the resources.
        putLeftFork.run();
        leftForkSemaphore.release();
        putRightFork.run();
        rightForkSemaphore.release();

        // Step 6: Release the permit from the main semaphore.
        // This allows another waiting philosopher to try and eat.
        semaphore.release();
    }

    /**
     * The main method to demonstrate the Dining Philosophers problem and its solution.
     * It sets up the threads and executes the simulation.
     */
    public static void main(String[] args) {
        DiningPhilosophers diningPhilosophers = new DiningPhilosophers();
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        for (int philosopher = 0; philosopher < 5; philosopher++) {
            final int id = philosopher;
            executorService.submit(() -> {
                try {
                    diningPhilosophers.wantsToEat(
                            id,
                            () -> System.out.println("Philosopher " + id + " picked up left fork"),
                            () -> System.out.println("Philosopher " + id + " picked up right fork"),
                            () -> {
                                System.out.println("Philosopher " + id + " is eating");
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            },
                            () -> System.out.println("Philosopher " + id + " put down left fork"),
                            () -> System.out.println("Philosopher " + id + " put down right fork")
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        executorService.shutdown();
    }
}