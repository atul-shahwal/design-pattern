package org.desingpatterns.questions.cabbookingsystem.not_good_for_interview;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Interview Q&A: What are the main components of a cab booking system?
 * Answer: The main components include:
 * 1. User Management - handling user registration and location updates
 * 2. Driver Management - handling driver registration, availability, and location tracking
 * 3. Ride Management - handling ride requests, assignment, and completion
 * 4. Spatial Indexing - efficiently finding nearby drivers
 * 5. Pricing Strategy - calculating fares based on distance and other factors
 * 6. https://www.youtube.com/watch?v=OcUKFIjhKu0&ab_channel=GauravSen
 */

// Enums
enum RideStatus {
    REQUESTED, DRIVER_ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED
}

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
    private final double latitude;
    private final double longitude;
    private final long timestamp;

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = System.currentTimeMillis();
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "Location{latitude=" + latitude + ", longitude=" + longitude + "}";
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
    private final String userId;
    private final String name;
    private final String phone;
    private final AtomicReference<Location> currentLocation;

    public User(String userId, String name, String phone) {
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.currentLocation = new AtomicReference<>();
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }

    public Location getCurrentLocation() {
        return currentLocation.get();
    }

    public void updateLocation(Location location) {
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
    private final String licensePlate;
    private final String model;
    private final String color;

    public Vehicle(String licensePlate, String model, String color) {
        this.licensePlate = licensePlate;
        this.model = model;
        this.color = color;
    }

    public String getLicensePlate() { return licensePlate; }
    public String getModel() { return model; }
    public String getColor() { return color; }
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
    private final String driverId;
    private final String name;
    private final String phone;
    private final Vehicle vehicle;
    private final AtomicReference<Location> currentLocation;
    private final AtomicReference<DriverStatus> status;
    private final AtomicReference<Boolean> isAvailable;
    private double rating;
    private int totalRides;

    public Driver(String driverId, String name, String phone, Vehicle vehicle) {
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

    public String getDriverId() { return driverId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public Vehicle getVehicle() { return vehicle; }
    public double getRating() { return rating; }
    public int getTotalRides() { return totalRides; }

    public Location getCurrentLocation() {
        return currentLocation.get();
    }

    public void updateLocation(Location location) {
        currentLocation.set(location);
    }

    public DriverStatus getStatus() {
        return status.get();
    }

    public void setStatus(DriverStatus newStatus) {
        status.set(newStatus);
    }

    public boolean isAvailable() {
        return isAvailable.get();
    }

    public void setAvailable(boolean available) {
        isAvailable.set(available);
    }

    /**
     * Interview Q&A: How would you implement a thread-safe rating update?
     * Answer: We use a compare-and-swap approach with a loop to ensure thread safety
     * when updating the driver's rating and total rides count.
     */
    public void updateRating(double newRating) {
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

// Ride State Pattern

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

    public static RequestedState getInstance() {
        return INSTANCE;
    }

    @Override
    public void requestRide(Ride ride) {
        throw new IllegalStateException("Ride already requested");
    }

    @Override
    public void assignDriver(Ride ride, Driver driver) {
        if (!driver.isAvailable()) {
            throw new IllegalStateException("Driver is not available");
        }

        driver.setAvailable(false);
        driver.setStatus(DriverStatus.IN_RIDE);
        ride.setDriver(driver);
        ride.setState(AssignedState.getInstance());
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
        ride.setState(CancelledState.getInstance());
    }

    @Override
    public RideStatus getStatus() {
        return RideStatus.REQUESTED;
    }
}

class AssignedState implements RideState {
    private static final AssignedState INSTANCE = new AssignedState();

    private AssignedState() {}

    public static AssignedState getInstance() {
        return INSTANCE;
    }

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
        ride.setStartTime(System.currentTimeMillis());
        ride.setState(InProgressState.getInstance());
    }

    @Override
    public void completeRide(Ride ride) {
        throw new IllegalStateException("Cannot complete ride that hasn't started");
    }

    @Override
    public void cancelRide(Ride ride) {
        Driver driver = ride.getDriver();
        if (driver != null) {
            driver.setAvailable(true);
            driver.setStatus(DriverStatus.ONLINE);
        }
        ride.setState(CancelledState.getInstance());
    }

    @Override
    public RideStatus getStatus() {
        return RideStatus.DRIVER_ASSIGNED;
    }
}

class InProgressState implements RideState {
    private static final InProgressState INSTANCE = new InProgressState();

    private InProgressState() {}

    public static InProgressState getInstance() {
        return INSTANCE;
    }

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
        ride.setEndTime(System.currentTimeMillis());
        ride.calculateFare();

        Driver driver = ride.getDriver();
        if (driver != null) {
            driver.setAvailable(true);
            driver.setStatus(DriverStatus.ONLINE);
        }

        ride.setState(CompletedState.getInstance());
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

    public static CompletedState getInstance() {
        return INSTANCE;
    }

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

    public static CancelledState getInstance() {
        return INSTANCE;
    }

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
    private final String rideId;
    private final User user;
    private final Location pickupLocation;
    private final Location dropoffLocation;
    private final long createdAt;

    private Driver driver;
    private double fare;
    private Long startTime;
    private Long endTime;
    private AtomicReference<RideState> state;

    private final ReentrantLock lock = new ReentrantLock();

    // Fare calculation constants (in INR)
    private static final double BASE_FARE = 40.0; // Base fare in INR
    private static final double PER_KM_RATE = 12.0; // Rate per km in INR
    private static final double PER_MINUTE_RATE = 2.0; // Rate per minute in INR
    private static final double MINIMUM_FARE = 80.0; // Minimum fare in INR

    public Ride(String rideId, User user, Location pickupLocation, Location dropoffLocation) {
        this.rideId = rideId;
        this.user = user;
        this.pickupLocation = pickupLocation;
        this.dropoffLocation = dropoffLocation;
        this.createdAt = System.currentTimeMillis();
        this.state = new AtomicReference<>(RequestedState.getInstance());
    }

    public String getRideId() { return rideId; }
    public User getUser() { return user; }
    public Location getPickupLocation() { return pickupLocation; }
    public Location getDropoffLocation() { return dropoffLocation; }
    public Driver getDriver() { return driver; }
    public double getFare() { return fare; }
    public long getCreatedAt() { return createdAt; }
    public Long getStartTime() { return startTime; }
    public Long getEndTime() { return endTime; }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setState(RideState state) {
        this.state.set(state);
    }

    public RideStatus getStatus() {
        return state.get().getStatus();
    }

    public void assignDriver(Driver driver) {
        lock.lock();
        try {
            state.get().assignDriver(this, driver);
        } finally {
            lock.unlock();
        }
    }

    public void startRide() {
        lock.lock();
        try {
            state.get().startRide(this);
        } finally {
            lock.unlock();
        }
    }

    public void completeRide() {
        lock.lock();
        try {
            state.get().completeRide(this);
        } finally {
            lock.unlock();
        }
    }

    public void cancelRide() {
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
    public void calculateFare() {
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

        double lat1 = Math.toRadians(loc1.getLatitude());
        double lat2 = Math.toRadians(loc2.getLatitude());
        double deltaLat = Math.toRadians(loc2.getLatitude() - loc1.getLatitude());
        double deltaLon = Math.toRadians(loc2.getLongitude() - loc1.getLongitude());

        double a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(deltaLon/2) * Math.sin(deltaLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return R * c;
    }
}

// Spatial Indexing with QuadTree

/**
 * Interview Q&A: Why use spatial indexing like QuadTree in a cab booking system?
 * Answer: Spatial indexing like QuadTree is essential for:
 * 1. Efficiently finding nearby drivers without scanning all drivers
 * 2. Scaling to large numbers of drivers and users
 * 3. Reducing computation time for location-based queries
 * 4. Supporting real-time location updates
 */
class BoundingBox {
    private final double minLat;
    private final double minLon;
    private final double maxLat;
    private final double maxLon;

    public BoundingBox(double minLat, double minLon, double maxLat, double maxLon) {
        this.minLat = minLat;
        this.minLon = minLon;
        this.maxLat = maxLat;
        this.maxLon = maxLon;
    }

    public boolean contains(Location location) {
        return location.getLatitude() >= minLat &&
                location.getLatitude() <= maxLat &&
                location.getLongitude() >= minLon &&
                location.getLongitude() <= maxLon;
    }

    public boolean intersects(BoundingBox other) {
        return !(other.minLat > maxLat ||
                other.maxLat < minLat ||
                other.minLon > maxLon ||
                other.maxLon < minLon);
    }

    public double getMinLat() { return minLat; }
    public double getMinLon() { return minLon; }
    public double getMaxLat() { return maxLat; }
    public double getMaxLon() { return maxLon; }
}

/**
 * Interview Q&A: What are the advantages of using a QuadTree for spatial indexing?
 * Answer: Advantages of QuadTree for spatial indexing:
 * 1. Efficient range queries - quickly find all points within a bounding box
 * 2. Dynamic updates - easily add, remove, or update points
 * 3. Memory efficiency - only stores points that exist, not the entire grid
 * 4. Adaptability - automatically adjusts density by subdividing when needed
 * 5. Good worst-case performance - O(log n) for most operations
 */
class QuadTreeNode {
    private static final int CAPACITY = 4;
    private static final int MAX_DEPTH = 10;

    private final BoundingBox boundary;
    private final int depth;
    private final Map<String, Location> points;
    private QuadTreeNode[] children;
    private boolean isDivided;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public QuadTreeNode(BoundingBox boundary, int depth) {
        this.boundary = boundary;
        this.depth = depth;
        this.points = new ConcurrentHashMap<>();
        this.children = new QuadTreeNode[4];
        this.isDivided = false;
    }

    public boolean insert(String driverId, Location location) {
        lock.writeLock().lock();
        try {
            if (!boundary.contains(location)) {
                return false;
            }

            if (points.size() < CAPACITY || depth >= MAX_DEPTH) {
                points.put(driverId, location);
                return true;
            }

            if (!isDivided) {
                subdivide();
            }

            for (QuadTreeNode child : children) {
                if (child.insert(driverId, location)) {
                    return true;
                }
            }

            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean remove(String driverId, Location location) {
        lock.writeLock().lock();
        try {
            if (!boundary.contains(location)) {
                return false;
            }

            if (points.remove(driverId) != null) {
                return true;
            }

            if (isDivided) {
                for (QuadTreeNode child : children) {
                    if (child.remove(driverId, location)) {
                        return true;
                    }
                }
            }

            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<String> queryRange(BoundingBox range) {
        lock.readLock().lock();
        try {
            List<String> results = new ArrayList<>();

            if (!boundary.intersects(range)) {
                return results;
            }

            for (Map.Entry<String, Location> entry : points.entrySet()) {
                if (range.contains(entry.getValue())) {
                    results.add(entry.getKey());
                }
            }

            if (isDivided) {
                for (QuadTreeNode child : children) {
                    results.addAll(child.queryRange(range));
                }
            }

            return results;
        } finally {
            lock.readLock().unlock();
        }
    }

    private void subdivide() {
        double midLat = (boundary.getMinLat() + boundary.getMaxLat()) / 2;
        double midLon = (boundary.getMinLon() + boundary.getMaxLon()) / 2;

        children[0] = new QuadTreeNode(
                new BoundingBox(boundary.getMinLat(), boundary.getMinLon(), midLat, midLon),
                depth + 1
        );
        children[1] = new QuadTreeNode(
                new BoundingBox(midLat, boundary.getMinLon(), boundary.getMaxLat(), midLon),
                depth + 1
        );
        children[2] = new QuadTreeNode(
                new BoundingBox(boundary.getMinLat(), midLon, midLat, boundary.getMaxLon()),
                depth + 1
        );
        children[3] = new QuadTreeNode(
                new BoundingBox(midLat, midLon, boundary.getMaxLat(), boundary.getMaxLon()),
                depth + 1
        );

        isDivided = true;
    }
}

class QuadTreeSpatialIndex {
    private final QuadTreeNode root;
    private final Map<String, Location> driverLocations;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public QuadTreeSpatialIndex(BoundingBox boundary) {
        this.root = new QuadTreeNode(boundary, 0);
        this.driverLocations = new ConcurrentHashMap<>();
    }

    public boolean addDriver(String driverId, Location location) {
        lock.writeLock().lock();
        try {
            if (root.insert(driverId, location)) {
                driverLocations.put(driverId, location);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean updateDriverLocation(String driverId, Location newLocation) {
        lock.writeLock().lock();
        try {
            Location oldLocation = driverLocations.get(driverId);
            if (oldLocation != null) {
                root.remove(driverId, oldLocation);
            }

            if (root.insert(driverId, newLocation)) {
                driverLocations.put(driverId, newLocation);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean removeDriver(String driverId) {
        lock.writeLock().lock();
        try {
            Location location = driverLocations.get(driverId);
            if (location != null) {
                root.remove(driverId, location);
                driverLocations.remove(driverId);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<String> findWithinRadius(Location center, double radiusKm) {
        lock.readLock().lock();
        try {
            // Convert radius to degrees (approximate)
            double radiusDeg = radiusKm / 111.0;

            BoundingBox searchArea = new BoundingBox(
                    center.getLatitude() - radiusDeg,
                    center.getLongitude() - radiusDeg,
                    center.getLatitude() + radiusDeg,
                    center.getLongitude() + radiusDeg
            );

            return root.queryRange(searchArea);
        } finally {
            lock.readLock().unlock();
        }
    }
}

// Matching Strategy Pattern

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
                    ride.getPickupLocation(),
                    driver.getCurrentLocation()
            );

            if (distance < minDistance) {
                minDistance = distance;
                closestDriver = driver;
            }
        }

        return closestDriver;
    }

    private double calculateDistance(Location loc1, Location loc2) {
        final int R = 6371; // Earth's radius in km

        double lat1 = Math.toRadians(loc1.getLatitude());
        double lat2 = Math.toRadians(loc2.getLatitude());
        double deltaLat = Math.toRadians(loc2.getLatitude() - loc1.getLatitude());
        double deltaLon = Math.toRadians(loc2.getLongitude() - loc1.getLongitude());

        double a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(deltaLon/2) * Math.sin(deltaLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return R * c;
    }
}

/**
 * Interview Q&A: What are the pros and cons of rating-based matching?
 * Answer: Pros:
 * 1. Provides better user experience by assigning higher-rated drivers
 * 2. Incentivizes drivers to maintain high ratings
 * 3. Can consider multiple factors beyond just distance
 *
 * Cons:
 * 1. May lead to longer pickup times
 * 2. Could disadvantage new drivers with no rating history
 * 3. More complex to implement and tune
 */
class RatingBasedMatching implements MatchingStrategy {
    @Override
    public Driver findBestDriver(Ride ride, List<Driver> availableDrivers) {
        if (availableDrivers.isEmpty()) {
            return null;
        }

        Driver bestDriver = null;
        double bestScore = Double.MIN_VALUE;

        for (Driver driver : availableDrivers) {
            double distance = calculateDistance(
                    ride.getPickupLocation(),
                    driver.getCurrentLocation()
            );

            // Score based on rating and distance (higher rating and shorter distance = better)
            double score = driver.getRating() * (1 / (1 + distance));

            if (score > bestScore) {
                bestScore = score;
                bestDriver = driver;
            }
        }

        return bestDriver;
    }

    private double calculateDistance(Location loc1, Location loc2) {
        final int R = 6371; // Earth's radius in km

        double lat1 = Math.toRadians(loc1.getLatitude());
        double lat2 = Math.toRadians(loc2.getLatitude());
        double deltaLat = Math.toRadians(loc2.getLatitude() - loc1.getLatitude());
        double deltaLon = Math.toRadians(loc2.getLongitude() - loc1.getLongitude());

        double a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(deltaLon/2) * Math.sin(deltaLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return R * c;
    }
}

// Services

/**
 * Interview Q&A: What are the responsibilities of the DriverService?
 * Answer: The DriverService is responsible for:
 * 1. Managing driver registration and information
 * 2. Updating driver status and location
 * 3. Finding nearby available drivers
 * 4. Coordinating with the spatial index for location queries
 */
class DriverService {
    private final Map<String, Driver> drivers;
    private final QuadTreeSpatialIndex spatialIndex;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public DriverService(QuadTreeSpatialIndex spatialIndex) {
        this.drivers = new ConcurrentHashMap<>();
        this.spatialIndex = spatialIndex;
    }

    public void registerDriver(Driver driver) {
        lock.writeLock().lock();
        try {
            drivers.put(driver.getDriverId(), driver);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateDriverStatus(String driverId, DriverStatus status) {
        lock.writeLock().lock();
        try {
            Driver driver = drivers.get(driverId);
            if (driver != null) {
                DriverStatus oldStatus = driver.getStatus();
                driver.setStatus(status);
                driver.setAvailable(status == DriverStatus.ONLINE);

                // Update the spatial index based on the status change
                if (status == DriverStatus.ONLINE && oldStatus != DriverStatus.ONLINE) {
                    // Driver came online, add them to the index
                    Location currentLocation = driver.getCurrentLocation();
                    if (currentLocation != null) {
                        spatialIndex.addDriver(driverId, currentLocation);
                    }
                } else if (status != DriverStatus.ONLINE && oldStatus == DriverStatus.ONLINE) {
                    // Driver went offline or in-ride, remove them from the index
                    spatialIndex.removeDriver(driverId);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean updateDriverLocation(String driverId, Location location) {
        lock.writeLock().lock();
        try {
            Driver driver = drivers.get(driverId);
            if (driver != null) {
                driver.updateLocation(location);

                // Only update spatial index if driver is online
                if (driver.getStatus() == DriverStatus.ONLINE) {
                    return spatialIndex.updateDriverLocation(driverId, location);
                }
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Driver> findNearbyDrivers(Location location, double radiusKm) {
        lock.readLock().lock();
        try {
            List<String> driverIds = spatialIndex.findWithinRadius(location, radiusKm);
            return driverIds.stream()
                    .map(drivers::get)
                    .filter(Objects::nonNull)
                    .filter(Driver::isAvailable)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public Driver getDriver(String driverId) {
        lock.readLock().lock();
        try {
            return drivers.get(driverId);
        } finally {
            lock.readLock().unlock();
        }
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
    private final Map<String, Ride> activeRides;
    private final DriverService driverService;
    private final MatchingStrategy matchingStrategy;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public RideService(DriverService driverService, MatchingStrategy matchingStrategy) {
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
    public Ride requestRide(User user, Location pickup, Location dropoff) {
        String rideId = UUID.randomUUID().toString();
        Ride ride = new Ride(rideId, user, pickup, dropoff);

        lock.writeLock().lock();
        try {
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
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void startRide(String rideId) {
        lock.writeLock().lock();
        try {
            Ride ride = activeRides.get(rideId);
            if (ride != null) {
                ride.startRide();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void completeRide(String rideId) {
        lock.writeLock().lock();
        try {
            Ride ride = activeRides.get(rideId);
            if (ride != null) {
                ride.completeRide();
                activeRides.remove(rideId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void cancelRide(String rideId) {
        lock.writeLock().lock();
        try {
            Ride ride = activeRides.get(rideId);
            if (ride != null) {
                ride.cancelRide();
                activeRides.remove(rideId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Ride getRide(String rideId) {
        lock.readLock().lock();
        try {
            return activeRides.get(rideId);
        } finally {
            lock.readLock().unlock();
        }
    }
}

// Main class to demonstrate the system
public class OnlineCabBookingSystem {
    public static void main(String[] args) {
        // Initialize spatial index with a bounding box that covers the operation area
        BoundingBox operationArea = new BoundingBox(-90, -180, 90, 180); // Worldwide coverage
        QuadTreeSpatialIndex spatialIndex = new QuadTreeSpatialIndex(operationArea);

        // Initialize services
        DriverService driverService = new DriverService(spatialIndex);
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
            System.out.println("Assigned driver: " + ride.getDriver().getName());

            // Simulate ride progress by adding a delay
            try {
                Thread.sleep(1000); // Simulate 1 minute ride duration
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Start the ride only if a driver was assigned
            rideService.startRide(ride.getRideId());
            System.out.println("Ride status after starting: " + ride.getStatus());

            // Complete the ride
            rideService.completeRide(ride.getRideId());
            System.out.println("Ride status after completion: " + ride.getStatus());
            System.out.println("Ride fare: ₹" + String.format("%.2f", ride.getFare()));
        } else {
            System.out.println("Ride could not be assigned to a driver. It was likely cancelled automatically.");
        }
    }
}