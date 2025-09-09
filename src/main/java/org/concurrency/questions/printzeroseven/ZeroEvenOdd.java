package org.concurrency.questions.printzeroseven;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.IntConsumer;

public class ZeroEvenOdd {
    private int n; // The maximum number to print (odd or even)
    private Semaphore zeroSemaphore; // Ensures "0" is printed
    private Semaphore evenSemaphore; // Ensures even numbers are printed
    private Semaphore oddSemaphore; // Ensures odd numbers are printed

    // Constructor to initialize the semaphores and the upper limit
    public ZeroEvenOdd(int n) {
        this.n = n;
        zeroSemaphore = new Semaphore(1); // Initially "0" can be printed, so start with 1 permit
        evenSemaphore = new Semaphore(0); // Even numbers cannot be printed yet
        oddSemaphore = new Semaphore(0); // Odd numbers cannot be printed yet
    }

    // Method to print "0" alternately before odd and even numbers
    // Takes an IntConsumer for printing numbers
    public void zero(IntConsumer printNumber) throws InterruptedException {
        boolean isOdd = true; // Flag to toggle between odd and even numbers
        for (int i = 1; i <= n; i++) {
            zeroSemaphore.acquire(); // Acquire permit to print "0"
            printNumber.accept(0); // Print "0"
            if (isOdd) {
                oddSemaphore.release(); // Allow odd numbers to be printed next
            } else {
                evenSemaphore.release(); // Allow even numbers to be printed next
            }
            isOdd = !isOdd; // Toggle the flag
        }
    }

    // Method to print even numbers
    public void even(IntConsumer printNumber) throws InterruptedException {
        for (int i = 2; i <= n; i += 2) { // Iterate over even numbers up to n
            evenSemaphore.acquire(); // Wait for "0" to be printed and semaphore to be released
            printNumber.accept(i); // Print the even number
            zeroSemaphore.release(); // Release semaphore to allow "0" to be printed next
        }
    }

    // Method to print odd numbers
    public void odd(IntConsumer printNumber) throws InterruptedException {
        for (int i = 1; i <= n; i += 2) { // Iterate over odd numbers up to n
            oddSemaphore.acquire(); // Wait for "0" to be printed and semaphore to be released
            printNumber.accept(i); // Print the odd number
            zeroSemaphore.release(); // Release semaphore to allow "0" to be printed next
        }
    }

    public static void main(String[] args) {
        // Set the limit for the sequence
        int n = 10;

        // Create the ZeroEvenOdd object
        ZeroEvenOdd zeroEvenOdd = new ZeroEvenOdd(n);

        // Create a thread pool for zero, even, and odd printing
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // Runnable to print numbers with thread names
        IntConsumer printNumber = number ->
                System.out.println(Thread.currentThread().getName() + " printed: " + number);

        // Submit the zero, even, and odd printing tasks
        executor.submit(() -> {
            Thread.currentThread().setName("Zero-Thread");
            try {
                zeroEvenOdd.zero(printNumber);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        executor.submit(() -> {
            Thread.currentThread().setName("Even-Thread");
            try {
                zeroEvenOdd.even(printNumber);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        executor.submit(() -> {
            Thread.currentThread().setName("Odd-Thread");
            try {
                zeroEvenOdd.odd(printNumber);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Shutdown the executor service after completion
        executor.shutdown();
    }
}
