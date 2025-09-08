package org.desingpatterns.questions.inventorymanagement;

import java.util.*;

/**
 * 🎯 Problem Statement: Low-Level Design - Inventory Management System
 *
 * Design an inventory management system that tracks products, manages stock levels, and notifies when stock is low.
 * The system should support different product categories, warehouses, and replenishment strategies.
 *
 * ✅ Requirements:
 * - Manage products with SKUs, categories, prices, and stock levels.
 * - Support multiple warehouses for inventory storage.
 * - Notify observers (e.g., suppliers) when stock levels fall below threshold.
 * - Use replenishment strategies (just-in-time, bulk order) for restocking.
 * - Use builder pattern for flexible product creation.
 *
 * 📦 Key Components:
 * - Product abstract class and concrete subclasses (Electronics, Clothing, etc.).
 * - Warehouse class for inventory storage per location.
 * - InventoryObserver interface and concrete observers (SupplierNotifier, etc.).
 * - ReplenishmentStrategy interface and implementations.
 * - InventoryManager singleton to coordinate operations.
 *
 * 🚀 Example Flow:
 * 1. Product is added to warehouse with initial stock → stock level recorded.
 * 2. Stock decreases due to sales → system checks against threshold.
 * 3. If stock low, notifies observers → triggers replenishment strategy.
 * 4. New stock arrives → inventory updated → notifications cleared.
 */

// Product Category Enum
enum ProductCategory {
    ELECTRONICS, CLOTHING, GROCERY, FURNITURE, OTHER
}

// Inventory Operation Enum
enum InventoryOperation {
    ADD, REMOVE, TRANSFER, ADJUST
}

// Observer Pattern
interface InventoryObserver {
    void update(Product product);
}

// Strategy Pattern
interface ReplenishmentStrategy {
    void replenish(Product product);
}

// Concrete Strategies
class JustInTimeStrategy implements ReplenishmentStrategy {
    @Override
    public void replenish(Product product) {
        System.out.println("Applying Just-In-Time replenishment for " + product.getName());
    }
}

class BulkOrderStrategy implements ReplenishmentStrategy {
    @Override
    public void replenish(Product product) {
        System.out.println("Applying Bulk Order replenishment for " + product.getName());
    }
}

// Base Product Class with Builder Pattern
abstract class Product {
    private String sku;
    private String name;
    private double price;
    private int quantity;
    private int threshold;
    private ProductCategory category;

    protected Product(Builder<?> builder) {
        this.sku = builder.sku;
        this.name = builder.name;
        this.price = builder.price;
        this.quantity = builder.quantity;
        this.threshold = builder.threshold;
        this.category = builder.category;
    }

