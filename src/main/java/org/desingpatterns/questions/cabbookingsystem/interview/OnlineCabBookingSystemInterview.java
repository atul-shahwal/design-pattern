package org.desingpatterns.questions.cabbookingsystem.interview;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Interview Q&A: What are the main states a ride can be in?
 * Answer: A ride can be in one of these states: REQUESTED, DRIVER_ASSIGNED,
 * IN_PROGRESS, COMPLETED, or CANCELLED. This helps manage the ride lifecycle.
 */
enum RideStatus {
    REQUESTED, DRIVER_ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED
}

/**
 * Interview Q&A: What are the possible statuses for a driver?
 * Answer: A driver can be OFFLINE (not available for rides), ONLINE (available for rides),
 * or IN_RIDE (currently serving a ride).
 */
enum DriverStatus {
    OFFLINE, ONLINE, IN_RIDE
}

/**
 * Interview Q&A: Why is the Location class important in a cab booking system?
 * Answer: The Location class is crucial because it represents geographic coordinates
 * that are used for tracking users and drivers, calculating distances, and determining
 * optimal driver assignments based on proximity.
 */
class Location {
    double latitude;
    double longitude;
    long timestamp;

    Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = System.currentTimeMillis();
    }
}

/**
 * Interview Q&A: What information should a User entity contain in a cab booking system?
 * Answer: A User entity should contain:
 * 1. Unique identifier
 * 2. Personal information (name, contact details)
 * 3. Current location for ride matching
 * 4. Payment information (not shown in this simplified example)
 * 5. Ride history (not shown in this simplified example)
 */
class User {
    String userId;
    String name;
    String phone;
    AtomicReference<Location> currentLocation;

    User(String userId, String name, String phone) {
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.currentLocation = new AtomicReference<>();
    }

    /**
     * Interview Q&A: Why is thread safety important for user location updates?
     * Answer: Thread safety is crucial because multiple threads might try to update
     * the user's location simultaneously (e.g., from different API requests).
     * Using AtomicReference ensures that location updates are atomic and consistent.
     */
    void updateLocation(Location location) {
        currentLocation.set(location);
    }
}

/**
 * Interview Q&A: Why is vehicle information important in a cab booking system?
 * Answer: Vehicle information is important for:
 * 1. Identification - license plate helps identify the vehicle
 * 2. User experience - users may prefer certain vehicle types
 * 3. Safety and compliance - ensuring registered vehicles meet requirements
 * 4. Capacity planning - different vehicles have different passenger capacities
 */
class Vehicle {
    String licensePlate;
    String model;
    String color;

    Vehicle(String licensePlate, String model, String color) {
        this.licensePlate = licensePlate;
        this.model = model;
        this.color = color;
    }
}

/**
 * Interview Q&A: What are the key responsibilities of the Driver class?
 * Answer: The Driver class is responsible for:
 * 1. Managing driver status (online, offline, in-ride)
 * 2. Tracking driver location for ride matching
 * 3. Maintaining driver rating and performance metrics
 * 4. Managing vehicle information
 * 5. Handling availability for new ride requests
 */
class Driver {
    String driverId;
    String name;
    String phone;
    Vehicle vehicle;
    AtomicReference<Location> currentLocation;
    AtomicReference<DriverStatus> status;
    AtomicReference<Boolean> isAvailable;
    double rating;
    int totalRides;

    Driver(String driverId, String name, String phone, Vehicle vehicle) {
        this.driverId = driverId;
        this.name = name;
        this.phone = phone;
        this.vehicle = vehicle;
        this.currentLocation = new AtomicReference<>();
        this.status = new AtomicReference<>(DriverStatus.OFFLINE);
        this.isAvailable = new AtomicReference<>(false);
        this.rating = 5.0; // Default rating
        this.totalRides = 0;
    }

