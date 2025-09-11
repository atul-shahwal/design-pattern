package org.concurrency.questions.movieticketbooking.interview;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 🎯 Problem Statement: Low-Level Design - Movie Ticket Booking System
 *
 * Design a system for booking movie tickets that manages theatres, screens, shows, and seats.
 * It must ensure seat availability, prevent conflicts during concurrent bookings, and handle payments securely.
 *
 * ✅ Requirements:
 * - Manage multiple theatres, screens, and shows.
 * - Allow users to select and lock seats before payment.
 * - Confirm bookings only after successful payment.
 * - Support multiple payment methods.
 *
 * 📦 Key Components:
 * - Movie, Theatre, Screen, Show, Seat, Booking domain models.
 * - SeatLockProvider for seat locking logic.
 * - BookingService for managing booking lifecycle.
 * - PaymentService with various payment strategies.
 *
 * 🚀 Example Flow:
 * 1. User selects a show and seats.
 * 2. Seats are locked and booking is created.
 * 3. Payment is processed, and booking confirmed upon success.
 * 4. Locks are released after confirmation or failure.
 */

// Booking status to track state of booking
enum BookingStatus { CREATED, CONFIRMED, EXPIRED }

// Seat categories for different pricing or priority
enum SeatCategory { SILVER, GOLD, PLATINUM }

// Seat status to track availability
enum SeatStatus { VACANT, BLOCKED, BOOKED }

/**
 * 📦 Movie Class
 * ❓ Interview Q&A:
 * Q: Why are Movie fields immutable and final?
 * A: It ensures thread-safety since the state can't be changed after creation, preventing race conditions.
 */
class Movie {
    private final int movieId;
    private final String movieName;
    private final int durationInMinutes;

    public Movie(int movieId, String movieName, int durationInMinutes) {
        this.movieId = movieId;
        this.movieName = movieName;
        this.durationInMinutes = durationInMinutes;
    }

    public int getMovieId() { return movieId; }
    public String getMovieName() { return movieName; }
    public int getDuration() { return durationInMinutes; }
}

/**
 * 📦 Seat Class
 * ❓ Interview Q&A:
 * Q: Why override equals and hashCode using seatId only?
 * A: Seats are uniquely identified by seatId. It helps in lock management and collection operations.
 */
class Seat {
    private final int seatId;
    private final int row;
    private final SeatCategory category;

    public Seat(int seatId, int row, SeatCategory category) {
        this.seatId = seatId;
        this.row = row;
        this.category = category;
    }

    public int getSeatId() { return seatId; }
    public int getRow() { return row; }
    public SeatCategory getCategory() { return category; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Seat seat = (Seat) o;
        return seatId == seat.seatId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(seatId);
    }

    @Override
    public String toString() {
        return "Seat{" + "seatId=" + seatId + ", row=" + row + ", category=" + category + '}';
    }
}

/**
 * 📦 Screen Class
 * ❓ Interview Q&A:
 * Q: Why not make the list of seats immutable?
 * A: Seats are configured during setup. After initialization, modifications are avoided to prevent concurrency issues.
 */
class Screen {
    private final int screenId;
    private final String name;
    private final List<Seat> seats;

    public Screen(int screenId, String name) {
        this.screenId = screenId;
        this.name = name;
        this.seats = new ArrayList<>();
    }

    public void addSeat(Seat seat) { seats.add(seat); }
    public int getScreenId() { return screenId; }
    public List<Seat> getSeats() { return seats; }
}

/**
 * 📦 Theatre Class
 * ❓ Interview Q&A:
 * Q: Why allow screens to be added after the theatre is created?
 * A: It simulates real-world scenarios where theatres are set up dynamically and screens are configured accordingly.
 */
class Theatre {
    private final int theatreId;
    private final String name;
    private final List<Screen> screens;

    public Theatre(int theatreId, String name) {
        this.theatreId = theatreId;
        this.name = name;
        this.screens = new ArrayList<>();
    }

    public void addScreen(Screen screen) { screens.add(screen); }
    public List<Screen> getScreens() { return screens; }
    public int getTheatreId() { return theatreId; }
    public String getName() { return name; }
}

/**
 * 📦 Show Class
 * ❓ Interview Q&A:
 * Q: Why track seat availability inside Show?
 * A: Seat availability is specific to a particular show, so the state is maintained within the show instance.
 */
class Show {
    private final int showId;
    private final Movie movie;
    private final Screen screen;
    private final Date startTime;
    private final int duration;
    private final Map<Seat, SeatStatus> seatStatusMap = new ConcurrentHashMap<>();

