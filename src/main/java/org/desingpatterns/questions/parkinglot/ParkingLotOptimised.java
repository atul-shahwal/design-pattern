package org.desingpatterns.questions.parkinglot;

import java.time.LocalDateTime;
import java.util.*;
/**
 * 🎯 Problem Statement: Low-Level Design - Parking Lot System
 *
 * Design a parking lot system that manages multiple buildings, floors, and parking slots for different vehicle types.
 * The system should handle vehicle parking, fee calculation, payment processing, and provide real-time availability.
 *
 * ✅ Requirements:
 * - Support different vehicle types (bike, car, truck) with dedicated slots.
 * - Manage parking slots with availability, reservation, and electric charging options.
 * - Calculate parking fees based on vehicle type and duration.
 * - Process payments via multiple methods (cash, card, etc.).
 * - Provide real-time status of available slots per building/floor.
 *
 * 📦 Key Components:
 * - Vehicle abstract class and concrete subclasses (Bike, Car, Truck).
 * - ParkingSlot class for individual slots with attributes.
 * - Floor and Building classes to organize slots hierarchically.
 * - ParkingTicket class to track parking sessions.
 * - ParkingLot class to manage operations and availability.
 *
 * 🚀 Example Flow:
 * 1. Vehicle arrives → system finds available slot based on type.
 * 2. Ticket issued upon entry → slot marked occupied.
 * 3. Upon exit, fee calculated based on duration → payment processed.
 * 4. After payment, slot freed → available for next vehicle.
 */
// Enums
enum VehicleType {
    BIKE, CAR, TRUCK
}

enum SlotStatus {
    AVAILABLE, OCCUPIED, MAINTENANCE
}

enum PaymentStatus {
    PENDING, COMPLETED, FAILED, REFUNDED
}

// Payment Strategy
interface PaymentStrategy {
    boolean processPayment(double amount);
    boolean refundPayment(double amount);
}

class CashPayment implements PaymentStrategy {
    @Override
    public boolean processPayment(double amount) {
        System.out.println("Processing cash payment of $" + amount);
        return true;
    }

    @Override
    public boolean refundPayment(double amount) {
        System.out.println("Refunding cash payment of $" + amount);
        return true;
    }
}

// Vehicle classes
abstract class Vehicle {
    protected String licensePlate;
    protected VehicleType type;
    protected boolean needsCharging;

    public Vehicle(String licensePlate, VehicleType type, boolean needsCharging) {
        this.licensePlate = licensePlate;
        this.type = type;
        this.needsCharging = needsCharging;
    }

    public VehicleType getType() {
        return type;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public boolean needsCharging() {
        return needsCharging;
    }

    public abstract double calculateParkingFee(long duration);
}

class Bike extends Vehicle {
    public Bike(String licensePlate, boolean needsCharging) {
        super(licensePlate, VehicleType.BIKE, needsCharging);
    }

    @Override
    public double calculateParkingFee(long duration) {
        // Bike parking fee calculation
        return duration * 0.5; // $0.5 per hour
    }
}

class Car extends Vehicle {
    public Car(String licensePlate, boolean needsCharging) {
        super(licensePlate, VehicleType.CAR, needsCharging);
    }

    @Override
    public double calculateParkingFee(long duration) {
        // Car parking fee calculation
        return duration * 2.0; // $2.0 per hour
    }
}

class Truck extends Vehicle {
    public Truck(String licensePlate, boolean needsCharging) {
        super(licensePlate, VehicleType.TRUCK, needsCharging);
    }

    @Override
    public double calculateParkingFee(long duration) {
        // Truck parking fee calculation
        return duration * 4.0; // $4.0 per hour
    }
}

// Parking Slot
class ParkingSlot {
    private int slotId;
    private VehicleType type;
    private SlotStatus status;
    private Vehicle vehicle;
    private boolean isReserved;
    private boolean isElectricChargingAvailable;

    public ParkingSlot(int slotId, VehicleType type, boolean isElectricChargingAvailable) {
        this.slotId = slotId;
        this.type = type;
        this.status = SlotStatus.AVAILABLE;
        this.isElectricChargingAvailable = isElectricChargingAvailable;
        this.isReserved = false;
    }

    public boolean parkVehicle(Vehicle vehicle) {
        if (status != SlotStatus.AVAILABLE || isReserved || vehicle.getType() != type) {
            return false;
        }

        this.vehicle = vehicle;
        this.status = SlotStatus.OCCUPIED;
        return true;
    }

    public void freeSlot() {
        this.vehicle = null;
        this.status = SlotStatus.AVAILABLE;
        this.isReserved = false;
    }