    /**
     * Interview Q&A: Why use AtomicReference for location updates?
     * Answer: AtomicReference ensures thread-safe updates to the driver's location
     * which might be updated by multiple threads concurrently (e.g., GPS updates).
     */
    void updateLocation(Location location) {
        currentLocation.set(location);
    }

    void setStatus(DriverStatus newStatus) {
        status.set(newStatus);
    }

    void setAvailable(boolean available) {
        isAvailable.set(available);
    }

    /**
     * Interview Q&A: How would you implement a thread-safe rating update?
     * Answer: We use a compare-and-swap approach with a loop to ensure thread safety
     * when updating the driver's rating and total rides count.
     */
    void updateRating(double newRating) {
        // Thread-safe rating update
        while (true) {
            double currentRating = this.rating;
            int currentTotalRides = this.totalRides;
            double newAverage = ((currentRating * currentTotalRides) + newRating) / (currentTotalRides + 1);

            if (Double.compare(currentRating, this.rating) == 0 && currentTotalRides == this.totalRides) {
                this.rating = newAverage;
                this.totalRides = currentTotalRides + 1;
                break;
            }
        }
    }
}

/**
 * Interview Q&A: Why use the State Pattern for ride management?
 * Answer: The State Pattern is ideal for ride management because:
 * 1. Rides have well-defined states with specific transitions
 * 2. Each state has different allowed operations
 * 3. It encapsulates state-specific behavior in separate classes
 * 4. It makes adding new states easier without changing existing code
 */
interface RideState {
    void requestRide(Ride ride);
    void assignDriver(Ride ride, Driver driver);
    void startRide(Ride ride);
    void completeRide(Ride ride);
    void cancelRide(Ride ride);
    RideStatus getStatus();
}

class RequestedState implements RideState {
    private static final RequestedState INSTANCE = new RequestedState();

    private RequestedState() {}

    static RequestedState getInstance() {
        return INSTANCE;
    }

    /**
     * Interview Q&A: What are the valid state transitions from the REQUESTED state?
     * Answer: From REQUESTED state, a ride can transition to:
     * 1. DRIVER_ASSIGNED (when a driver is assigned)
     * 2. CANCELLED (if the user cancels the ride)
     */
    @Override
    public void requestRide(Ride ride) {
        throw new IllegalStateException("Ride already requested");
    }

    @Override
    public void assignDriver(Ride ride, Driver driver) {
        if (!driver.isAvailable.get()) {
            throw new IllegalStateException("Driver is not available");
        }

        driver.setAvailable(false);
        driver.setStatus(DriverStatus.IN_RIDE);
        ride.driver = driver;
        ride.state.set(AssignedState.getInstance());
    }

    @Override
    public void startRide(Ride ride) {
        throw new IllegalStateException("Cannot start ride without driver assignment");
    }

    @Override
    public void completeRide(Ride ride) {
        throw new IllegalStateException("Cannot complete ride that hasn't started");
    }

    @Override
    public void cancelRide(Ride ride) {
        ride.state.set(CancelledState.getInstance());
    }

    @Override
    public RideStatus getStatus() {
        return RideStatus.REQUESTED;
    }
}

class AssignedState implements RideState {
    private static final AssignedState INSTANCE = new AssignedState();

    private AssignedState() {}

    static AssignedState getInstance() {
        return INSTANCE;
    }

    /**
     * Interview Q&A: What are the valid state transitions from the DRIVER_ASSIGNED state?
     * Answer: From DRIVER_ASSIGNED state, a ride can transition to:
     * 1. IN_PROGRESS (when the ride starts)
     * 2. CANCELLED (if the user or driver cancels before starting)
     */
    @Override
    public void requestRide(Ride ride) {
        throw new IllegalStateException("Ride already assigned to a driver");
    }

    @Override
    public void assignDriver(Ride ride, Driver driver) {
        throw new IllegalStateException("Ride already assigned to a driver");
    }

    @Override
    public void startRide(Ride ride) {
        ride.startTime = System.currentTimeMillis();
        ride.state.set(InProgressState.getInstance());
    }

