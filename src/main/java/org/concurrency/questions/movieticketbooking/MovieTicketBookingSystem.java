package org.concurrency.questions.movieticketbooking;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// Enums
enum BookingStatus { CREATED, CONFIRMED, EXPIRED }
enum SeatCategory { SILVER, GOLD, PLATINUM }

// Domain Classes
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

class Show {
    private final int showId;
    private final Movie movie;
    private final Screen screen;
    private final Date startTime;
    private final int duration;

    public Show(int showId, Movie movie, Screen screen, Date startTime, int duration) {
        this.showId = showId;
        this.movie = movie;
        this.screen = screen;
        this.startTime = startTime;
        this.duration = duration;
    }

    public int getShowId() { return showId; }
    public Movie getMovie() { return movie; }
    public Screen getScreen() { return screen; }
    public Date getStartTime() { return startTime; }
}

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
    public int getTheatreId() { return theatreId; }
    public List<Screen> getScreens() { return screens; }
}

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

    public void confirm() { status = BookingStatus.CONFIRMED; }
    public void expire() { status = BookingStatus.EXPIRED; }
    public boolean isConfirmed() { return status == BookingStatus.CONFIRMED; }
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

// Seat Locking Mechanism
interface SeatLockProvider {
    void lockSeats(Show show, List<Seat> seats, User user);
    void unlockSeats(Show show, List<Seat> seats, User user);
    boolean validateLock(Show show, Seat seat, User user);
    List<Seat> getLockedSeats(Show show);
    Map<String, Map<Seat, Date>> getAllLocks(); // For testing
}

class InMemorySeatLock implements SeatLockProvider {
    private final int timeout;
    private final Map<String, Map<Seat, Date>> locks = new ConcurrentHashMap<>();

    public InMemorySeatLock(int timeout) { this.timeout = timeout; }

    private String getLockKey(Show show) {
        return String.valueOf(show.getShowId());
    }

    @Override
    public void lockSeats(Show show, List<Seat> seats, User user) {
        synchronized (this) {
            String key = getLockKey(show);
            locks.putIfAbsent(key, new ConcurrentHashMap<>());
            Map<Seat, Date> showLocks = locks.get(key);

            for (Seat seat : seats) {
                if (showLocks.containsKey(seat) && !isLockExpired(showLocks.get(seat))) {
                    throw new RuntimeException("Seat already locked: " + seat.getSeatId());
                }
                showLocks.put(seat, new Date());
            }
        }
    }

    @Override
    public void unlockSeats(Show show, List<Seat> seats, User user) {
        synchronized (this) {
            String key = getLockKey(show);
            if (!locks.containsKey(key)) return;

            Map<Seat, Date> showLocks = locks.get(key);
            for (Seat seat : seats) {
                showLocks.remove(seat);
            }
        }
    }

    @Override
    public boolean validateLock(Show show, Seat seat, User user) {
        String key = getLockKey(show);
        if (!locks.containsKey(key)) return false;

        Map<Seat, Date> showLocks = locks.get(key);
        return showLocks.containsKey(seat) && !isLockExpired(showLocks.get(seat));
    }

