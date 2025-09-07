package org.desingpatterns.questions.elivator;

import java.util.*;

// Enums for direction and elevator state
enum Direction {
    UP, DOWN, IDLE
}

enum ElevatorState {
    IDLE, MOVING, STOPPED, MAINTENANCE
}

// Observer interface
interface ElevatorObserver {
    void onElevatorStateChange(Elevator elevator, ElevatorState state);
    void onElevatorFloorChange(Elevator elevator, int floor);
}

// Scheduling strategy interface
interface SchedulingStrategy {
    int getNextStop(Elevator elevator);
}

// Elevator request class
class ElevatorRequest {
    private int floor;
    private Direction direction;
    private boolean isInternalRequest;

    public ElevatorRequest(int floor, Direction direction, boolean isInternalRequest) {
        this.floor = floor;
        this.direction = direction;
        this.isInternalRequest = isInternalRequest;
    }

    public int getFloor() {
        return floor;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isInternalRequest() {
        return isInternalRequest;
    }

    @Override
    public String toString() {
        return "Floor: " + floor + ", Direction: " + direction +
                ", Type: " + (isInternalRequest ? "Internal" : "External");
    }
}

// FCFS Scheduling Strategy
class FCFSSchedulingStrategy implements SchedulingStrategy {
    @Override
    public int getNextStop(Elevator elevator) {
        Queue<ElevatorRequest> requests = elevator.getRequestsQueue();
        if (requests.isEmpty()) {
            return elevator.getCurrentFloor();
        }

        return requests.peek().getFloor();
    }
}

// Scan Scheduling Strategy
class ScanSchedulingStrategy implements SchedulingStrategy {
    @Override
    public int getNextStop(Elevator elevator) {
        int currentFloor = elevator.getCurrentFloor();
        Direction currentDirection = elevator.getDirection();
        Queue<ElevatorRequest> requests = elevator.getRequestsQueue();

        if (requests.isEmpty()) {
            return currentFloor;
        }

        // If idle, determine direction based on first request
        if (currentDirection == Direction.IDLE) {
            ElevatorRequest firstRequest = requests.peek();
            if (firstRequest.getFloor() > currentFloor) {
                elevator.setDirection(Direction.UP);
                return firstRequest.getFloor();
            } else if (firstRequest.getFloor() < currentFloor) {
                elevator.setDirection(Direction.DOWN);
                return firstRequest.getFloor();
            } else {
                return currentFloor;
            }
        }

        // If moving, find the next request in the current direction
        if (currentDirection == Direction.UP) {
            int nextStop = Integer.MAX_VALUE;
            for (ElevatorRequest request : requests) {
                if (request.getFloor() >= currentFloor && request.getFloor() < nextStop) {
                    nextStop = request.getFloor();
                }
            }

            if (nextStop != Integer.MAX_VALUE) {
                return nextStop;
            } else {
                // No requests above, change direction
                elevator.setDirection(Direction.DOWN);
                return getNextStop(elevator);
            }
        } else { // DOWN direction
            int nextStop = Integer.MIN_VALUE;
            for (ElevatorRequest request : requests) {
                if (request.getFloor() <= currentFloor && request.getFloor() > nextStop) {
                    nextStop = request.getFloor();
                }
            }

            if (nextStop != Integer.MIN_VALUE) {
                return nextStop;
            } else {
                // No requests below, change direction
                elevator.setDirection(Direction.UP);
                return getNextStop(elevator);
            }
        }
    }
}

// Look Scheduling Strategy
class LookSchedulingStrategy implements SchedulingStrategy {
    @Override
    public int getNextStop(Elevator elevator) {
        int currentFloor = elevator.getCurrentFloor();
        Direction currentDirection = elevator.getDirection();
        Queue<ElevatorRequest> requests = elevator.getRequestsQueue();

        if (requests.isEmpty()) {
            return currentFloor;
        }

        // If idle, determine direction based on closest request
        if (currentDirection == Direction.IDLE) {
            int closestFloor = currentFloor;
            int minDistance = Integer.MAX_VALUE;

            for (ElevatorRequest request : requests) {
                int distance = Math.abs(request.getFloor() - currentFloor);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestFloor = request.getFloor();
                }
            }

            if (closestFloor > currentFloor) {
                elevator.setDirection(Direction.UP);
            } else if (closestFloor < currentFloor) {
                elevator.setDirection(Direction.DOWN);
            }

            return closestFloor;
        }