    public Show(int showId, Movie movie, Screen screen, Date startTime, int duration) {
        this.showId = showId;
        this.movie = movie;
        this.screen = screen;
        this.startTime = startTime;
        this.duration = duration;

        for (Seat seat : screen.getSeats()) {
            seatStatusMap.put(seat, SeatStatus.VACANT);
        }
    }

    public int getShowId() { return showId; }
    public Movie getMovie() { return movie; }
    public Screen getScreen() { return screen; }
    public Date getStartTime() { return startTime; }
    public Map<Seat, SeatStatus> getSeatStatusMap() { return seatStatusMap; }
}

/**
 * 📦 User Class
 * ❓ Interview Q&A:
 * Q: Why make user fields immutable?
 * A: It ensures that user data can't be changed during booking operations, supporting thread-safety.
 */
class User {
    private final String name;
    private final String email;

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }

    @Override
    public String toString() {
        return "User{" + "name='" + name + '\'' + ", email='" + email + '\'' + '}';
    }
}

/**
 * 📦 Booking Class
 * ❓ Interview Q&A:
 * Q: Why is status mutable while other fields are final?
 * A: The booking’s status changes over time while other fields identify the booking and remain constant.
 */
class Booking {
    private final String bookingId;
    private final Show show;
    private final List<Seat> seats;
    private final User user;
    private BookingStatus status;

    public Booking(String bookingId, Show show, User user, List<Seat> seats) {
        this.bookingId = bookingId;
        this.show = show;
        this.user = user;
        this.seats = seats;
        this.status = BookingStatus.CREATED;
    }

    public synchronized void confirm() { status = BookingStatus.CONFIRMED; }
    public synchronized void expire() { status = BookingStatus.EXPIRED; }
    public synchronized boolean isConfirmed() { return status == BookingStatus.CONFIRMED; }
    public String getBookingId() { return bookingId; }
    public Show getShow() { return show; }
    public List<Seat> getSeats() { return seats; }
    public User getUser() { return user; }
    public BookingStatus getStatus() { return status; }

    @Override
    public String toString() {
        return "Booking{" + "bookingId='" + bookingId + '\'' + ", show=" + show.getShowId() +
                ", seats=" + seats.stream().map(Seat::getSeatId).collect(Collectors.toList()) +
                ", user=" + user.getName() + ", status=" + status + '}';
    }
}

/**
 * 📦 SeatLockProvider Interface
 * ❓ Interview Q&A:
 * Q: Why use an interface instead of a concrete implementation?
 * A: It allows multiple implementations like in-memory, database-backed, or distributed locks without changing business logic.
 */
interface SeatLockProvider {
    void lockSeats(Show show, List<Seat> seats, User user);
    void unlockSeats(Show show, List<Seat> seats, User user);
    boolean validateLock(Show show, Seat seat, User user);
}

/**
 * 🛡 InMemorySeatLock Class
 * ❓ Interview Q&A:
 * Q: Why is synchronization needed when using ConcurrentHashMap?
 * A: ConcurrentHashMap handles individual operations but not multi-step atomicity. Synchronization ensures consistency across reads and writes.
 */
class InMemorySeatLock implements SeatLockProvider {
    private final int timeout;
    private final Map<String, Map<Seat, Date>> locks = new ConcurrentHashMap<>();

    public InMemorySeatLock(int timeout) { this.timeout = timeout; }

    private String getLockKey(Show show) { return String.valueOf(show.getShowId()); }

    @Override
    public synchronized void lockSeats(Show show, List<Seat> seats, User user) {
        String key = getLockKey(show);
        locks.putIfAbsent(key, new ConcurrentHashMap<>());
        Map<Seat, Date> showLocks = locks.get(key);

        for (Seat seat : seats) {
            SeatStatus status = show.getSeatStatusMap().get(seat);
            if (status == SeatStatus.BLOCKED || status == SeatStatus.BOOKED) {
                throw new RuntimeException("Seat cannot be locked: " + seat.getSeatId());
            }
            show.getSeatStatusMap().put(seat, SeatStatus.BLOCKED);
            showLocks.put(seat, new Date());
        }
    }

    @Override
    public synchronized void unlockSeats(Show show, List<Seat> seats, User user) {
        String key = getLockKey(show);
        if (!locks.containsKey(key)) return;

        Map<Seat, Date> showLocks = locks.get(key);
        for (Seat seat : seats) {
            showLocks.remove(seat);
            if (show.getSeatStatusMap().get(seat) == SeatStatus.BLOCKED) {
                show.getSeatStatusMap().put(seat, SeatStatus.VACANT);
            }
        }
    }

    @Override
    public synchronized boolean validateLock(Show show, Seat seat, User user) {
        String key = getLockKey(show);
        if (!locks.containsKey(key)) return false;
        Map<Seat, Date> showLocks = locks.get(key);
        SeatStatus status = show.getSeatStatusMap().get(seat);
        return showLocks.containsKey(seat) && !isLockExpired(showLocks.get(seat)) && status == SeatStatus.BLOCKED;
    }

