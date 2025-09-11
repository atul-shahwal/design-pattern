package org.concurrency.questions.webcrawler.interview;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🎯 Problem Statement: Low-Level Design - Web Crawler
 *
 * Design a web crawler that traverses web pages starting from a given URL, staying within the same hostname.
 * The system should efficiently process multiple URLs concurrently while avoiding redundant visits.
 *
 * ✅ Requirements:
 * - Crawl URLs only under the same domain as the starting point.
 * - Avoid revisiting already crawled URLs.
 * - Support concurrency using thread pools and atomic counters.
 * - Ensure thread-safe operations on shared data.
 *
 * 📦 Key Components:
 * - HtmlParser interface to extract URLs from a page.
 * - WebCrawler to manage crawling tasks and state.
 * - ExecutorService to process tasks concurrently.
 * - Atomic counters and concurrent maps to track progress and visited URLs.
 *
 * 🚀 Example Flow:
 * 1. Parse URLs from the current page.
 * 2. Filter URLs with the same hostname.
 * 3. Avoid revisiting URLs by checking a thread-safe set.
 * 4. Submit new crawl tasks for each valid URL.
 */

// HtmlParser interface as defined in the problem
interface HtmlParser {
    List<String> getUrls(String url);
}

// Main solution class implementing the web crawler
public class WebCrawler {
    private String hostname;
    private final ConcurrentHashMap<String, Boolean> visited = new ConcurrentHashMap<>();
    private ExecutorService executor;
    private final AtomicInteger pendingTasks = new AtomicInteger(0);
    private HtmlParser htmlParser;

    // Helper method to extract hostname from URL
    private String extractHostname(String url) {
        String[] parts = url.split("/");
        if (parts.length >= 3) {
            return parts[2];
        }
        return "";
    }

    public List<String> crawl(String startUrl, HtmlParser htmlParser) {
        this.hostname = extractHostname(startUrl);
        this.htmlParser = htmlParser;

        // Create a thread pool with optimal size
        executor = Executors.newFixedThreadPool(10);

        // Mark start URL as visited
        visited.put(startUrl, true);

        // Increment task counter and submit first task
        pendingTasks.incrementAndGet();
        executor.submit(new CrawlTask(startUrl));

        // Wait until all tasks are completed
        while (pendingTasks.get() > 0) {
            try {
                Thread.sleep(50); // Avoid busy waiting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Shutdown the executor service
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        // Return all visited URLs
        return new ArrayList<>(visited.keySet());
    }

    // Inner class representing a crawling task that implements Runnable
    class CrawlTask implements Runnable {
        private final String url;

        CrawlTask(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            try {
                // Get all URLs from the current page
                List<String> urls = htmlParser.getUrls(url);

                // Process each URL
                for (String u : urls) {
                    // Check if URL has the same hostname and hasn't been visited
                    if (extractHostname(u).equals(hostname)) {
                        if (visited.putIfAbsent(u, true) == null) {
                            // URL is new, increment counter and submit new task
                            pendingTasks.incrementAndGet();
                            executor.submit(new CrawlTask(u));
                        }
                    }
                }
            } finally {
                // Always decrement the task counter when done
                pendingTasks.decrementAndGet();
            }
        }
    }

    // Mock implementation for testing
    static class MockHtmlParser implements HtmlParser {
        @Override
        public List<String> getUrls(String url) {
            // Simple mock implementation for demonstration
            System.out.println(Thread.currentThread().getName() + " fetching URLs from: " + url);

            Map<String, List<String>> urlMap = new HashMap<>();

            // Define some URL relationships for testing
            urlMap.put("http://news.yahoo.com/news/topics/", Arrays.asList(
                    "http://news.yahoo.com/news/topics/sports",
                    "http://news.yahoo.com/news/topics/technology",
                    "http://news.yahoo.com/news",
                    "http://news.yahoo.com/news/topics/"  // Self-reference
            ));

            urlMap.put("http://news.yahoo.com/news", Arrays.asList(
                    "http://news.yahoo.com/news/topics/",
                    "http://news.yahoo.com/news/politics"
            ));

            urlMap.put("http://news.yahoo.com/news/topics/sports", Arrays.asList(
                    "http://news.yahoo.com/news/topics/sports/football",
                    "http://news.yahoo.com/news/topics/sports/basketball"
            ));

            // Return URLs for the requested page, or empty list if not found
            return urlMap.getOrDefault(url, Collections.emptyList());
        }
    }

    // Main method for testing
    public static void main(String[] args) {
        WebCrawler crawler = new WebCrawler();
        HtmlParser htmlParser = new MockHtmlParser();

        List<String> result = crawler.crawl("http://news.yahoo.com/news/topics/", htmlParser);

        System.out.println("\nDiscovered URLs:");
        for (String url : result) {
            System.out.println(url);
        }
    }
}