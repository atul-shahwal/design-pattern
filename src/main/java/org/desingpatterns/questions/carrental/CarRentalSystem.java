package org.desingpatterns.questions.carrental;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * 🎯 Problem Statement: Low-Level Design - Car Rental System
 *
 * Design a car rental system that manages vehicles, reservations, payments, and multiple rental stores.
 * The system should support different vehicle types, rental periods, payment methods, and ensure concurrency control.
 *
 * ✅ Requirements:
 * - Manage multiple rental stores with inventory of vehicles.
 * - Support various vehicle types (economy, luxury, SUV) with different pricing.
 * - Handle reservations with pick-up and return locations and dates.
 * - Process payments via multiple methods (credit card, cash, PayPal).
 * - Ensure thread-safe operations for concurrent bookings.
 *
 * 📦 Key Components:
 * - Vehicle hierarchy with specialized classes (EconomyVehicle, LuxuryVehicle, etc.).
 * - RentalStore class for each physical location.
 * - Reservation class to track booking details.
 * - PaymentStrategy interface for different payment methods.
 * - RentalSystem singleton to manage global operations.
 *
 * 🚀 Example Flow:
 * 1. User selects vehicle type and rental period → system checks availability.
 * 2. System creates a reservation and temporarily locks the vehicle.
 * 3. User pays via chosen method → payment processed → reservation confirmed.
 * 4. User picks up vehicle at scheduled time → rental period starts.
 * 5. Upon return, vehicle is marked available for future rentals.
 */
// ... existing code ...
// ENUM DEFINITIONS
enum VehicleType { ECONOMY, LUXURY, SUV, BIKE, AUTO }
enum VehicleStatus { AVAILABLE, RESERVED, RENTED, MAINTENANCE, OUT_OF_SERVICE }
enum ReservationStatus { PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELED }

// LOCATION CLASS
/**
 * Represents a physical location with address details
 * Design Pattern: Value Object (immutable data container)
 */
class Location {
    private final String address;
    private final String city;
    private final String state;
    private final String zipCode;

    public Location(String address, String city, String state, String zipCode) {
        this.address = address;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
    }

    // Getters
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getZipCode() { return zipCode; }

    @Override
    public String toString() {
        return address + ", " + city + ", " + state + " " + zipCode;
    }
}

// VEHICLE HIERARCHY (Factory Pattern)
/**
 * Abstract base class for all vehicle types
 * Design Pattern: Factory Method (abstract class with concrete implementations)
 */
abstract class Vehicle {
    private final String registrationNumber;
    private final String model;
    private final VehicleType type;
    private VehicleStatus status;
    private final double baseRentalPrice;

    public Vehicle(String registrationNumber, String model, VehicleType type, double baseRentalPrice) {
        this.registrationNumber = registrationNumber;
        this.model = model;
        this.type = type;
        this.status = VehicleStatus.AVAILABLE;
        this.baseRentalPrice = baseRentalPrice;
    }

    public abstract double calculateRentalFee(int days);

    // Getters and setters
    public String getRegistrationNumber() { return registrationNumber; }
    public String getModel() { return model; }
    public VehicleType getType() { return type; }
    public VehicleStatus getStatus() { return status; }
    public void setStatus(VehicleStatus status) { this.status = status; }
    public double getBaseRentalPrice() { return baseRentalPrice; }

    @Override
    public String toString() {
        return type + " - " + model + " (" + registrationNumber + ") - " + status;
    }
}

/**
 * Concrete vehicle class for economy cars
 */
class EconomyVehicle extends Vehicle {
    private static final double RATE_MULTIPLIER = 1.0;

    public EconomyVehicle(String registrationNumber, String model, double baseRentalPrice) {
        super(registrationNumber, model, VehicleType.ECONOMY, baseRentalPrice);
    }

    @Override
    public double calculateRentalFee(int days) {
        return getBaseRentalPrice() * days * RATE_MULTIPLIER;
    }
}