    private boolean isLockExpired(Date lockTime) {
        return new Date().getTime() - lockTime.getTime() > timeout;
    }
}

/**
 * 📦 PaymentStrategy Interface
 * ❓ Interview Q&A:
 * Q: Why use strategy pattern for payments?
 * A: It enables adding new payment methods without modifying existing business logic, adhering to the Open/Closed Principle.
 */
interface PaymentStrategy {
    boolean pay(Booking booking, double amount);
}

/**
 * 🏦 CreditCardPayment Class
 * ❓ Interview Q&A:
 * Q: Why simulate payments here?
 * A: For simplicity in an interview scenario, payments are mocked instead of integrating with external APIs.
 */
class CreditCardPayment implements PaymentStrategy {
    @Override
    public boolean pay(Booking booking, double amount) {
        System.out.println("Processing credit card payment of " + amount + " for " + booking.getUser().getName());
        return true;
    }
}

/**
 * 📦 BookingService Class
 * ❓ Interview Q&A:
 * Q: Why are methods synchronized?
 * A: To ensure thread safety during seat locking and status changes, preventing race conditions in concurrent bookings.
 */
class BookingService {
    private final SeatLockProvider seatLockProvider;

    public BookingService(SeatLockProvider seatLockProvider) {
        this.seatLockProvider = seatLockProvider;
    }

    public synchronized Booking createBooking(Show show, User user, List<Seat> seats) {
        seatLockProvider.lockSeats(show, seats, user);
        return new Booking(UUID.randomUUID().toString(), show, user, seats);
    }

    public synchronized boolean processPayment(Booking booking, PaymentStrategy paymentStrategy, double amount) {
        if (booking.isConfirmed()) return false;
        boolean success = paymentStrategy.pay(booking, amount);
        if (success) {
            booking.confirm();
            for (Seat seat : booking.getSeats()) {
                booking.getShow().getSeatStatusMap().put(seat, SeatStatus.BOOKED);
            }
            seatLockProvider.unlockSeats(booking.getShow(), booking.getSeats(), booking.getUser());
        } else {
            booking.expire();
            seatLockProvider.unlockSeats(booking.getShow(), booking.getSeats(), booking.getUser());
        }
        return success;
    }
}

/**
 * 🎬 MovieTicketBookingDemo Class
 * ❓ Interview Q&A:
 * Q: Why is the demo structured with sequential booking and timeout?
 * A: It demonstrates how the system handles concurrent access, lock expiration, and booking confirmation flows effectively.
 */
public class MovieTicketBookingDemo {
    public static void main(String[] args) {
        SeatLockProvider seatLockProvider = new InMemorySeatLock(3000);
        BookingService bookingService = new BookingService(seatLockProvider);

        Movie movie = new Movie(1, "Avengers", 180);
        Screen screen = new Screen(1, "Screen 1");
        Theatre theatre = new Theatre(1, "Cineplex");
        theatre.addScreen(screen);

        for (int i = 1; i <= 10; i++) {
            screen.addSeat(new Seat(i, i / 5 + 1, SeatCategory.SILVER));
        }

        Show show = new Show(1, movie, screen, new Date(), 180);
        User user1 = new User("John Doe", "john@example.com");
        User user2 = new User("Alice Smith", "alice@example.com");

        List<Seat> seats = screen.getSeats().subList(0, 3);

        Booking booking1 = bookingService.createBooking(show, user1, seats);
        System.out.println("Created Booking1: " + booking1);

        try {
            Booking booking2 = bookingService.createBooking(show, user2, seats);
            System.out.println("Created Booking2: " + booking2);
        } catch (RuntimeException e) {
            System.out.println("Booking failed for User2 due to lock: " + e.getMessage());
        }

        boolean paymentResult = bookingService.processPayment(booking1, new CreditCardPayment(), 300);
        if (paymentResult) {
            System.out.println("Payment successful for Booking1.");
        } else {
            System.out.println("Payment failed for Booking1.");
        }

        System.out.println("\n=== Waiting for lock expiration ===");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("\n=== Second Booking Attempt After Timeout ===");
        try {
            Booking booking3 = bookingService.createBooking(show, user2, seats);
            System.out.println("Created Booking3: " + booking3);
            boolean paymentResult2 = bookingService.processPayment(booking3, new CreditCardPayment(), 300);
            if (paymentResult2) {
                System.out.println("Payment successful for Booking3.");
            } else {
                System.out.println("Payment failed for Booking3.");
            }
        } catch (RuntimeException e) {
            System.out.println("Booking failed for User2 after timeout: " + e.getMessage());
        }
    }
}