    @Override
    public List<Seat> getLockedSeats(Show show) {
        String key = getLockKey(show);
        if (!locks.containsKey(key)) return new ArrayList<>();

        Map<Seat, Date> showLocks = locks.get(key);
        return showLocks.entrySet().stream()
                .filter(entry -> !isLockExpired(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Map<Seat, Date>> getAllLocks() {
        return locks;
    }

    private boolean isLockExpired(Date lockTime) {
        return System.currentTimeMillis() - lockTime.getTime() > timeout * 1000;
    }
}

// Services
class MovieService {
    private final Map<Integer, Movie> movies = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger();

    public Movie createMovie(String name, int duration) {
        int id = counter.incrementAndGet();
        Movie movie = new Movie(id, name, duration);
        movies.put(id, movie);
        return movie;
    }

    public Movie getMovie(int id) {
        return movies.get(id);
    }
}

class TheatreService {
    private final Map<Integer, Theatre> theatres = new ConcurrentHashMap<>();
    private final Map<Integer, Screen> screens = new ConcurrentHashMap<>();
    private final Map<Integer, Seat> seats = new ConcurrentHashMap<>();
    private final AtomicInteger theatreCounter = new AtomicInteger();
    private final AtomicInteger screenCounter = new AtomicInteger();
    private final AtomicInteger seatCounter = new AtomicInteger();

    public Theatre createTheatre(String name) {
        int id = theatreCounter.incrementAndGet();
        Theatre theatre = new Theatre(id, name);
        theatres.put(id, theatre);
        return theatre;
    }

    public Screen addScreenToTheatre(int theatreId, String screenName) {
        Theatre theatre = theatres.get(theatreId);
        if (theatre == null) throw new RuntimeException("Theatre not found");

        int screenId = screenCounter.incrementAndGet();
        Screen screen = new Screen(screenId, screenName);
        theatre.addScreen(screen);
        screens.put(screenId, screen);
        return screen;
    }

    public Seat addSeatToScreen(int screenId, int row, SeatCategory category) {
        Screen screen = screens.get(screenId);
        if (screen == null) throw new RuntimeException("Screen not found");

        int seatId = seatCounter.incrementAndGet();
        Seat seat = new Seat(seatId, row, category);
        screen.addSeat(seat);
        seats.put(seatId, seat);
        return seat;
    }

    public Screen getScreen(int screenId) {
        return screens.get(screenId);
    }

    public Seat getSeat(int seatId) {
        return seats.get(seatId);
    }

    public List<Seat> getAllSeats() {
        return new ArrayList<>(seats.values());
    }
}

class ShowService {
    private final Map<Integer, Show> shows = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger();

    public Show createShow(Movie movie, Screen screen, Date startTime, int duration) {
        int id = counter.incrementAndGet();
        Show show = new Show(id, movie, screen, startTime, duration);
        shows.put(id, show);
        return show;
    }

    public Show getShow(int id) {
        return shows.get(id);
    }
}

class BookingService {
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();
    private final SeatLockProvider lockProvider;
    private final AtomicInteger counter = new AtomicInteger();

    public BookingService(SeatLockProvider lockProvider) {
        this.lockProvider = lockProvider;
    }

    public Booking createBooking(Show show, User user, List<Seat> seats) {
        // Check seat availability
        List<Seat> unavailable = getUnavailableSeats(show);
        if (seats.stream().anyMatch(unavailable::contains)) {
            throw new RuntimeException("Seats not available");
        }

        // Lock seats
        lockProvider.lockSeats(show, seats, user);

        // Create booking
        String bookingId = "B" + counter.incrementAndGet();
        Booking booking = new Booking(bookingId, show, user, seats);
        bookings.put(bookingId, booking);

        return booking;
    }

    public void confirmBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) throw new RuntimeException("Booking not found");

        for (Seat seat : booking.getSeats()) {
            if (!lockProvider.validateLock(booking.getShow(), seat, booking.getUser())) {
                throw new RuntimeException("Seat lock validation failed");
            }
        }
        booking.confirm();
    }

    public void releaseSeatLocks(Booking booking) {
        lockProvider.unlockSeats(booking.getShow(), booking.getSeats(), booking.getUser());
    }

    public boolean isSeatAvailable(Show show, Seat seat) {
        List<Seat> unavailableSeats = getUnavailableSeats(show);
        return !unavailableSeats.contains(seat);
    }

    private List<Seat> getUnavailableSeats(Show show) {
        return lockProvider.getLockedSeats(show);
    }

    public Booking getBooking(String bookingId) {
        return bookings.get(bookingId);
    }

    public List<Booking> getAllBookings() {
        return new ArrayList<>(bookings.values());
    }
}

// Payment Strategy
interface PaymentStrategy {
    boolean processPayment(double amount);
}

class CreditCardPayment implements PaymentStrategy {
    @Override
    public boolean processPayment(double amount) {
        // Process credit card payment
        System.out.println("Processing credit card payment of $" + amount);
        return true;
    }
}

class PayPalPayment implements PaymentStrategy {
    @Override
    public boolean processPayment(double amount) {
        // Process PayPal payment
        System.out.println("Processing PayPal payment of $" + amount);
        return true;
    }
}

class PaymentService {
    private final PaymentStrategy paymentStrategy;
    private final BookingService bookingService;

    public PaymentService(PaymentStrategy paymentStrategy, BookingService bookingService) {
        this.paymentStrategy = paymentStrategy;
        this.bookingService = bookingService;
    }