/**
 * Concrete vehicle class for luxury cars
 */
class LuxuryVehicle extends Vehicle {
    private static final double RATE_MULTIPLIER = 2.5;
    private static final double PREMIUM_FEE = 50.0;

    public LuxuryVehicle(String registrationNumber, String model, double baseRentalPrice) {
        super(registrationNumber, model, VehicleType.LUXURY, baseRentalPrice);
    }

    @Override
    public double calculateRentalFee(int days) {
        return (getBaseRentalPrice() * days * RATE_MULTIPLIER) + PREMIUM_FEE;
    }
}

/**
 * Concrete vehicle class for SUVs
 */
class SUVVehicle extends Vehicle {
    private static final double RATE_MULTIPLIER = 1.5;

    public SUVVehicle(String registrationNumber, String model, double baseRentalPrice) {
        super(registrationNumber, model, VehicleType.SUV, baseRentalPrice);
    }

    @Override
    public double calculateRentalFee(int days) {
        return getBaseRentalPrice() * days * RATE_MULTIPLIER;
    }
}

/**
 * Factory class for creating vehicles
 * Design Pattern: Factory Method (centralizes object creation)
 */
class VehicleFactory {
    public static Vehicle createVehicle(VehicleType type, String registrationNumber,
                                        String model, double baseRentalPrice) {
        switch (type) {
            case ECONOMY:
                return new EconomyVehicle(registrationNumber, model, baseRentalPrice);
            case LUXURY:
                return new LuxuryVehicle(registrationNumber, model, baseRentalPrice);
            case SUV:
                return new SUVVehicle(registrationNumber, model, baseRentalPrice);
            default:
                throw new IllegalArgumentException("Unsupported vehicle type: " + type);
        }
    }
}

// USER CLASS
/**
 * Represents a user/customer of the rental system
 */
class User {
    private final int id;
    private final String name;
    private final String email;
    private final List<Reservation> reservations;

    public User(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.reservations = new ArrayList<>();
    }

    public void addReservation(Reservation reservation) {
        reservations.add(reservation);
    }

    public void removeReservation(Reservation reservation) {
        reservations.remove(reservation);
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public List<Reservation> getReservations() { return new ArrayList<>(reservations); }

    @Override
    public String toString() {
        return name + " (" + email + ")";
    }
}

// RENTAL STORE CLASS
/**
 * Represents a physical rental location with vehicles
 */
class RentalStore {
    private final int id;
    private final String name;
    private final Location location;
    private final Map<String, Vehicle> vehicles; // Registration Number -> Vehicle

    public RentalStore(int id, String name, Location location) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.vehicles = new HashMap<>();
    }

    public List<Vehicle> getAvailableVehicles(Date startDate, Date endDate) {
        List<Vehicle> availableVehicles = new ArrayList<>();
        for (Vehicle vehicle : vehicles.values()) {
            if (vehicle.getStatus() == VehicleStatus.AVAILABLE) {
                availableVehicles.add(vehicle);
            }
        }
        return availableVehicles;
    }

    public void addVehicle(Vehicle vehicle) {
        vehicles.put(vehicle.getRegistrationNumber(), vehicle);
    }

    public void removeVehicle(String registrationNumber) {
        vehicles.remove(registrationNumber);
    }

    public boolean isVehicleAvailable(String registrationNumber, Date startDate, Date endDate) {
        Vehicle vehicle = vehicles.get(registrationNumber);
        return vehicle != null && vehicle.getStatus() == VehicleStatus.AVAILABLE;
    }

    public Vehicle getVehicle(String registrationNumber) {
        return vehicles.get(registrationNumber);
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public Location getLocation() { return location; }
    public Map<String, Vehicle> getVehicles() { return new HashMap<>(vehicles); }

    @Override
    public String toString() {
        return name + " - " + location;
    }
}

// RESERVATION CLASS
/**
 * Represents a vehicle reservation with rental period details
 */
class Reservation {
    private final int id;
    private final User user;
    private final Vehicle vehicle;
    private final RentalStore pickupStore;
    private final RentalStore returnStore;
    private final Date startDate;
    private final Date endDate;
    private ReservationStatus status;
    private final double totalAmount;