    public boolean isAvailable() {
        return status == SlotStatus.AVAILABLE && !isReserved;
    }

    public void reserveSlot() {
        this.isReserved = true;
    }

    public void releaseReservation() {
        this.isReserved = false;
    }

    // Getters and setters
    public int getSlotId() { return slotId; }
    public VehicleType getType() { return type; }
    public SlotStatus getStatus() { return status; }
    public Vehicle getVehicle() { return vehicle; }
    public boolean isReserved() { return isReserved; }
    public boolean isElectricChargingAvailable() { return isElectricChargingAvailable; }
}

// Floor
class Floor {
    private int floorNumber;
    private String name;
    private Map<Integer, ParkingSlot> slots;
    private Map<VehicleType, Integer> capacityByType;
    private Map<VehicleType, Integer> availableSlotsByType;

    public Floor(int floorNumber, String name) {
        this.floorNumber = floorNumber;
        this.name = name;
        this.slots = new HashMap<>();
        this.capacityByType = new HashMap<>();
        this.availableSlotsByType = new HashMap<>();

        // Initialize counts
        for (VehicleType type : VehicleType.values()) {
            capacityByType.put(type, 0);
            availableSlotsByType.put(type, 0);
        }
    }

    public boolean addSlot(ParkingSlot slot) {
        if (slots.containsKey(slot.getSlotId())) {
            return false;
        }

        slots.put(slot.getSlotId(), slot);

        // Update capacity and available counts
        VehicleType type = slot.getType();
        capacityByType.put(type, capacityByType.get(type) + 1);
        availableSlotsByType.put(type, availableSlotsByType.get(type) + 1);

        return true;
    }

    public boolean removeSlot(int slotId) {
        if (!slots.containsKey(slotId)) {
            return false;
        }

        ParkingSlot slot = slots.get(slotId);
        VehicleType type = slot.getType();

        slots.remove(slotId);

        // Update capacity and available counts
        capacityByType.put(type, capacityByType.get(type) - 1);
        if (slot.isAvailable()) {
            availableSlotsByType.put(type, availableSlotsByType.get(type) - 1);
        }

        return true;
    }

    public ParkingSlot findAvailableSlot(VehicleType vehicleType) {
        for (ParkingSlot slot : slots.values()) {
            if (slot.isAvailable() && slot.getType() == vehicleType) {
                return slot;
            }
        }
        return null;
    }

    public int getAvailableSlotsCount(VehicleType vehicleType) {
        return availableSlotsByType.getOrDefault(vehicleType, 0);
    }

    public void updateAvailableSlots(VehicleType vehicleType, int delta) {
        int current = availableSlotsByType.getOrDefault(vehicleType, 0);
        availableSlotsByType.put(vehicleType, current + delta);
    }

    // Getters
    public int getFloorNumber() { return floorNumber; }
    public String getName() { return name; }
    public Map<Integer, ParkingSlot> getSlots() { return slots; }
}

// Building
class Building {
    private String buildingId;
    private String name;
    private String address;
    private Map<Integer, Floor> floors;
    private Map<VehicleType, Integer> totalCapacityByType;
    private Map<VehicleType, Integer> availableSlotsByType;

    public Building(String buildingId, String name, String address) {
        this.buildingId = buildingId;
        this.name = name;
        this.address = address;
        this.floors = new HashMap<>();
        this.totalCapacityByType = new HashMap<>();
        this.availableSlotsByType = new HashMap<>();

        // Initialize counts
        for (VehicleType type : VehicleType.values()) {
            totalCapacityByType.put(type, 0);
            availableSlotsByType.put(type, 0);
        }
    }

    public boolean addFloor(Floor floor) {
        if (floors.containsKey(floor.getFloorNumber())) {
            return false;
        }

        floors.put(floor.getFloorNumber(), floor);

        // Update capacity and available counts
        for (VehicleType type : VehicleType.values()) {
            int floorCapacity = floor.getAvailableSlotsCount(type);
            totalCapacityByType.put(type, totalCapacityByType.get(type) + floorCapacity);
            availableSlotsByType.put(type, availableSlotsByType.get(type) + floorCapacity);
        }

        return true;
    }

    public boolean removeFloor(int floorNumber) {
        if (!floors.containsKey(floorNumber)) {
            return false;
        }

        Floor floor = floors.get(floorNumber);
        floors.remove(floorNumber);

        // Update capacity and available counts
        for (VehicleType type : VehicleType.values()) {
            int floorCapacity = floor.getAvailableSlotsCount(type);
            totalCapacityByType.put(type, totalCapacityByType.get(type) - floorCapacity);
            availableSlotsByType.put(type, availableSlotsByType.get(type) - floorCapacity);
        }

        return true;
    }

