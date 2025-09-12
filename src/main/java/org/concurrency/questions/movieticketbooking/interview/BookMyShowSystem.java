package org.concurrency.questions.movieticketbooking.interview;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 1. ENUMS: Define fixed states and types (SeatStatus, BookingStatus, SeatCategory, LANGUAGE, MovieStatus.)
 * 2. ENTITIES: Core domain objects (User, Seat, Theatre, Screen, Movie, Show, Booking)
 * 3. REPOSITORIES: Data access layer (ShowRepository, BookingRepository)
 * 4. PAYMENT SERVICE: Payment processing with Strategy pattern
 * 5. SERVICES: Business logic layer (ShowService, BookingService)
 *
 * Future AIs ->
 * 1. UserService & UserRepository
 * - Manage user registration, authentication, and profile updates
 * - Separation ensures single responsibility and easier maintenance
 * - DB: CockroachDB for structured, distributed, and strongly consistent data; Redis for caching sessions
 *
 * 2. TheatreService & TheatreRepository
 * - Handle theatres, screens, and seats configuration
 * - Keeps administrative data separate from booking logic
 * - DB: CockroachDB for distributed relational data with high availability; Elasticsearch for fast search queries
 *
 * 3. ShowService
 * - Manage shows, schedules, and theatre allocations
 * - DB: CockroachDB ensures transactional integrity across distributed nodes with low latency
 *
 * 4. BookingService & BookingRepository
 * - Handle seat reservation and booking state changes
 * - Requires strong consistency to avoid double booking under concurrency
 * - DB: CockroachDB for ACID-compliant transactions ensuring correctness even at scale
 *
 * 5. PaymentService
 * - Process payments securely and reliably
 * - DB: Depends on audit and reporting needs; CockroachDB if consistency is needed, or another store if performance is prioritized
 *
 * 6. Microservice Decomposition
 * - Split into services: User, Theatre, Show, Booking, Payment
 * - Enables independent scaling, fault isolation, and faster deployments
 * - Use API Gateway, event-driven architecture, monitoring tools, and service mesh patterns
 * - DB per service based on workload and consistency requirements:
 *   • UserService → CockroachDB + (Redis)
 *   • TheatreService → CockroachDB + Elasticsearch + (Redis)
 *   • ShowService → CockroachDB + (Redis)
 *   • BookingService → CockroachDB (preferred for consistency and fault tolerance)
 *   • PaymentService → CockroachDB or other store depending on audit requirements
 *    * 🟠 Trade-offs:
 *  *  • CockroachDB → Slightly higher latency due to distributed consensus, but ensures correctness.
 *  *  • NoSQL → Faster reads/writes, but harder to guarantee seat availability under concurrent booking requests.
 *
 */



enum SeatStatus {VACANT, BOOKED}

enum SeatCategory {NORMAL, EXECUTIVE, PREMIUM, VIP}

enum BookingStatus {CREATED, CONFIRMED, CANCELLED}

enum LANGUAGE {ENGLISH, HINDI}

enum MovieStatus {AVAILABLE, NOT_AVAILABLE}

// ==================== ENTITIES ====================



/**
 * ❓ Why make User class final fields?
 * ✅ Final fields ensure values don't change at time of booking
 */
class User {
    private final Long userId;
    private final String name;
    private final String email;

    public User(Long userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}

/**
 * ❓ Why override equals() and hashCode() in Seat class?
 * ✅ Essential for proper functioning in Hash-based collections (HashMap, HashSet)
 * ✅ Prevents duplicate seats with same ID in collections
 */
class Seat {
    private final Long seatId;
    private final int row;
    private final SeatCategory category;

    public Seat(Long seatId, int row, SeatCategory category) {
        this.seatId = seatId;
        this.row = row;
        this.category = category;
    }

    public Long getSeatId() {
        return seatId;
    }

    public int getRow() {
        return row;
    }

    public SeatCategory getCategory() {
        return category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Seat seat)) return false;
        return Objects.equals(seatId, seat.seatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seatId);
    }
}

/**
 * ❓ Why use composition (List<Seat>) instead of inheritance?
 * ✅ Composition provides better encapsulation and flexibility
 * ✅ Avoids the fragile base class problem in inheritance
 */
class Screen {
    private final Long screenId;
    private final String name;
    private final List<Seat> seats = new ArrayList<>();

    public Screen(Long screenId, String name) {
        this.screenId = screenId;
        this.name = name;
    }