    public boolean processPayment(String bookingId, double amount) {
        boolean success = paymentStrategy.processPayment(amount);
        if (success) {
            bookingService.confirmBooking(bookingId);
            // Release the locks after successful payment
            Booking booking = bookingService.getBooking(bookingId);
            if (booking != null) {
                bookingService.releaseSeatLocks(booking);
            }
        }
        return success;
    }
}

// Main System
public class MovieTicketBookingSystem {
    public static void main(String[] args) {
        // Initialize services
        SeatLockProvider lockProvider = new InMemorySeatLock(300); // 5min timeout
        BookingService bookingService = new BookingService(lockProvider);
        PaymentService paymentService = new PaymentService(new CreditCardPayment(), bookingService);

        // Create sample data
        MovieService movieService = new MovieService();
        Movie movie = movieService.createMovie("Inception", 150);

        TheatreService theatreService = new TheatreService();
        Theatre theatre = theatreService.createTheatre("Multiplex");
        Screen screen = theatreService.addScreenToTheatre(theatre.getTheatreId(), "Screen 1");

        // Add seats to screen
        for (int row = 1; row <= 5; row++) {
            SeatCategory category = row == 1 ? SeatCategory.PLATINUM :
                    row <= 3 ? SeatCategory.GOLD : SeatCategory.SILVER;
            for (int seatNum = 1; seatNum <= 10; seatNum++) {
                theatreService.addSeatToScreen(screen.getScreenId(), row, category);
            }
        }

        ShowService showService = new ShowService();
        Show show = showService.createShow(movie, screen, new Date(), 150);

        User user1 = new User("John Doe", "john@example.com");
        User user2 = new User("Jane Smith", "jane@example.com");

        // Test case: Two users trying to book the same seat at the same time
        System.out.println("Testing concurrent seat booking...");

        // Get a specific seat to test with
        Seat targetSeat = screen.getSeats().get(0); // First seat

        // Create threads for concurrent booking attempts
        Thread user1Thread = new Thread(() -> {
            try {
                System.out.println("User 1 attempting to book seat " + targetSeat.getSeatId());

                // Check if seat is available before trying to book
                if (!bookingService.isSeatAvailable(show, targetSeat)) {
                    System.out.println("User 1: Seat " + targetSeat.getSeatId() + " is not available");
                    return;
                }

                Booking booking = bookingService.createBooking(show, user1, Arrays.asList(targetSeat));
                System.out.println("User 1 successfully booked seat " + targetSeat.getSeatId() +
                        ", Booking ID: " + booking.getBookingId());

                // Simulate payment processing time
                Thread.sleep(100);

                boolean paymentSuccess = paymentService.processPayment(booking.getBookingId(), 250.0);
                if (paymentSuccess) {
                    System.out.println("User 1 payment successful");
                } else {
                    System.out.println("User 1 payment failed");
                    // Release locks if payment fails
                    bookingService.releaseSeatLocks(booking);
                }
            } catch (Exception e) {
                System.out.println("User 1 failed to book seat " + targetSeat.getSeatId() +
                        ": " + e.getMessage());
            }
        });

        Thread user2Thread = new Thread(() -> {
            try {
                // Add a small delay to ensure user1 gets the lock first
                Thread.sleep(50);

                System.out.println("User 2 attempting to book seat " + targetSeat.getSeatId());

                // Check if seat is available before trying to book
                if (!bookingService.isSeatAvailable(show, targetSeat)) {
                    System.out.println("User 2: Seat " + targetSeat.getSeatId() + " is not available");
                    return;
                }

                Booking booking = bookingService.createBooking(show, user2, Arrays.asList(targetSeat));
                System.out.println("User 2 successfully booked seat " + targetSeat.getSeatId() +
                        ", Booking ID: " + booking.getBookingId());

                boolean paymentSuccess = paymentService.processPayment(booking.getBookingId(), 250.0);
                if (paymentSuccess) {
                    System.out.println("User 2 payment successful");
                } else {
                    System.out.println("User 2 payment failed");
                    // Release locks if payment fails
                    bookingService.releaseSeatLocks(booking);
                }
            } catch (Exception e) {
                System.out.println("User 2 failed to book seat " + targetSeat.getSeatId() +
                        ": " + e.getMessage());
            }
        });

        // Start both threads
        user1Thread.start();
        user2Thread.start();

        // Wait for both threads to complete
        try {
            user1Thread.join();
            user2Thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check final status of the seat
        List<Seat> lockedSeats = lockProvider.getLockedSeats(show);
        boolean isSeatLocked = lockedSeats.contains(targetSeat);
        System.out.println("Seat " + targetSeat.getSeatId() + " is currently locked: " + isSeatLocked);

        // Check if the seat is available now
        boolean isAvailable = bookingService.isSeatAvailable(show, targetSeat);
        System.out.println("Seat " + targetSeat.getSeatId() + " is available: " + isAvailable);

        // Check all bookings for the show
        System.out.println("\nFinal booking status:");
        List<Booking> allBookings = bookingService.getAllBookings();
        for (Booking booking : allBookings) {
            System.out.println(booking);
        }

        // Check all locks for the show
        System.out.println("\nFinal lock status:");
        Map<String, Map<Seat, Date>> allLocks = lockProvider.getAllLocks();
        for (Map.Entry<String, Map<Seat, Date>> entry : allLocks.entrySet()) {
            System.out.println("Show " + entry.getKey() + " locks: " + entry.getValue());
        }
    }
}