    public ParkingSlot findAvailableSlot(VehicleType vehicleType) {
        for (Floor floor : floors.values()) {
            ParkingSlot slot = floor.findAvailableSlot(vehicleType);
            if (slot != null) {
                return slot;
            }
        }
        return null;
    }

    public int getAvailableSlotsCount(VehicleType vehicleType) {
        return availableSlotsByType.getOrDefault(vehicleType, 0);
    }

    public void updateAvailableSlots(VehicleType vehicleType, int delta) {
        int current = availableSlotsByType.getOrDefault(vehicleType, 0);
        availableSlotsByType.put(vehicleType, current + delta);
    }

    // Getters
    public String getBuildingId() { return buildingId; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public Map<Integer, Floor> getFloors() { return floors; }
}

// Parking Ticket
class ParkingTicket {
    private int ticketId;
    private Vehicle vehicle;
    private ParkingSlot slot;
    private Building building;
    private Floor floor;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private PaymentStatus paymentStatus;
    private double fee;

    public ParkingTicket(int ticketId, Vehicle vehicle, ParkingSlot slot, Building building, Floor floor) {
        this.ticketId = ticketId;
        this.vehicle = vehicle;
        this.slot = slot;
        this.building = building;
        this.floor = floor;
        this.entryTime = LocalDateTime.now();
        this.paymentStatus = PaymentStatus.PENDING;
    }

    public double calculateFee() {
        if (exitTime == null) {
            exitTime = LocalDateTime.now();
        }

        long duration = java.time.Duration.between(entryTime, exitTime).toHours();
        fee = vehicle.calculateParkingFee(duration);
        return fee;
    }

    public void updateExitTime() {
        this.exitTime = LocalDateTime.now();
    }

    public void markAsPaid() {
        this.paymentStatus = PaymentStatus.COMPLETED;
    }

    // Getters and setters
    public int getTicketId() { return ticketId; }
    public Vehicle getVehicle() { return vehicle; }
    public ParkingSlot getSlot() { return slot; }
    public Building getBuilding() { return building; }
    public Floor getFloor() { return floor; }
    public LocalDateTime getEntryTime() { return entryTime; }
    public LocalDateTime getExitTime() { return exitTime; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public double getFee() { return fee; }
}

// Parking Lot
class ParkingLot {
    private Map<String, Building> buildings;
    private Map<Integer, ParkingTicket> activeTickets;
    private int nextTicketId;

    public ParkingLot() {
        this.buildings = new HashMap<>();
        this.activeTickets = new HashMap<>();
        this.nextTicketId = 1;
    }

    public boolean addBuilding(Building building) {
        if (buildings.containsKey(building.getBuildingId())) {
            return false;
        }

        buildings.put(building.getBuildingId(), building);
        return true;
    }

    public boolean removeBuilding(String buildingId) {
        if (!buildings.containsKey(buildingId)) {
            return false;
        }

        buildings.remove(buildingId);
        return true;
    }

    public ParkingSlot findAvailableSlot(VehicleType vehicleType, String preferredBuilding) {
        if (preferredBuilding != null && buildings.containsKey(preferredBuilding)) {
            ParkingSlot slot = buildings.get(preferredBuilding).findAvailableSlot(vehicleType);
            if (slot != null) {
                return slot;
            }
        }

        // Search all buildings if preferred building is not specified or has no available slots
        for (Building building : buildings.values()) {
            ParkingSlot slot = building.findAvailableSlot(vehicleType);
            if (slot != null) {
                return slot;
            }
        }

        return null;
    }

    public ParkingTicket parkVehicle(Vehicle vehicle, String preferredBuilding) {
        ParkingSlot slot = findAvailableSlot(vehicle.getType(), preferredBuilding);
        if (slot == null) {
            return null;
        }

        // Find the building and floor for this slot
        Building building = null;
        Floor floor = null;

        for (Building b : buildings.values()) {
            for (Floor f : b.getFloors().values()) {
                if (f.getSlots().containsValue(slot)) {
                    building = b;
                    floor = f;
                    break;
                }
            }
            if (building != null) break;
        }

        if (building == null) {
            return null;
        }

        // Park the vehicle
        if (!slot.parkVehicle(vehicle)) {
            return null;
        }

        // Update available slots count
        building.updateAvailableSlots(vehicle.getType(), -1);
        floor.updateAvailableSlots(vehicle.getType(), -1);

        // Create ticket
        ParkingTicket ticket = new ParkingTicket(nextTicketId++, vehicle, slot, building, floor);
        activeTickets.put(ticket.getTicketId(), ticket);

        return ticket;
    }