    public void addSeat(Seat seat) {
        seats.add(seat);
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public Long getScreenId() {
        return screenId;
    }
}

class Theatre {
    private final Long theatreId;
    private final String name;
    private final List<Screen> screens = new ArrayList<>();

    public Theatre(Long theatreId, String name) {
        this.theatreId = theatreId;
        this.name = name;
    }

    public void addScreen(Screen screen) {
        screens.add(screen);
    }

    public List<Screen> getScreens() {
        return screens;
    }

    public Long getTheatreId() {
        return theatreId;
    }
}

class Movie {
    private final Long movieId;
    private final String name;
    private final LANGUAGE type;
    private final MovieStatus status;

    public Movie(Long movieId, String name, LANGUAGE type, MovieStatus status) {
        this.movieId = movieId;
        this.name = name;
        this.type = type;
        this.status = status;
    }
}

/**
 * ❓ Why use Set<Theatre> instead of List<Theatre>?
 * ✅ Set prevents duplicate theatres for the same show
 * ✅ Provides faster lookups for contains() operations
 */
class Show {
    private final Long showId;
    private final Movie movie;
    private final Date startTime;
    private final Set<Theatre> theatres = new HashSet<>();

    public Show(Long showId, Movie movie, Date startTime) {
        this.showId = showId;
        this.movie = movie;
        this.startTime = startTime;
    }

    public void addTheatre(Theatre theatre) {
        theatres.add(theatre);
    }

    public Long getShowId() {
        return showId;
    }

    public Set<Theatre> getTheatres() {
        return theatres;
    }
}

// ==================== REPOSITORIES ====================

/**
 * ❓ Why keep BookingStatus as a field instead of subclassing?
 * ✅ State pattern would be overkill for simple status changes
 * ✅ Easier to manage and understand for this use case
 */
class Booking {
    private final Long bookingId;
    private final User user;
    private final Show show;
    private BookingStatus status;

    public Booking(Long bookingId, User user, Show show, Theatre theatre, Screen screen, List<Seat> seats) {
        this.bookingId = bookingId;
        this.user = user;
        this.show = show;
        this.status = BookingStatus.CREATED;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public User getUser() {
        return user;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }
}

/**
 * ❓ Why create repository classes instead of using DB directly?
 * ✅ Abstracts data access logic, making it easier to change implementations
 * ✅ Follows Single Responsibility Principle
 */
class ShowRepository {
    private final Map<Long, Show> shows = new HashMap<>();

    public void saveShow(Show show) {
        shows.put(show.getShowId(), show);
    }

    public Show getShow(Long showId) {
        return shows.get(showId);
    }
}

// ==================== PAYMENT SERVICE ====================

class BookingRepository {
    private final Map<Long, Booking> bookings = new HashMap<>();

    public void saveBooking(Booking booking) {
        bookings.put(booking.getBookingId(), booking);
    }

    public Booking getBooking(Long bookingId) {
        return bookings.get(bookingId);
    }
}

/**
 * ❓ Why use Strategy Pattern for payments?
 * ✅ Allows easy addition of new payment methods without changing existing code
 * ✅ Follows Open/Closed Principle
 */
interface PaymentStrategy {
    boolean pay(Booking booking, double amount);
}

class CreditCardPayment implements PaymentStrategy {
    @Override
    public boolean pay(Booking booking, double amount) {
        System.out.println("Processing credit card payment of " + amount + " for booking " + booking.getBookingId());
        return true;
    }
}

class PaymentService {
    public boolean processPayment(Booking booking, double amount, PaymentStrategy strategy) {
        return strategy.pay(booking, amount);
    }
}

// ==================== SERVICES ====================

class ShowService {
    private final ShowRepository repo;

    public ShowService(ShowRepository repo) {
        this.repo = repo;
    }

    public Show getShow(Long showId) {
        return repo.getShow(showId);
    }

    public void addShow(Show show) {
        repo.saveShow(show);
    }
}

/**
 * ❓ Why use ReentrantLock instead of synchronized?
 * ✅ More flexible - can try to acquire lock, interruptible locking, etc.
 * ✅ Better performance in highly contended scenarios
 */
class BookingService {
    private final BookingRepository bookingRepo;
    private final ShowService showService;
    private final PaymentService paymentService;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * ❓ Why use Map<String, SeatStatus> instead of nested structure?
     * ✅ Using a flat key ensures constant-time lookup and avoids deep traversal across nested objects
     * ✅ Simpler to manage and less prone to concurrency issues
     */
    private final Map<String, SeatStatus> seatStatus = new HashMap<>();