    public Reservation(int id, User user, Vehicle vehicle, RentalStore pickupStore,
                       RentalStore returnStore, Date startDate, Date endDate) {
        this.id = id;
        this.user = user;
        this.vehicle = vehicle;
        this.pickupStore = pickupStore;
        this.returnStore = returnStore;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = ReservationStatus.PENDING;

        // Calculate days between start and end dates
        long diffInMillies = endDate.getTime() - startDate.getTime();
        int days = (int) (diffInMillies / (1000 * 60 * 60 * 24)) + 1;
        this.totalAmount = vehicle.calculateRentalFee(days);
    }

    public void confirmReservation() {
        if (status == ReservationStatus.PENDING) {
            status = ReservationStatus.CONFIRMED;
            vehicle.setStatus(VehicleStatus.RESERVED);
        }
    }

    public void startRental() {
        if (status == ReservationStatus.CONFIRMED) {
            status = ReservationStatus.IN_PROGRESS;
            vehicle.setStatus(VehicleStatus.RENTED);
        }
    }

    public void completeRental() {
        if (status == ReservationStatus.IN_PROGRESS) {
            status = ReservationStatus.COMPLETED;
            vehicle.setStatus(VehicleStatus.AVAILABLE);
        }
    }

    public void cancelReservation() {
        if (status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED) {
            status = ReservationStatus.CANCELED;
            vehicle.setStatus(VehicleStatus.AVAILABLE);
        }
    }

    // Getters
    public int getId() { return id; }
    public User getUser() { return user; }
    public Vehicle getVehicle() { return vehicle; }
    public RentalStore getPickupStore() { return pickupStore; }
    public RentalStore getReturnStore() { return returnStore; }
    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }
    public ReservationStatus getStatus() { return status; }
    public double getTotalAmount() { return totalAmount; }

    @Override
    public String toString() {
        return "Reservation #" + id + " - " + vehicle.getModel() + " (" + status + ")";
    }
}

// RESERVATION MANAGER CLASS
/**
 * Manages all reservations in the system
 * Design Pattern: Manager (centralizes reservation operations)
 */
class ReservationManager {
    private final Map<Integer, Reservation> reservations;
    private int nextReservationId;

    public ReservationManager() {
        this.reservations = new HashMap<>();
        this.nextReservationId = 1;
    }

    public Reservation createReservation(User user, Vehicle vehicle, RentalStore pickupStore,
                                         RentalStore returnStore, Date startDate, Date endDate) {
        Reservation reservation = new Reservation(nextReservationId++, user, vehicle,
                pickupStore, returnStore, startDate, endDate);
        reservations.put(reservation.getId(), reservation);
        user.addReservation(reservation);
        return reservation;
    }

    public void confirmReservation(int reservationId) {
        Reservation reservation = reservations.get(reservationId);
        if (reservation != null) {
            reservation.confirmReservation();
        }
    }

    public void startRental(int reservationId) {
        Reservation reservation = reservations.get(reservationId);
        if (reservation != null) {
            reservation.startRental();
        }
    }

    public void completeRental(int reservationId) {
        Reservation reservation = reservations.get(reservationId);
        if (reservation != null) {
            reservation.completeRental();
        }
    }

    public void cancelReservation(int reservationId) {
        Reservation reservation = reservations.get(reservationId);
        if (reservation != null) {
            reservation.cancelReservation();
            reservation.getUser().removeReservation(reservation);
        }
    }

    public Reservation getReservation(int reservationId) {
        return reservations.get(reservationId);
    }

    public List<Reservation> getAllReservations() {
        return new ArrayList<>(reservations.values());
    }
}