        // If moving, find the next request in the current direction
        if (currentDirection == Direction.UP) {
            int nextStop = Integer.MAX_VALUE;
            for (ElevatorRequest request : requests) {
                if (request.getFloor() >= currentFloor && request.getFloor() < nextStop) {
                    nextStop = request.getFloor();
                }
            }

            if (nextStop != Integer.MAX_VALUE) {
                return nextStop;
            } else {
                // No requests above, look for requests below
                int highestBelow = Integer.MIN_VALUE;
                for (ElevatorRequest request : requests) {
                    if (request.getFloor() <= currentFloor && request.getFloor() > highestBelow) {
                        highestBelow = request.getFloor();
                    }
                }

                if (highestBelow != Integer.MIN_VALUE) {
                    elevator.setDirection(Direction.DOWN);
                    return highestBelow;
                }
            }
        } else { // DOWN direction
            int nextStop = Integer.MIN_VALUE;
            for (ElevatorRequest request : requests) {
                if (request.getFloor() <= currentFloor && request.getFloor() > nextStop) {
                    nextStop = request.getFloor();
                }
            }

            if (nextStop != Integer.MIN_VALUE) {
                return nextStop;
            } else {
                // No requests below, look for requests above
                int lowestAbove = Integer.MAX_VALUE;
                for (ElevatorRequest request : requests) {
                    if (request.getFloor() >= currentFloor && request.getFloor() < lowestAbove) {
                        lowestAbove = request.getFloor();
                    }
                }

                if (lowestAbove != Integer.MAX_VALUE) {
                    elevator.setDirection(Direction.UP);
                    return lowestAbove;
                }
            }
        }

        return currentFloor;
    }
}

// Elevator class
class Elevator {
    private int id;
    private int currentFloor;
    private Direction direction;
    private ElevatorState state;
    private List<ElevatorObserver> observers;
    private Queue<ElevatorRequest> requests;
    private int capacity;
    private int currentLoad;
    private boolean emergencyStop;
    private boolean doorObstructed;

    public Elevator(int id, int capacity) {
        this.id = id;
        this.currentFloor = 0; // Ground floor
        this.direction = Direction.IDLE;
        this.state = ElevatorState.IDLE;
        this.observers = new ArrayList<>();
        this.requests = new LinkedList<>();
        this.capacity = capacity;
        this.currentLoad = 0;
        this.emergencyStop = false;
        this.doorObstructed = false;
    }