    @Override
    public void completeRide(Ride ride) {
        throw new IllegalStateException("Cannot complete ride that hasn't started");
    }

    @Override
    public void cancelRide(Ride ride) {
        Driver driver = ride.driver;
        if (driver != null) {
            driver.setAvailable(true);
            driver.setStatus(DriverStatus.ONLINE);
        }
        ride.state.set(CancelledState.getInstance());
    }

    @Override
    public RideStatus getStatus() {
        return RideStatus.DRIVER_ASSIGNED;
    }
}

class InProgressState implements RideState {
    private static final InProgressState INSTANCE = new InProgressState();

    private InProgressState() {}

    static InProgressState getInstance() {
        return INSTANCE;
    }

    /**
     * Interview Q&A: What are the valid state transitions from the IN_PROGRESS state?
     * Answer: From IN_PROGRESS state, a ride can only transition to COMPLETED
     * (when the ride finishes) as cancelling an ongoing ride is typically not allowed.
     */
    @Override
    public void requestRide(Ride ride) {
        throw new IllegalStateException("Ride is in progress");
    }

    @Override
    public void assignDriver(Ride ride, Driver driver) {
        throw new IllegalStateException("Ride is in progress");
    }

    @Override
    public void startRide(Ride ride) {
        throw new IllegalStateException("Ride already started");
    }

    @Override
    public void completeRide(Ride ride) {
        ride.endTime = System.currentTimeMillis();
        ride.calculateFare();

        Driver driver = ride.driver;
        if (driver != null) {
            driver.setAvailable(true);
            driver.setStatus(DriverStatus.ONLINE);
        }

        ride.state.set(CompletedState.getInstance());
    }

    @Override
    public void cancelRide(Ride ride) {
        throw new IllegalStateException("Cannot cancel ride that is in progress");
    }

    @Override
    public RideStatus getStatus() {
        return RideStatus.IN_PROGRESS;
    }
}

class CompletedState implements RideState {
    private static final CompletedState INSTANCE = new CompletedState();

    private CompletedState() {}

    static CompletedState getInstance() {
        return INSTANCE;
    }

    /**
     * Interview Q&A: What state transitions are possible from the COMPLETED state?
     * Answer: From COMPLETED state, no further transitions are allowed as the ride
     * has already ended. This is a terminal state.
     */
    @Override
    public void requestRide(Ride ride) {
        throw new IllegalStateException("Ride already completed");
    }

    @Override
    public void assignDriver(Ride ride, Driver driver) {
        throw new IllegalStateException("Ride already completed");
    }

    @Override
    public void startRide(Ride ride) {
        throw new IllegalStateException("Ride already completed");
    }

    @Override
    public void completeRide(Ride ride) {
        throw new IllegalStateException("Ride already completed");
    }

    @Override
    public void cancelRide(Ride ride) {
        throw new IllegalStateException("Cannot cancel completed ride");
    }

    @Override
    public RideStatus getStatus() {
        return RideStatus.COMPLETED;
    }
}

class CancelledState implements RideState {
    private static final CancelledState INSTANCE = new CancelledState();

    private CancelledState() {}

    static CancelledState getInstance() {
        return INSTANCE;
    }

    /**
     * Interview Q&A: What state transitions are possible from the CANCELLED state?
     * Answer: From CANCELLED state, no further transitions are allowed as the ride
     * has been cancelled. This is a terminal state.
     */
    @Override
    public void requestRide(Ride ride) {
        throw new IllegalStateException("Ride was cancelled");
    }

    @Override
    public void assignDriver(Ride ride, Driver driver) {
        throw new IllegalStateException("Ride was cancelled");
    }

    @Override
    public void startRide(Ride ride) {
        throw new IllegalStateException("Ride was cancelled");
    }

    @Override
    public void completeRide(Ride ride) {
        throw new IllegalStateException("Ride was cancelled");
    }