    public boolean processExit(int ticketId, PaymentStrategy paymentStrategy) {
        if (!activeTickets.containsKey(ticketId)) {
            return false;
        }

        ParkingTicket ticket = activeTickets.get(ticketId);

        // Calculate fee
        double fee = ticket.calculateFee();

        // Process payment
        if (!paymentStrategy.processPayment(fee)) {
            return false;
        }

        // Mark ticket as paid
        ticket.markAsPaid();

        // Free the slot
        ParkingSlot slot = ticket.getSlot();
        slot.freeSlot();

        // Update available slots count
        Building building = ticket.getBuilding();
        Floor floor = ticket.getFloor();
        building.updateAvailableSlots(ticket.getVehicle().getType(), 1);
        floor.updateAvailableSlots(ticket.getVehicle().getType(), 1);

        // Remove from active tickets
        activeTickets.remove(ticketId);

        return true;
    }

    public int getAvailableSlotsCount(VehicleType vehicleType, String buildingId) {
        if (buildingId != null && buildings.containsKey(buildingId)) {
            return buildings.get(buildingId).getAvailableSlotsCount(vehicleType);
        }

        // Sum across all buildings
        int count = 0;
        for (Building building : buildings.values()) {
            count += building.getAvailableSlotsCount(vehicleType);
        }

        return count;
    }

