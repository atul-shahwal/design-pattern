package org.concurrency.questions.kafka;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;


/* -------------------
   FUTURE ACTION ITEMS
   -------------------
   - Implement TTL-based message retention in CustomQueue.
   - Support distributed broker with sharding & replication.
   - Add backpressure handling for slow subscribers.
   - Support batch delivery & async acknowledgment.
   - Add monitoring metrics (delivery count, latency).
   - Add unsubscribe method in SubscriptionStore.
   - Optimize executor strategy (ThreadPool scaling, work-stealing).
*/

/* -------------------
   DOMAIN CLASS: Message
   -------------------
   ❓ Q: Why include timestamp, UUID, and priority in Message?
   ✅ A:
   - Timestamp: ensures FIFO within same priority.
   - UUID: unique identification for tracking/logging.
   - Priority: supports priority-based delivery to subscribers.
*/
class Message implements Comparable<Message> {
    private final String content;
    private final long timestamp;
    private final String messageId;
    private final int priority;

    public Message(String content) {
        this(content, 0);
    }

    public Message(String content, int priority) {
        this.content = content;
        this.priority = priority;
        this.timestamp = System.currentTimeMillis();
        this.messageId = UUID.randomUUID().toString();
    }

    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
    public String getMessageId() { return messageId; }
    public int getPriority() { return priority; }

    @Override
    public int compareTo(Message o) {
        int p = Integer.compare(o.priority, this.priority); // higher priority first
        if (p == 0) return Long.compare(this.timestamp, o.timestamp);
        return p;
    }
}

/* -------------------
   INTERFACE: Publisher
   ------------------- */
interface Publisher {
    String getId();
    void publish(String topicId, Message message);
}

/* -------------------
   INTERFACE: Subscriber
   ------------------- */
interface Subscriber {
    String getId();
    void onMessage(Message message);
}

/* -------------------
   INTERFACE: CustomQueue
   -------------------
   ❓ Q: Why a custom queue abstraction?
   ✅ A:
   - Allows pluggable message storage (priority, FIFO, TTL, etc.)
   - Decouples Topic from specific data structure.
*/
interface CustomQueue {
    void offer(Message message);
    Message poll();
    int size();
}

/* -------------------
   IMPLEMENTATION: PriorityMessageQueue
   ------------------- */
class PriorityMessageQueue implements CustomQueue {
    private final PriorityBlockingQueue<Message> queue = new PriorityBlockingQueue<>();

    @Override
    public void offer(Message message) {
        queue.offer(message);
    }

    @Override
    public Message poll() {
        return queue.poll();
    }

    @Override
    public int size() {
        return queue.size();
    }
}

/* -------------------
   DOMAIN CLASS: Subscription
   -------------------
   ❓ Q: Why Subscription stores a queue?
   ✅ A:
   - Each subscriber receives messages independently.
   - Queue allows asynchronous processing.
*/
class Subscription {
    private final Subscriber subscriber;
    private final CustomQueue queue;
    private final List<String> consumptionLog = new CopyOnWriteArrayList<>();

    public Subscription(Subscriber subscriber, CustomQueue queue) {
        this.subscriber = subscriber;
        this.queue = queue;
    }

    public void enqueueMessage(Message message) {
        queue.offer(message);
    }

    public void startProcessing(ExecutorService executor) {
        executor.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Message message = queue.poll();
                    if (message != null) {
                        subscriber.onMessage(message);
                        consumptionLog.add("Consumed by " + subscriber.getId() +
                                ": " + message.getContent() +
                                " [msgId=" + message.getMessageId() + "]");
                    } else {
                        Thread.sleep(10); // avoid busy-wait
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public List<String> getConsumptionLog() { return consumptionLog; }
    public Subscriber getSubscriber() { return subscriber; }

    // Hashcode/equals based on subscriber ID for uniqueness in sets
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subscription)) return false;
        Subscription that = (Subscription) o;
        return subscriber.getId().equals(that.subscriber.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriber.getId());
    }
}

/* -------------------
   INTERFACE: SubscriptionStore
   -------------------
   ❓ Q: Why abstract subscription storage?
   ✅ A:
   - Decouples Topic from concrete implementation.
   - Supports different strategies (HashSet, List, distributed store).
*/
interface SubscriptionStore {
    boolean add(Subscription subscription);
    boolean remove(Subscription subscription);
    Collection<Subscription> getAll();
    boolean contains(Subscription subscription);
    int size();
}

/* -------------------
   IMPLEMENTATION: HashSetSubscriptionStore
   ------------------- */
class HashSetSubscriptionStore implements SubscriptionStore {
    private final Set<Subscription> subscriptions = ConcurrentHashMap.newKeySet();

    @Override
    public boolean add(Subscription subscription) {
        return subscriptions.add(subscription);
    }

    @Override
    public boolean remove(Subscription subscription) {
        return subscriptions.remove(subscription);
    }

    @Override
    public Collection<Subscription> getAll() {
        return subscriptions;
    }