    @Override
    public void cancelRide(Ride ride) {
        throw new IllegalStateException("Ride already cancelled");
    }

    @Override
    public RideStatus getStatus() {
        return RideStatus.CANCELLED;
    }
}

/**
 * Interview Q&A: What information does a Ride entity need to track?
 * Answer: A Ride entity needs to track:
 * 1. Ride identification and status
 * 2. User and driver information
 * 3. Pickup and dropoff locations
 * 4. Timestamps for different ride stages
 * 5. Fare calculation
 * 6. Payment status (not shown in this simplified example)
 */
class Ride {
    String rideId;
    User user;
    Location pickupLocation;
    Location dropoffLocation;
    long createdAt;

    Driver driver;
    double fare;
    Long startTime;
    Long endTime;
    AtomicReference<RideState> state;

    ReentrantLock lock = new ReentrantLock();

    // Fare calculation constants (in INR)
    static final double BASE_FARE = 40.0; // Base fare in INR
    static final double PER_KM_RATE = 12.0; // Rate per km in INR
    static final double PER_MINUTE_RATE = 2.0; // Rate per minute in INR
    static final double MINIMUM_FARE = 80.0; // Minimum fare in INR

    Ride(String rideId, User user, Location pickupLocation, Location dropoffLocation) {
        this.rideId = rideId;
        this.user = user;
        this.pickupLocation = pickupLocation;
        this.dropoffLocation = dropoffLocation;
        this.createdAt = System.currentTimeMillis();
        this.state = new AtomicReference<>(RequestedState.getInstance());
    }

    RideStatus getStatus() {
        return state.get().getStatus();
    }

    void assignDriver(Driver driver) {
        lock.lock();
        try {
            state.get().assignDriver(this, driver);
        } finally {
            lock.unlock();
        }
    }

    void startRide() {
        lock.lock();
        try {
            state.get().startRide(this);
        } finally {
            lock.unlock();
        }
    }

    void completeRide() {
        lock.lock();
        try {
            state.get().completeRide(this);
        } finally {
            lock.unlock();
        }
    }