// PAYMENT STRATEGY (Strategy Pattern)
/**
 * Interface for payment strategies
 * Design Pattern: Strategy (interchangeable algorithms)
 */
interface PaymentStrategy {
    boolean processPayment(double amount);
}

/**
 * Concrete payment strategy for credit cards
 */
class CreditCardPayment implements PaymentStrategy {
    private final String cardNumber;
    private final String name;
    private final String expiryDate;

    public CreditCardPayment(String cardNumber, String name, String expiryDate) {
        this.cardNumber = cardNumber;
        this.name = name;
        this.expiryDate = expiryDate;
    }

    @Override
    public boolean processPayment(double amount) {
        System.out.println("Processing credit card payment of $" + amount);
        System.out.println("Card details: " + maskCardNumber(cardNumber) + ", " + name + ", expires: " + expiryDate);
        // Simulate payment processing
        return true;
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber.length() <= 4) return cardNumber;
        return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
    }
}

/**
 * Concrete payment strategy for cash
 */
class CashPayment implements PaymentStrategy {
    @Override
    public boolean processPayment(double amount) {
        System.out.println("Processing cash payment of $" + amount);
        // Simulate payment processing
        return true;
    }
}

/**
 * Concrete payment strategy for PayPal
 */
class PayPalPayment implements PaymentStrategy {
    private final String email;

    public PayPalPayment(String email) {
        this.email = email;
    }

    @Override
    public boolean processPayment(double amount) {
        System.out.println("Processing PayPal payment of $" + amount);
        System.out.println("PayPal Account: " + email);
        // Simulate payment processing
        return true;
    }
}

/**
 * Payment processor that uses different payment strategies
 * Design Pattern: Strategy (context class)
 */
class PaymentProcessor {
    public boolean processPayment(double amount, PaymentStrategy paymentStrategy) {
        return paymentStrategy.processPayment(amount);
    }
}

// RENTAL SYSTEM (Singleton Pattern)
/**
 * Main system class that coordinates all operations
 * Design Pattern: Singleton (only one instance)
 */
class RentalSystem {
    private static RentalSystem instance;
    private final List<RentalStore> stores;
    private final VehicleFactory vehicleFactory;
    private final ReservationManager reservationManager;
    private final PaymentProcessor paymentProcessor;
    private final Map<Integer, User> users;
    private int nextUserId;

    private RentalSystem() {
        this.stores = new ArrayList<>();
        this.vehicleFactory = new VehicleFactory();
        this.reservationManager = new ReservationManager();
        this.paymentProcessor = new PaymentProcessor();
        this.users = new HashMap<>();
        this.nextUserId = 1;
    }

    public static synchronized RentalSystem getInstance() {
        if (instance == null) {
            instance = new RentalSystem();
        }
        return instance;
    }

    public void addStore(RentalStore store) {
        stores.add(store);
    }

    public RentalStore getStore(int storeId) {
        for (RentalStore store : stores) {
            if (store.getId() == storeId) {
                return store;
            }
        }
        return null;
    }

    public List<RentalStore> getStores() {
        return new ArrayList<>(stores);
    }

    public User registerUser(String name, String email) {
        User user = new User(nextUserId++, name, email);
        users.put(user.getId(), user);
        return user;
    }

    public User getUser(int userId) {
        return users.get(userId);
    }

    public Reservation createReservation(int userId, String vehicleRegistration,
                                         int pickupStoreId, int returnStoreId,
                                         Date startDate, Date endDate) {
        User user = users.get(userId);
        RentalStore pickupStore = getStore(pickupStoreId);
        RentalStore returnStore = getStore(returnStoreId);
        Vehicle vehicle = (pickupStore != null) ? pickupStore.getVehicle(vehicleRegistration) : null;

        if (user != null && pickupStore != null && returnStore != null && vehicle != null) {
            return reservationManager.createReservation(user, vehicle, pickupStore, returnStore, startDate, endDate);
        }
        return null;
    }