    public void addObserver(ElevatorObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(ElevatorObserver observer) {
        observers.remove(observer);
    }

    private void notifyStateChange(ElevatorState state) {
        for (ElevatorObserver observer : observers) {
            observer.onElevatorStateChange(this, state);
        }
    }

    private void notifyFloorChange(int floor) {
        for (ElevatorObserver observer : observers) {
            observer.onElevatorFloorChange(this, floor);
        }
    }

    public void setState(ElevatorState newState) {
        this.state = newState;
        notifyStateChange(newState);
    }

    public void setDirection(Direction newDirection) {
        this.direction = newDirection;
    }

    public void addRequest(ElevatorRequest request) {
        if (!requests.contains(request)) {
            requests.add(request);

            if (state == ElevatorState.IDLE) {
                int requestedFloor = request.getFloor();
                if (requestedFloor > currentFloor) {
                    direction = Direction.UP;
                } else if (requestedFloor < currentFloor) {
                    direction = Direction.DOWN;
                }
                setState(ElevatorState.MOVING);
            }
        }
    }

    public void moveToNextStop(int nextStop) {
        if (state != ElevatorState.MOVING || emergencyStop) {
            return;
        }

        while (currentFloor != nextStop) {
            try {
                Thread.sleep(1000); // Simulate movement between floors
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (direction == Direction.UP) {
                currentFloor++;
            } else {
                currentFloor--;
            }

            notifyFloorChange(currentFloor);

            if (currentFloor == nextStop) {
                completeArrival();
                return;
            }
        }
    }

    private void completeArrival() {
        setState(ElevatorState.STOPPED);

        // Remove all requests for this floor
        Iterator<ElevatorRequest> iterator = requests.iterator();
        while (iterator.hasNext()) {
            ElevatorRequest request = iterator.next();
            if (request.getFloor() == currentFloor) {
                iterator.remove();
            }
        }

        // Open doors, wait, then close doors
        try {
            Thread.sleep(2000); // Simulate doors opening and closing
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (requests.isEmpty()) {
            direction = Direction.IDLE;
            setState(ElevatorState.IDLE);
        } else {
            setState(ElevatorState.MOVING);
        }
    }

    public void emergencyStop() {
        emergencyStop = true;
        setState(ElevatorState.STOPPED);
    }

    public void resumeFromEmergency() {
        emergencyStop = false;
        if (!requests.isEmpty()) {
            setState(ElevatorState.MOVING);
        } else {
            setState(ElevatorState.IDLE);
        }
    }

    public boolean addLoad(int weight) {
        if (currentLoad + weight > capacity) {
            return false;
        }
        currentLoad += weight;
        return true;
    }

    public void removeLoad(int weight) {
        currentLoad = Math.max(0, currentLoad - weight);
    }

    public void setDoorObstructed(boolean obstructed) {
        this.doorObstructed = obstructed;
    }

    // Getters
    public int getId() { return id; }
    public int getCurrentFloor() { return currentFloor; }
    public Direction getDirection() { return direction; }
    public ElevatorState getState() { return state; }
    public Queue<ElevatorRequest> getRequestsQueue() { return new LinkedList<>(requests); }
    public int getCurrentLoad() { return currentLoad; }
    public int getCapacity() { return capacity; }
    public boolean isEmergencyStop() { return emergencyStop; }
    public boolean isDoorObstructed() { return doorObstructed; }
}

// Floor class
class Floor {
    private int floorNumber;

    public Floor(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public int getFloorNumber() {
        return floorNumber;
    }
}

// Elevator Controller class
class ElevatorController {
    private List<Elevator> elevators;
    private List<Floor> floors;
    private SchedulingStrategy schedulingStrategy;

    public ElevatorController(int numberOfElevators, int numberOfFloors, int elevatorCapacity) {
        this.elevators = new ArrayList<>();
        this.floors = new ArrayList<>();
        this.schedulingStrategy = new LookSchedulingStrategy(); // Default strategy

        // Initialize elevators
        for (int i = 0; i < numberOfElevators; i++) {
            elevators.add(new Elevator(i + 1, elevatorCapacity));
        }

        // Initialize floors
        for (int i = 0; i < numberOfFloors; i++) {
            floors.add(new Floor(i));
        }
    }

    public void setSchedulingStrategy(SchedulingStrategy strategy) {
        this.schedulingStrategy = strategy;
    }

    public void requestElevator(int floor, Direction direction) {
        // Find the best elevator for this request
        Elevator bestElevator = findBestElevator(floor, direction);
        if (bestElevator != null) {
            bestElevator.addRequest(new ElevatorRequest(floor, direction, false));
            System.out.println("Assigned elevator " + bestElevator.getId() + " to floor " + floor);
        } else {
            System.out.println("No elevator available for floor " + floor);
        }
    }

    public void requestFloor(int elevatorId, int floor) {
        Elevator elevator = getElevatorById(elevatorId);
        if (elevator != null) {
            Direction direction = floor > elevator.getCurrentFloor() ? Direction.UP : Direction.DOWN;
            elevator.addRequest(new ElevatorRequest(floor, direction, true));
            System.out.println("Added floor request " + floor + " to elevator " + elevatorId);
        }
    }

    private Elevator findBestElevator(int floor, Direction direction) {
        // Simple algorithm: find the closest idle elevator or one moving in the same direction
        Elevator bestElevator = null;
        int minDistance = Integer.MAX_VALUE;

        for (Elevator elevator : elevators) {
            if (elevator.getState() == ElevatorState.MAINTENANCE || elevator.isEmergencyStop()) {
                continue;
            }

            int distance = Math.abs(elevator.getCurrentFloor() - floor);

            // Prefer idle elevators
            if (elevator.getState() == ElevatorState.IDLE) {
                if (distance < minDistance) {
                    minDistance = distance;
                    bestElevator = elevator;
                }
            }
            // Then consider elevators moving in the same direction
            else if (elevator.getDirection() == direction) {
                if ((direction == Direction.UP && elevator.getCurrentFloor() <= floor) ||
                        (direction == Direction.DOWN && elevator.getCurrentFloor() >= floor)) {
                    if (distance < minDistance) {
                        minDistance = distance;
                        bestElevator = elevator;
                    }
                }
            }
        }

        return bestElevator;
    }

    public void step() {
        for (Elevator elevator : elevators) {
            if (elevator.getState() == ElevatorState.MOVING && !elevator.getRequestsQueue().isEmpty()) {
                int nextStop = schedulingStrategy.getNextStop(elevator);
                elevator.moveToNextStop(nextStop);
            }
        }
    }

    public void emergencyStop(int elevatorId) {
        Elevator elevator = getElevatorById(elevatorId);
        if (elevator != null) {
            elevator.emergencyStop();
        }
    }

    public void resumeEmergency(int elevatorId) {
        Elevator elevator = getElevatorById(elevatorId);
        if (elevator != null) {
            elevator.resumeFromEmergency();
        }
    }

    public void setMaintenanceMode(int elevatorId, boolean maintenance) {
        Elevator elevator = getElevatorById(elevatorId);
        if (elevator != null) {
            elevator.setState(maintenance ? ElevatorState.MAINTENANCE : ElevatorState.IDLE);
        }
    }

    private Elevator getElevatorById(int id) {
        for (Elevator elevator : elevators) {
            if (elevator.getId() == id) {
                return elevator;
            }
        }
        return null;
    }

    public List<Elevator> getElevators() {
        return elevators;
    }

    public List<Floor> getFloors() {
        return floors;
    }
}

// Elevator Display class
class ElevatorDisplay implements ElevatorObserver {
    @Override
    public void onElevatorStateChange(Elevator elevator, ElevatorState state) {
        System.out.println("Elevator " + elevator.getId() + " state changed to: " + state);
    }

    @Override
    public void onElevatorFloorChange(Elevator elevator, int floor) {
        System.out.println("Elevator " + elevator.getId() + " is now at floor: " + floor);
    }
}

// Building class
class Building {
    private String name;
    private int numberOfFloors;
    private ElevatorController elevatorController;

    public Building(String name, int numberOfFloors, int numberOfElevators, int elevatorCapacity) {
        this.name = name;
        this.numberOfFloors = numberOfFloors;
        this.elevatorController = new ElevatorController(numberOfElevators, numberOfFloors, elevatorCapacity);
    }

    public String getName() {
        return name;
    }

    public int getNumberOfFloors() {
        return numberOfFloors;
    }

    public ElevatorController getElevatorController() {
        return elevatorController;
    }
}

// Main class
public class ElevatorSystem {
    public static void main(String[] args) {
        // Initialize building with 10 floors and 3 elevators with capacity of 10 people
        Building building = new Building("Office Tower", 10, 3, 10);
        ElevatorController controller = building.getElevatorController();

        // Create display and add as observer to all elevators
        ElevatorDisplay display = new ElevatorDisplay();
        for (Elevator elevator : controller.getElevators()) {
            elevator.addObserver(display);
        }

        // Simulate elevator requests
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("Elevator System Simulation");
        System.out.println("Building: " + building.getName());
        System.out.println("Floors: " + building.getNumberOfFloors());
        System.out.println("Elevators: " + controller.getElevators().size());

        while (running) {
            System.out.println("\nSelect an option:");
            System.out.println("1. Request elevator (external)");
            System.out.println("2. Request floor (internal)");
            System.out.println("3. Simulate next step");
            System.out.println("4. Change scheduling strategy");
            System.out.println("5. Emergency stop");
            System.out.println("6. Resume from emergency");
            System.out.println("7. Set maintenance mode");
            System.out.println("8. Exit simulation");

            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    System.out.print("Enter floor number: ");
                    int floor = scanner.nextInt();
                    System.out.print("Direction (1 for UP, 2 for DOWN): ");
                    int dirChoice = scanner.nextInt();
                    Direction dir = dirChoice == 1 ? Direction.UP : Direction.DOWN;
                    controller.requestElevator(floor, dir);
                    break;

                case 2:
                    System.out.print("Enter elevator ID: ");
                    int elevatorId = scanner.nextInt();
                    System.out.print("Enter destination floor: ");
                    int destFloor = scanner.nextInt();
                    controller.requestFloor(elevatorId, destFloor);
                    break;

                case 3:
                    System.out.println("Simulating next step...");
                    controller.step();
                    displayElevatorStatus(controller.getElevators());
                    break;

                case 4:
                    System.out.println("Select strategy:");
                    System.out.println("1. FCFS Algorithm");
                    System.out.println("2. SCAN Algorithm");
                    System.out.println("3. LOOK Algorithm");
                    int strategyChoice = scanner.nextInt();

                    if (strategyChoice == 1) {
                        controller.setSchedulingStrategy(new FCFSSchedulingStrategy());
                        System.out.println("Strategy set to FCFS");
                    } else if (strategyChoice == 2) {
                        controller.setSchedulingStrategy(new ScanSchedulingStrategy());
                        System.out.println("Strategy set to SCAN");
                    } else {
                        controller.setSchedulingStrategy(new LookSchedulingStrategy());
                        System.out.println("Strategy set to LOOK");
                    }
                    break;

                case 5:
                    System.out.print("Enter elevator ID for emergency stop: ");
                    int emergencyId = scanner.nextInt();
                    controller.emergencyStop(emergencyId);
                    break;

                case 6:
                    System.out.print("Enter elevator ID to resume: ");
                    int resumeId = scanner.nextInt();
                    controller.resumeEmergency(resumeId);
                    break;

                case 7:
                    System.out.print("Enter elevator ID: ");
                    int maintenanceId = scanner.nextInt();
                    System.out.print("Set maintenance mode (true/false): ");
                    boolean maintenance = scanner.nextBoolean();
                    controller.setMaintenanceMode(maintenanceId, maintenance);
                    break;

                case 8:
                    running = false;
                    break;

                default:
                    System.out.println("Invalid choice!");
            }
        }

        scanner.close();
        System.out.println("Simulation ended");
    }

    private static void displayElevatorStatus(List<Elevator> elevators) {
        System.out.println("\nElevator Status:");
        for (Elevator elevator : elevators) {
            System.out.println("Elevator " + elevator.getId() +
                    ": Floor " + elevator.getCurrentFloor() +
                    ", Direction " + elevator.getDirection() +
                    ", State " + elevator.getState() +
                    ", Load " + elevator.getCurrentLoad() + "/" + elevator.getCapacity() +
                    ", Requests: " + elevator.getRequestsQueue().size());
        }
    }
}