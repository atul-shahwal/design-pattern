package org.concurrency.questions.kafka;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;

// Design Pattern: Observer (Pub-Sub)
interface IPublisher {
    String getId();
    void publish(String topicId, Message message);
}

interface ISubscriber {
    String getId();
    void onMessage(Message message) throws InterruptedException;
}

// Core message entity
class Message {
    private final String message;
    public Message(String message) { this.message = message; }
    public String getMessage() { return message; }
}

// Manages messages for a topic
class Topic {
    private final String topicId;
    private final String topicName;
    private final List<Message> messages = new CopyOnWriteArrayList<>();

    public Topic(String topicName, String topicId) {
        this.topicName = topicName;
        this.topicId = topicId;
    }

    public void addMessage(Message message) {
        messages.add(message);
    }

    public Message getMessageAt(int index) {
        return messages.get(index);
    }

    public int size() {
        return messages.size();
    }

    public String getTopicId() { return topicId; }
    public String getTopicName() { return topicName; }
}

// Tracks subscriber progress within a topic
class TopicSubscriber {
    private final Topic topic;
    private final ISubscriber subscriber;
    private final AtomicInteger offset = new AtomicInteger(0);

    // Lock and condition for subscriber's offset index
    private final Lock lock = new ReentrantLock();
    private final Condition newMessageAvailable = lock.newCondition();

    // Consumer log
    private final List<String> consumedLog = new CopyOnWriteArrayList<>();

    public TopicSubscriber(Topic topic, ISubscriber subscriber) {
        this.topic = topic;
        this.subscriber = subscriber;
    }

    public Topic getTopic() { return topic; }
    public ISubscriber getSubscriber() { return subscriber; }
    public AtomicInteger getOffset() { return offset; }

    public Lock getLock() { return lock; }
    public Condition getCondition() { return newMessageAvailable; }

    public void logConsumption(Message msg) {
        consumedLog.add("Consumed by " + subscriber.getId() +
                " from topic " + topic.getTopicName() +
                " at offset " + (offset.get() - 1) +
                " -> " + msg.getMessage());
    }

    public List<String> getConsumedLog() {
        return consumedLog;
    }
}

// Controller
class KafkaController {
    private final Map<String, Topic> topics = new ConcurrentHashMap<>();
    private final Map<String, List<TopicSubscriber>> topicSubscribers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final AtomicInteger topicIdCounter = new AtomicInteger(0);
    private volatile boolean running = true;

    public Topic createTopic(String topicName) {
        String topicId = "T" + topicIdCounter.incrementAndGet();
        Topic topic = new Topic(topicName, topicId);
        topics.put(topicId, topic);
        topicSubscribers.put(topicId, new CopyOnWriteArrayList<>());
        return topic;
    }

    public void subscribe(ISubscriber subscriber, String topicId) {
        Topic topic = topics.get(topicId);
        if (topic == null) throw new IllegalArgumentException("Unknown topic");

        TopicSubscriber ts = new TopicSubscriber(topic, subscriber);
        topicSubscribers.get(topicId).add(ts);

        executor.submit(() -> {
            try {
                while (running) {
                    ts.getLock().lock();
                    try {
                        while (ts.getOffset().get() >= topic.size()) {
                            ts.getCondition().await();
                            if (!running) return; // exit if shutdown
                        }
                        Message msg = topic.getMessageAt(ts.getOffset().getAndIncrement());
                        ts.getSubscriber().onMessage(msg);
                        ts.logConsumption(msg); // ✅ log consumed message
                    } finally {
                        ts.getLock().unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public void publish(IPublisher publisher, String topicId, Message message) {
        Topic topic = topics.get(topicId);
        if (topic == null) throw new IllegalArgumentException("Unknown topic");

        topic.addMessage(message);

        // notify all subscribers of this topic
        topicSubscribers.get(topicId).forEach(ts -> {
            ts.getLock().lock();
            try {
                ts.getCondition().signal();
            } finally {
                ts.getLock().unlock();
            }
        });
    }

    public void shutdown() {
        running = false;
        // wake up all waiting subscribers so they can exit
        topicSubscribers.values().forEach(list -> list.forEach(ts -> {
            ts.getLock().lock();
            try {
                ts.getCondition().signalAll();
            } finally {
                ts.getLock().unlock();
            }
        }));
        executor.shutdownNow();
    }

    // ✅ Show consumer logs for debugging
    public void printConsumerLogs() {
        topicSubscribers.values().forEach(list ->
                list.forEach(ts -> {
                    System.out.println("---- Consumer log for " + ts.getSubscriber().getId() + " ----");
                    ts.getConsumedLog().forEach(System.out::println);
                })
        );
    }
}

// Concrete implementations
class SimplePublisher implements IPublisher {
    private final String id;
    private final KafkaController controller;

    public SimplePublisher(String id, KafkaController controller) {
        this.id = id;
        this.controller = controller;
    }

    public String getId() { return id; }
    public void publish(String topicId, Message message) {
        controller.publish(this, topicId, message);
    }
}

class SimpleSubscriber implements ISubscriber {
    private final String id;
    public SimpleSubscriber(String id) { this.id = id; }
    public String getId() { return id; }
    public void onMessage(Message message) {
        System.out.println(id + " received: " + message.getMessage());
    }
}

// Driver class
public class KafkaPubSub {
    public static void main(String[] args) throws InterruptedException {
        KafkaController controller = new KafkaController();

        Topic t1 = controller.createTopic("Orders");
        Topic t2 = controller.createTopic("Logs");

        ISubscriber s1 = new SimpleSubscriber("S1");
        ISubscriber s2 = new SimpleSubscriber("S2");

        controller.subscribe(s1, t1.getTopicId());
        controller.subscribe(s2, t2.getTopicId());

        IPublisher p1 = new SimplePublisher("P1", controller);
        p1.publish(t1.getTopicId(), new Message("Order1"));
        p1.publish(t2.getTopicId(), new Message("Log1"));
        p1.publish(t1.getTopicId(), new Message("Order2"));

        Thread.sleep(1000);
        controller.shutdown();

        // ✅ Print consumer logs
        controller.printConsumerLogs();
    }
}