    @Override
    public boolean contains(Subscription subscription) {
        return subscriptions.contains(subscription);
    }

    @Override
    public int size() {
        return subscriptions.size();
    }
}

/* -------------------
   DOMAIN CLASS: Topic
   -------------------
   ❓ Q: Why Topic uses SubscriptionStore and CustomQueue?
   ✅ A:
   - SubscriptionStore: decouples storage, ensures uniqueness.
   - CustomQueue: allows pluggable message storage, e.g., priority queue.
*/
class Topic {
    private final String topicId;
    private final String topicName;
    private final int maxRetention;
    private final CustomQueue retentionQueue;
    private final SubscriptionStore subscriptionStore;

    public Topic(String topicName, String topicId, int maxRetention, SubscriptionStore store) {
        this.topicName = topicName;
        this.topicId = topicId;
        this.maxRetention = maxRetention;
        this.retentionQueue = new PriorityMessageQueue();
        this.subscriptionStore = store;
    }

    public void publish(Message message) {
        if (retentionQueue.size() >= maxRetention) retentionQueue.poll();
        retentionQueue.offer(message);
        subscriptionStore.getAll().forEach(sub -> sub.enqueueMessage(message));
    }

    public void addSubscriber(Subscription subscription) {
        subscriptionStore.add(subscription);
    }

    public String getTopicId() { return topicId; }
    public String getTopicName() { return topicName; }
    public Collection<Subscription> getSubscriptions() { return subscriptionStore.getAll(); }
}

/* -------------------
   BROKER CLASS: MessageBroker
   -------------------
   ❓ Q: How broker manages topics and subscriptions?
   ✅ A:
   - Stores topics in ConcurrentHashMap for thread-safe access.
   - Each topic maintains its own subscriptions via SubscriptionStore.
   - ExecutorService for async delivery.
*/
class MessageBroker {
    private final Map<String, Topic> topics = new ConcurrentHashMap<>();
    private final AtomicLong topicCounter = new AtomicLong(0);
    private final int topicMaxRetention = 1000;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public Topic createTopic(String topicName) {
        String topicId = "T" + topicCounter.incrementAndGet();
        Topic topic = new Topic(topicName, topicId, topicMaxRetention, new HashSetSubscriptionStore());
        topics.put(topicId, topic);
        return topic;
    }

    public void subscribe(Subscriber subscriber, String topicId) {
        Topic topic = topics.get(topicId);
        if (topic == null) throw new IllegalArgumentException("Unknown topic: " + topicId);
        Subscription subscription = new Subscription(subscriber, new PriorityMessageQueue());
        topic.addSubscriber(subscription);
        subscription.startProcessing(executor);
    }

    public void publish(Publisher publisher, String topicId, Message message) {
        Topic topic = topics.get(topicId);
        if (topic == null) throw new IllegalArgumentException("Unknown topic: " + topicId);
        topic.publish(message);
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    public void printConsumptionLogs() {
        topics.values().forEach(topic -> {
            topic.getSubscriptions().forEach(sub -> {
                System.out.println("---- Consumption log for " + sub.getSubscriber().getId() + " ----");
                sub.getConsumptionLog().forEach(System.out::println);
            });
        });
    }
}

/* -------------------
   SIMPLE IMPLEMENTATIONS: Publisher & Subscriber
   ------------------- */
class SimplePublisher implements Publisher {
    private final String id;
    private final MessageBroker broker;

    public SimplePublisher(String id, MessageBroker broker) {
        this.id = id;
        this.broker = broker;
    }

    public String getId() { return id; }
    public void publish(String topicId, Message message) {
        broker.publish(this, topicId, message);
    }
}

class SimpleSubscriber implements Subscriber {
    private final String id;
    public SimpleSubscriber(String id) { this.id = id; }
    public String getId() { return id; }
    public void onMessage(Message message) {
        System.out.println(id + " received: " + message.getContent() +
                " [msgId=" + message.getMessageId() + ", priority=" + message.getPriority() + "]");
    }
}

/* -------------------
   DEMO
   ------------------- */
public class MessageBrokerDemo {
    public static void main(String[] args) throws InterruptedException {
        MessageBroker broker = new MessageBroker();

        Topic ordersTopic = broker.createTopic("Orders");
        Topic logsTopic = broker.createTopic("Logs");

        Subscriber s1 = new SimpleSubscriber("S1");
        Subscriber s2 = new SimpleSubscriber("S2");

        broker.subscribe(s1, ordersTopic.getTopicId());
        broker.subscribe(s2, logsTopic.getTopicId());

        Publisher p1 = new SimplePublisher("P1", broker);

        p1.publish(ordersTopic.getTopicId(), new Message("Order1", 2));
        p1.publish(logsTopic.getTopicId(), new Message("Log1", 1));
        p1.publish(ordersTopic.getTopicId(), new Message("Order2", 5));

        Thread.sleep(500); // allow async processing

        broker.printConsumptionLogs();
        broker.shutdown();
    }
}