    // Getters and Setters
    public String getSku() { return sku; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public int getThreshold() { return threshold; }
    public ProductCategory getCategory() { return category; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setThreshold(int threshold) { this.threshold = threshold; }

    // Builder Pattern
    public static abstract class Builder<T extends Builder<T>> {
        private String sku;
        private String name;
        private double price;
        private int quantity = 0;
        private int threshold = 10;
        private ProductCategory category;

        public T sku(String sku) { this.sku = sku; return self(); }
        public T name(String name) { this.name = name; return self(); }
        public T price(double price) { this.price = price; return self(); }
        public T quantity(int quantity) { this.quantity = quantity; return self(); }
        public T threshold(int threshold) { this.threshold = threshold; return self(); }
        public T category(ProductCategory category) { this.category = category; return self(); }

        protected abstract T self();
        public abstract Product build();
    }

    public void addStock(int quantity) {
        this.quantity += quantity;
    }

    public void removeStock(int quantity) {
        this.quantity = Math.max(0, this.quantity - quantity);
    }
}

// Concrete Products with Builders
class ElectronicsProduct extends Product {
    private String brand;
    private int warrantyPeriod;

    private ElectronicsProduct(ElectronicsBuilder builder) {
        super(builder);
        this.brand = builder.brand;
        this.warrantyPeriod = builder.warrantyPeriod;
    }

    public static class ElectronicsBuilder extends Builder<ElectronicsBuilder> {
        private String brand;
        private int warrantyPeriod = 12;

        public ElectronicsBuilder brand(String brand) { this.brand = brand; return this; }
        public ElectronicsBuilder warrantyPeriod(int warrantyPeriod) { this.warrantyPeriod = warrantyPeriod; return this; }

        @Override protected ElectronicsBuilder self() { return this; }
        @Override public ElectronicsProduct build() { return new ElectronicsProduct(this); }
    }
}

class ClothingProduct extends Product {
    private String size;
    private String color;

    private ClothingProduct(ClothingBuilder builder) {
        super(builder);
        this.size = builder.size;
        this.color = builder.color;
    }

    public static class ClothingBuilder extends Builder<ClothingBuilder> {
        private String size;
        private String color;

        public ClothingBuilder size(String size) { this.size = size; return this; }
        public ClothingBuilder color(String color) { this.color = color; return this; }

        @Override protected ClothingBuilder self() { return this; }
        @Override public ClothingProduct build() { return new ClothingProduct(this); }
    }
}

// Factory Pattern
class ProductFactory {
    public Product createProduct(ProductCategory category, String sku, String name,
                                 double price, int quantity, int threshold) {
        switch (category) {
            case ELECTRONICS:
                return new ElectronicsProduct.ElectronicsBuilder()
                        .sku(sku).name(name).price(price).quantity(quantity).threshold(threshold)
                        .category(ProductCategory.ELECTRONICS).build();
            case CLOTHING:
                return new ClothingProduct.ClothingBuilder()
                        .sku(sku).name(name).price(price).quantity(quantity).threshold(threshold)
                        .category(ProductCategory.CLOTHING).build();
            default:
                throw new IllegalArgumentException("Unsupported category");
        }
    }
}

// Warehouse Class
class Warehouse {
    private String id;
    private String name;
    private Map<String, Product> products = new HashMap<>();

    public Warehouse(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addProduct(Product product, int quantity) {
        product.addStock(quantity);
        products.put(product.getSku(), product);
    }

    public boolean removeProduct(String sku, int quantity) {
        Product product = products.get(sku);
        if (product != null && product.getQuantity() >= quantity) {
            product.removeStock(quantity);
            return true;
        }
        return false;
    }

    public Product getProduct(String sku) {
        return products.get(sku);
    }
}

// Singleton Inventory Manager with Observer Pattern
class InventoryManager {
    private static InventoryManager instance;
    private List<Warehouse> warehouses = new ArrayList<>();
    private List<InventoryObserver> observers = new ArrayList<>();
    private ReplenishmentStrategy strategy;

    private InventoryManager() {}

    public static synchronized InventoryManager getInstance() {
        if (instance == null) instance = new InventoryManager();
        return instance;
    }

    public void addObserver(InventoryObserver observer) {
        observers.add(observer);
    }

    public void setReplenishmentStrategy(ReplenishmentStrategy strategy) {
        this.strategy = strategy;
    }

    public void checkStockLevels() {
        for (Warehouse warehouse : warehouses) {
            // Implementation would check all products in warehouse
        }
    }

    public void notifyObservers(Product product) {
        for (InventoryObserver observer : observers) {
            observer.update(product);
        }
    }
}

// Concrete Observers
class SupplierNotifier implements InventoryObserver {
    @Override
    public void update(Product product) {
        System.out.println("Notifying supplier about: " + product.getSku());
    }
}

class DashboardAlert implements InventoryObserver {
    @Override
    public void update(Product product) {
        System.out.println("Dashboard alert for: " + product.getSku());
    }
}

// Main Class
public class InventoryManagementSystem {
    public static void main(String[] args) {
        InventoryManager manager = InventoryManager.getInstance();

        // Add observers
        manager.addObserver(new SupplierNotifier());
        manager.addObserver(new DashboardAlert());

        // Set strategy
        manager.setReplenishmentStrategy(new JustInTimeStrategy());

        // Create products
        ProductFactory factory = new ProductFactory();
        Product laptop = factory.createProduct(
                ProductCategory.ELECTRONICS, "SKU001", "Laptop", 999.99, 10, 5);

        // Add to warehouse
        Warehouse wh = new Warehouse("WH1", "Main Warehouse");
        wh.addProduct(laptop, 10);

        // Check stock levels
        manager.checkStockLevels();
    }
}