    public boolean processPayment(int reservationId, PaymentStrategy paymentStrategy) {
        Reservation reservation = reservationManager.getReservation(reservationId);
        if (reservation != null) {
            boolean result = paymentProcessor.processPayment(reservation.getTotalAmount(), paymentStrategy);
            if (result) {
                reservationManager.confirmReservation(reservationId);
                return true;
            }
        }
        return false;
    }

    public void startRental(int reservationId) {
        reservationManager.startRental(reservationId);
    }

    public void completeRental(int reservationId) {
        reservationManager.completeRental(reservationId);
    }

    public void cancelReservation(int reservationId) {
        reservationManager.cancelReservation(reservationId);
    }

    public List<Reservation> getAllReservations() {
        return reservationManager.getAllReservations();
    }
}

// MAIN CLASS
/**
 * Demo class to test the car rental system
 */
public class CarRentalSystem {
    public static void main(String[] args) throws Exception {
        // Get the Rental System instance (Singleton)
        RentalSystem rentalSystem = RentalSystem.getInstance();

        // Create rental stores
        RentalStore store1 = new RentalStore(1, "Downtown Rentals",
                new Location("123 Main St", "New York", "NY", "10001"));
        RentalStore store2 = new RentalStore(2, "Airport Rentals",
                new Location("456 Airport Rd", "Los Angeles", "CA", "90045"));
        rentalSystem.addStore(store1);
        rentalSystem.addStore(store2);

        // Create vehicles using Factory Pattern
        Vehicle economyCar = VehicleFactory.createVehicle(
                VehicleType.ECONOMY, "EC001", "Toyota Corolla", 50.0);
        Vehicle luxuryCar = VehicleFactory.createVehicle(
                VehicleType.LUXURY, "LX001", "Mercedes S-Class", 200.0);
        Vehicle suvCar = VehicleFactory.createVehicle(
                VehicleType.SUV, "SV001", "Honda CR-V", 75.0);

        // Add vehicles to stores
        store1.addVehicle(economyCar);
        store1.addVehicle(luxuryCar);
        store2.addVehicle(suvCar);

        // Register users
        User user1 = rentalSystem.registerUser("John Doe", "john.doe@example.com");
        User user2 = rentalSystem.registerUser("Jane Smith", "jane.smith@example.com");

        // Display available vehicles
        System.out.println("Available vehicles at Downtown Rentals:");
        for (Vehicle vehicle : store1.getAvailableVehicles(new Date(), new Date())) {
            System.out.println("  " + vehicle);
        }

        // Create a reservation
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date startDate = sdf.parse("2025-05-01");
        Date endDate = sdf.parse("2025-05-05");

        Reservation reservation = rentalSystem.createReservation(
                user1.getId(), economyCar.getRegistrationNumber(),
                store1.getId(), store1.getId(), startDate, endDate);

        if (reservation != null) {
            System.out.println("\nCreated reservation: " + reservation);
            System.out.println("Total amount: $" + reservation.getTotalAmount());

            // Process payment
            PaymentStrategy paymentStrategy = new CreditCardPayment("1234567812345678", "John Doe", "12/2027");
            boolean paymentSuccess = rentalSystem.processPayment(reservation.getId(), paymentStrategy);

            if (paymentSuccess) {
                System.out.println("Payment successful! Reservation confirmed.");

                // Start the rental
                rentalSystem.startRental(reservation.getId());
                System.out.println("Rental started: " + reservation);

                // Complete the rental
                rentalSystem.completeRental(reservation.getId());
                System.out.println("Rental completed: " + reservation);
            } else {
                System.out.println("Payment failed!");
            }
        }

        // Display all reservations
        System.out.println("\nAll reservations in the system:");
        for (Reservation res : rentalSystem.getAllReservations()) {
            System.out.println("  " + res);
        }
    }
}