    void cancelRide() {
        lock.lock();
        try {
            state.get().cancelRide(this);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Interview Q&A: How would you design a fare calculation system for a cab service?
     * Answer: A fare calculation system should consider:
     * 1. Base fare - fixed amount for any ride
     * 2. Distance-based fare - variable amount based on distance traveled
     * 3. Time-based fare - variable amount based on time taken
     * 4. Surge pricing - multiplier during high demand
     * 5. Minimum fare - ensuring the driver gets paid fairly for short rides
     * 6. Additional charges - tolls, waiting time, etc.
     */
    void calculateFare() {
        // Calculate distance in km
        double distance = calculateDistance(pickupLocation, dropoffLocation);

        // Calculate time in minutes
        long durationMinutes = (endTime - startTime) / (60 * 1000);

        // Calculate fare components
        double distanceFare = distance * PER_KM_RATE;
        double timeFare = durationMinutes * PER_MINUTE_RATE;

        // Total fare with minimum guarantee
        this.fare = Math.max(MINIMUM_FARE, BASE_FARE + distanceFare + timeFare);
    }

    /**
     * Interview Q&A: How does the Haversine formula work for distance calculation?
     * Answer: The Haversine formula calculates the great-circle distance between two points
     * on a sphere given their longitudes and latitudes. It's more accurate than simple
     * Euclidean distance for geographical coordinates because it accounts for Earth's curvature.
     */
    private double calculateDistance(Location loc1, Location loc2) {
        // Haversine formula implementation
        final int R = 6371; // Earth's radius in km

        double lat1 = Math.toRadians(loc1.latitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double deltaLat = Math.toRadians(loc2.latitude - loc1.latitude);
        double deltaLon = Math.toRadians(loc2.longitude - loc1.longitude);

        double a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(deltaLon/2) * Math.sin(deltaLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return R * c;
    }
}

/**
 * Interview Q&A: Why use the Strategy Pattern for driver matching?
 * Answer: The Strategy Pattern is ideal for driver matching because:
 * 1. It allows different matching algorithms to be implemented interchangeably
 * 2. It makes adding new matching strategies easy without changing existing code
 * 3. It encapsulates each matching algorithm in its own class
 * 4. It allows runtime selection of matching strategy based on business needs
 */
interface MatchingStrategy {
    Driver findBestDriver(Ride ride, List<Driver> availableDrivers);
}

/**
 * Interview Q&A: What are the pros and cons of distance-based matching?
 * Answer: Pros:
 * 1. Minimizes pickup time for users
 * 2. Reduces fuel consumption for drivers
 * 3. Simple to implement and understand
 *
 * Cons:
 * 1. May not consider driver ratings or other quality factors
 * 2. Could lead to uneven distribution of rides among drivers
 * 3. Doesn't account for traffic conditions
 */
class DistanceBasedMatching implements MatchingStrategy {
    @Override
    public Driver findBestDriver(Ride ride, List<Driver> availableDrivers) {
        if (availableDrivers.isEmpty()) {
            return null;
        }

        Driver closestDriver = null;
        double minDistance = Double.MAX_VALUE;

        for (Driver driver : availableDrivers) {
            double distance = calculateDistance(
                    ride.pickupLocation,
                    driver.currentLocation.get()
            );

            if (distance < minDistance) {
                minDistance = distance;
                closestDriver = driver;
            }
        }

        return closestDriver;
    }
    /**
     * Interview Q&A: How does the Haversine formula work for distance calculation?
     * Answer:
     * The Haversine formula calculates the great-circle distance between two points
     * on a sphere given their latitudes and longitudes. It is more accurate than using
     * simple Euclidean distance because it accounts for the Earth's curvature.
     *
     * Mathematical formula:
     *
     * a = sin²(Δφ / 2) + cos(φ1) * cos(φ2) * sin²(Δλ / 2)
     * c = 2 * atan2(√a, √(1 − a))
     * d = R * c
     *
     * where:
     * - φ1, φ2 are the latitudes of the two points in radians
     * - λ1, λ2 are the longitudes of the two points in radians
     * - Δφ = φ2 - φ1 is the difference in latitude
     * - Δλ = λ2 - λ1 is the difference in longitude
     * - R is the Earth's radius (mean radius = 6371 km)
     * - d is the calculated distance between the two points along the surface of the sphere
     *
     */
    private double calculateDistance(Location loc1, Location loc2) {
        final int R = 6371; // Earth's radius in km

        double lat1 = Math.toRadians(loc1.latitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double deltaLat = Math.toRadians(loc2.latitude - loc1.latitude);
        double deltaLon = Math.toRadians(loc2.longitude - loc1.longitude);

        double a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(deltaLon/2) * Math.sin(deltaLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return R * c;
    }
}

/**
 * Interview Q&A: What are the responsibilities of the DriverService?
 * Answer: The DriverService is responsible for:
 * 1. Managing driver registration and information
 * 2. Updating driver status and location
 * 3. Finding nearby available drivers
 * 4. Coordinating with the spatial index for location queries
 */
class DriverService {
    Map<String, Driver> drivers;
    List<Driver> availableDrivers;

    DriverService() {
        this.drivers = new ConcurrentHashMap<>();
        this.availableDrivers = new CopyOnWriteArrayList<>();
    }

    void registerDriver(Driver driver) {
        drivers.put(driver.driverId, driver);
    }

    /**
     * Interview Q&A: How do you handle driver status updates?
     * Answer: When a driver's status changes, we update their availability and
     * add/remove them from the available drivers list accordingly. This ensures
     * that only online and available drivers are considered for ride assignments.
     */
    void updateDriverStatus(String driverId, DriverStatus status) {
        Driver driver = drivers.get(driverId);
        if (driver != null) {
            DriverStatus oldStatus = driver.status.get();
            driver.setStatus(status);
            driver.setAvailable(status == DriverStatus.ONLINE);

            if (status == DriverStatus.ONLINE && oldStatus != DriverStatus.ONLINE) {
                availableDrivers.add(driver);
            } else if (status != DriverStatus.ONLINE && oldStatus == DriverStatus.ONLINE) {
                availableDrivers.remove(driver);
            }
        }
    }

    boolean updateDriverLocation(String driverId, Location location) {
        Driver driver = drivers.get(driverId);
        if (driver != null) {
            driver.updateLocation(location);
            return true;
        }
        return false;
    }

    /**
     * Interview Q&A: How would you find nearby drivers without spatial indexing?
     * Answer: Without spatial indexing, we can use a linear search through all available
     * drivers, calculating the distance from the pickup location to each driver's location.
     * This is simpler to implement but less efficient for large numbers of drivers.
     */
    List<Driver> findNearbyDrivers(Location location, double radiusKm) {
        List<Driver> nearbyDrivers = new ArrayList<>();

        for (Driver driver : availableDrivers) {
            if (driver.currentLocation.get() != null) {
                double distance = calculateDistance(location, driver.currentLocation.get());
                if (distance <= radiusKm) {
                    nearbyDrivers.add(driver);
                }
            }
        }

        return nearbyDrivers;
    }

    /**
     * Interview Q&A: How does the Haversine formula work for distance calculation?
     * Answer:
     * The Haversine formula calculates the great-circle distance between two points
     * on a sphere given their latitudes and longitudes. It is more accurate than using
     * simple Euclidean distance because it accounts for the Earth's curvature.
     *
     * Mathematical formula:
     *
     * a = sin²(Δφ / 2) + cos(φ1) * cos(φ2) * sin²(Δλ / 2)
     * c = 2 * atan2(√a, √(1 − a))
     * d = R * c
     *
     * where:
     * - φ1, φ2 are the latitudes of the two points in radians
     * - λ1, λ2 are the longitudes of the two points in radians
     * - Δφ = φ2 - φ1 is the difference in latitude
     * - Δλ = λ2 - λ1 is the difference in longitude
     * - R is the Earth's radius (mean radius = 6371 km)
     * - d is the calculated distance between the two points along the surface of the sphere
     */
    private double calculateDistance(Location loc1, Location loc2) {
        final int R = 6371; // Earth's radius in kilometers

        // Convert latitude and longitude from degrees to radians
        double lat1 = Math.toRadians(loc1.latitude); // φ1
        double lat2 = Math.toRadians(loc2.latitude); // φ2
        double deltaLat = Math.toRadians(loc2.latitude - loc1.latitude); // Δφ
        double deltaLon = Math.toRadians(loc2.longitude - loc1.longitude); // Δλ

        // Apply the Haversine formula
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) + // sin²(Δφ / 2)
                Math.cos(lat1) * Math.cos(lat2) *                 // cos(φ1) * cos(φ2)
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);   // sin²(Δλ / 2)

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));    // 2 * atan2(√a, √(1 − a))

        return R * c; // d = R * c
    }


    Driver getDriver(String driverId) {
        return drivers.get(driverId);
    }
}

/**
 * Interview Q&A: What are the responsibilities of the RideService?
 * Answer: The RideService is responsible for:
 * 1. Managing the ride lifecycle from request to completion
 * 2. Coordinating with DriverService to find available drivers
 * 3. Using the matching strategy to assign the best driver
 * 4. Handling ride state transitions
 * 5. Calculating fares
 */
class RideService {
    Map<String, Ride> activeRides;
    DriverService driverService;
    MatchingStrategy matchingStrategy;

    RideService(DriverService driverService, MatchingStrategy matchingStrategy) {
        this.activeRides = new ConcurrentHashMap<>();
        this.driverService = driverService;
        this.matchingStrategy = matchingStrategy;
    }

    /**
     * Interview Q&A: What steps are involved in processing a ride request?
     * Answer: Processing a ride request involves:
     * 1. Creating a ride object with user and location details
     * 2. Finding nearby available drivers using spatial indexing
     * 3. Selecting the best driver using the matching strategy
     * 4. Assigning the driver to the ride
     * 5. Handling the case where no drivers are available
     */
    Ride requestRide(User user, Location pickup, Location dropoff) {
        String rideId = UUID.randomUUID().toString();
        Ride ride = new Ride(rideId, user, pickup, dropoff);

        activeRides.put(rideId, ride);

        // Find nearby available drivers with a larger search radius
        List<Driver> nearbyDrivers = driverService.findNearbyDrivers(pickup, 200.0); // 200km radius

        // Use matching strategy to find the best driver
        Driver driver = matchingStrategy.findBestDriver(ride, nearbyDrivers);

        if (driver != null) {
            ride.assignDriver(driver);
        } else {
            ride.cancelRide();
        }

        return ride;
    }

    void startRide(String rideId) {
        Ride ride = activeRides.get(rideId);
        if (ride != null) {
            ride.startRide();
        }
    }

    void completeRide(String rideId) {
        Ride ride = activeRides.get(rideId);
        if (ride != null) {
            ride.completeRide();
            activeRides.remove(rideId);
        }
    }

    void cancelRide(String rideId) {
        Ride ride = activeRides.get(rideId);
        if (ride != null) {
            ride.cancelRide();
            activeRides.remove(rideId);
        }
    }

    Ride getRide(String rideId) {
        return activeRides.get(rideId);
    }
}

// Main class to demonstrate the system
public class OnlineCabBookingSystemInterview {
    public static void main(String[] args) {
        // Initialize services
        DriverService driverService = new DriverService();
        MatchingStrategy matchingStrategy = new DistanceBasedMatching();
        RideService rideService = new RideService(driverService, matchingStrategy);

        // Create some drivers
        Vehicle vehicle1 = new Vehicle("ABC123", "Toyota Camry", "White");
        Vehicle vehicle2 = new Vehicle("XYZ789", "Honda Accord", "Black");

        Driver driver1 = new Driver("D1", "John Doe", "555-1234", vehicle1);
        Driver driver2 = new Driver("D2", "Jane Smith", "555-5678", vehicle2);

        driverService.registerDriver(driver1);
        driverService.registerDriver(driver2);

        // Set drivers to online and update their locations
        driverService.updateDriverStatus("D1", DriverStatus.ONLINE);
        driverService.updateDriverStatus("D2", DriverStatus.ONLINE);

        driverService.updateDriverLocation("D1", new Location(10, 10));
        driverService.updateDriverLocation("D2", new Location(12, 12));

        // Create a user
        User user = new User("U1", "Alice Johnson", "555-0001");
        user.updateLocation(new Location(11, 11));

        // Request a ride
        Ride ride = rideService.requestRide(user, new Location(11, 11), new Location(20, 20));

        System.out.println("Ride status: " + ride.getStatus());

        if (ride.getStatus() == RideStatus.DRIVER_ASSIGNED) {
            System.out.println("Assigned driver: " + ride.driver.name);

            // Simulate ride progress by adding a delay
            try {
                Thread.sleep(1000); // Simulate 1 minute ride duration
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Start the ride only if a driver was assigned
            rideService.startRide(ride.rideId);
            System.out.println("Ride status after starting: " + ride.getStatus());

            // Complete the ride
            rideService.completeRide(ride.rideId);
            System.out.println("Ride status after completion: " + ride.getStatus());
            System.out.println("Ride fare: ₹" + String.format("%.2f", ride.fare));
        } else {
            System.out.println("Ride could not be assigned to a driver. It was likely cancelled automatically.");
        }
    }
}