    public BookingService(BookingRepository bookingRepo, ShowService showService, PaymentService paymentService) {
        this.bookingRepo = bookingRepo;
        this.showService = showService;
        this.paymentService = paymentService;
    }

    private String seatKey(Long theatreId, Long screenId, Long seatId) {
        return theatreId + "-" + screenId + "-" + seatId;
    }

    /**
     * ❓ Why process payment before booking and lock the whole method?
     * ✅ Ensures atomicity — seats are only marked booked if payment succeeds, avoiding partial states
     * ✅ Prevents race conditions where payment fails after seats are marked booked
     */
    public Booking bookSeats(User user, Long showId, Long theatreId, Long screenId, List<Long> seatIds, PaymentStrategy strategy, double amount) {
        lock.lock();
        try {
            System.out.println("Started Booking for user : " + user);
            Show show = showService.getShow(showId);
            if (show == null) throw new RuntimeException("Show not found");

            Theatre theatre = null;
            for (Theatre t : show.getTheatres()) {
                if (t.getTheatreId().equals(theatreId)) {
                    theatre = t;
                    break;
                }
            }
            if (theatre == null) throw new RuntimeException("Theatre not found");

            Screen screen = null;
            for (Screen s : theatre.getScreens()) {
                if (s.getScreenId().equals(screenId)) {
                    screen = s;
                    break;
                }
            }
            if (screen == null) throw new RuntimeException("Screen not found");

            List<Seat> seatsToBook = new ArrayList<>();
            for (Seat seat : screen.getSeats()) {
                if (seatIds.contains(seat.getSeatId())) {
                    seatsToBook.add(seat);
                }
            }
            if (seatsToBook.size() != seatIds.size())
                throw new RuntimeException("Some seats not found");

            // Check availability
            for (Seat seat : seatsToBook) {
                String key = seatKey(theatreId, screenId, seat.getSeatId());
                SeatStatus status = seatStatus.getOrDefault(key, SeatStatus.VACANT);
                if (status != SeatStatus.VACANT)
                    throw new RuntimeException("Seat already booked: " + seat.getSeatId());
            }

            // Create booking but process payment first
            Booking booking = new Booking(System.currentTimeMillis(), user, show, theatre, screen, seatsToBook);
            boolean paymentSuccess = paymentService.processPayment(booking, amount, strategy);
            if (!paymentSuccess)
                throw new RuntimeException("Payment failed");

            // Mark seats as booked after successful payment
            for (Seat seat : seatsToBook) {
                String key = seatKey(theatreId, screenId, seat.getSeatId());
                seatStatus.put(key, SeatStatus.BOOKED);
            }

            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepo.saveBooking(booking);
            System.out.println("Booking confirmed for user: " + user.getName());
            return booking;

        } finally {
            lock.unlock();
        }
    }
}

public class BookMyShowSystem {
    // ==================== TEST CASES ====================

    public static void main(String[] args) throws InterruptedException {
        ShowRepository showRepo = new ShowRepository();
        ShowService showService = new ShowService(showRepo);
        BookingRepository bookingRepo = new BookingRepository();
        PaymentService paymentService = new PaymentService();
        BookingService bookingService = new BookingService(bookingRepo, showService, paymentService);

        Theatre theatre = new Theatre(1L, "Cineplex");
        Screen screen = new Screen(1L, "Screen1");
        for (long i = 1; i <= 5; i++)
            screen.addSeat(new Seat(i, 1, SeatCategory.NORMAL));
        theatre.addScreen(screen);

        Movie movie = new Movie(1L, "Avengers", LANGUAGE.ENGLISH, MovieStatus.AVAILABLE);
        Show show = new Show(1L, movie, new Date());
        show.addTheatre(theatre);
        showService.addShow(show);

        User user1 = new User(1L, "John Doe", "john@example.com");
        User user2 = new User(2L, "Jane Roe", "jane@example.com");

        System.out.println("=== Test Case 1: Two simultaneous booking requests for same seat ===");
        bookingService.bookSeats(user1, 1L, 1L, 1L, List.of(1L, 2L), new CreditCardPayment(), 500.0);
        try {
            bookingService.bookSeats(user2, 1L, 1L, 1L, List.of(2L, 3L), new CreditCardPayment(), 500.0);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        Thread.sleep(2000);
        try {
            bookingService.bookSeats(user2, 1L, 1L, 1L, List.of(1L), new CreditCardPayment(), 500.0);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}