    public Map<VehicleType, Integer> getBuildingStatus(String buildingId) {
        if (!buildings.containsKey(buildingId)) {
            return null;
        }

        Building building = buildings.get(buildingId);
        Map<VehicleType, Integer> status = new HashMap<>();

        for (VehicleType type : VehicleType.values()) {
            status.put(type, building.getAvailableSlotsCount(type));
        }

        return status;
    }
}

// Example usage
public class ParkingLotOptimised {
    public static void main(String[] args) throws InterruptedException {
        // Create parking lot
        ParkingLot parkingLot = new ParkingLot();

        // Create multiple buildings
        Building building1 = new Building("B1", "Main Building", "123 Main St");
        Building building2 = new Building("B2", "Annex Building", "456 Side St");

        // Create floors for building 1
        Floor floor1 = new Floor(1, "Ground Floor");
        Floor floor2 = new Floor(2, "First Floor");
        Floor floor3 = new Floor(3, "Second Floor");

        // Create floors for building 2
        Floor floor4 = new Floor(1, "Ground Floor");
        Floor floor5 = new Floor(2, "First Floor");

        // Add slots to floors in building 1
        for (int i = 1; i <= 5; i++) {
            floor1.addSlot(new ParkingSlot(i, VehicleType.CAR, i % 2 == 0));
        }

        for (int i = 6; i <= 10; i++) {
            floor1.addSlot(new ParkingSlot(i, VehicleType.BIKE, false));
        }

        for (int i = 11; i <= 15; i++) {
            floor2.addSlot(new ParkingSlot(i, VehicleType.CAR, i % 2 == 0));
        }

        for (int i = 16; i <= 20; i++) {
            floor2.addSlot(new ParkingSlot(i, VehicleType.TRUCK, false));
        }

        for (int i = 21; i <= 25; i++) {
            floor3.addSlot(new ParkingSlot(i, VehicleType.CAR, true));
        }

        // Add slots to floors in building 2
        for (int i = 26; i <= 35; i++) {
            floor4.addSlot(new ParkingSlot(i, VehicleType.CAR, i % 3 == 0));
        }

        for (int i = 36; i <= 40; i++) {
            floor5.addSlot(new ParkingSlot(i, VehicleType.BIKE, false));
        }

        // Add floors to buildings
        building1.addFloor(floor1);
        building1.addFloor(floor2);
        building1.addFloor(floor3);

        building2.addFloor(floor4);
        building2.addFloor(floor5);

        // Add buildings to parking lot
        parkingLot.addBuilding(building1);
        parkingLot.addBuilding(building2);

        // Display initial available slots
        System.out.println("=== INITIAL PARKING LOT STATUS ===");
        displayParkingLotStatus(parkingLot);

        // Create vehicles
        Vehicle car1 = new Car("ABC123", false);
        Vehicle car2 = new Car("DEF456", true); // Needs charging
        Vehicle bike1 = new Bike("XYZ789", false);
        Vehicle bike2 = new Bike("MNO101", false);
        Vehicle truck1 = new Truck("TRK001", false);
        Vehicle truck2 = new Truck("TRK002", false);

        // Park vehicles
        System.out.println("\n=== PARKING VEHICLES ===");
        ParkingTicket car1Ticket = parkingLot.parkVehicle(car1, "B1");
        ParkingTicket car2Ticket = parkingLot.parkVehicle(car2, "B2");
        ParkingTicket bike1Ticket = parkingLot.parkVehicle(bike1, "B1");
        ParkingTicket bike2Ticket = parkingLot.parkVehicle(bike2, "B1");
        ParkingTicket truck1Ticket = parkingLot.parkVehicle(truck1, "B1");
        ParkingTicket truck2Ticket = parkingLot.parkVehicle(truck2, "B1"); // This might fail if no truck slots

        // Display tickets
        if (car1Ticket != null) {
            System.out.println("Car 1 parked successfully. Ticket ID: " + car1Ticket.getTicketId());
        }

        if (car2Ticket != null) {
            System.out.println("Car 2 parked successfully. Ticket ID: " + car2Ticket.getTicketId());
        }

        if (bike1Ticket != null) {
            System.out.println("Bike 1 parked successfully. Ticket ID: " + bike1Ticket.getTicketId());
        }

        if (bike2Ticket != null) {
            System.out.println("Bike 2 parked successfully. Ticket ID: " + bike2Ticket.getTicketId());
        }

        if (truck1Ticket != null) {
            System.out.println("Truck 1 parked successfully. Ticket ID: " + truck1Ticket.getTicketId());
        }

        if (truck2Ticket != null) {
            System.out.println("Truck 2 parked successfully. Ticket ID: " + truck2Ticket.getTicketId());
        } else {
            System.out.println("Failed to park Truck 2 - no available truck slots");
        }

        // Display status after parking
        System.out.println("\n=== STATUS AFTER PARKING ===");
        displayParkingLotStatus(parkingLot);

        // Wait a bit to simulate time passing
        System.out.println("\nWaiting for 2 hours...");
        Thread.sleep(2000); // Simulate 2 hours passing

        // Process exits with payment
        System.out.println("\n=== PROCESSING EXITS ===");
        PaymentStrategy cashPayment = new CashPayment();

        if (car1Ticket != null) {
            boolean exitSuccess = parkingLot.processExit(car1Ticket.getTicketId(), cashPayment);
            System.out.println("Car 1 exit: " + (exitSuccess ? "Success" : "Failed"));
        }

        if (bike1Ticket != null) {
            boolean exitSuccess = parkingLot.processExit(bike1Ticket.getTicketId(), cashPayment);
            System.out.println("Bike 1 exit: " + (exitSuccess ? "Success" : "Failed"));
        }

        // Display status after some exits
        System.out.println("\n=== STATUS AFTER SOME EXITS ===");
        displayParkingLotStatus(parkingLot);

        // Try to park another vehicle
        System.out.println("\n=== PARKING ADDITIONAL VEHICLE ===");
        Vehicle car3 = new Car("GHI789", false);
        ParkingTicket car3Ticket = parkingLot.parkVehicle(car3, "B1");

        if (car3Ticket != null) {
            System.out.println("Car 3 parked successfully. Ticket ID: " + car3Ticket.getTicketId());
        }

        // Display final status
        System.out.println("\n=== FINAL PARKING LOT STATUS ===");
        displayParkingLotStatus(parkingLot);

        // Test building status method
        System.out.println("\n=== BUILDING B1 DETAILED STATUS ===");
        Map<VehicleType, Integer> buildingStatus = parkingLot.getBuildingStatus("B1");
        if (buildingStatus != null) {
            for (VehicleType type : buildingStatus.keySet()) {
                System.out.println(type + " slots available: " + buildingStatus.get(type));
            }
        }
    }

    private static void displayParkingLotStatus(ParkingLot parkingLot) {
        System.out.println("Overall available slots:");
        for (VehicleType type : VehicleType.values()) {
            int count = parkingLot.getAvailableSlotsCount(type, null);
            System.out.println("  " + type + ": " + count);
        }

        System.out.println("Building B1 available slots:");
        for (VehicleType type : VehicleType.values()) {
            int count = parkingLot.getAvailableSlotsCount(type, "B1");
            System.out.println("  " + type + ": " + count);
        }

        System.out.println("Building B2 available slots:");
        for (VehicleType type : VehicleType.values()) {
            int count = parkingLot.getAvailableSlotsCount(type, "B2");
            System.out.println("  " + type + ": " + count);
        }
    }
}