import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

/**
 * The "brain" of the program: all the data and all the data structures are here.
 * The screens (MovieApp) only ask this class to do the work.
 *
 *   ArrayList  - movies, dates, showtimes, seats of a booking
 *   HashMap    - usernames/passwords, and the seat map of each movie showtime
 *   Stack      - seats the user picked (the last seat picked can be undone)
 *   Queue      - payment queue (first booking in is the first one processed)
 *   LinkedList - booking history (newest booking is added at the front)
 */
public class BookingManager {
    public static final int ROWS = 6;   // seat rows A to F
    public static final int COLS = 8;   // seats 1 to 8

    private ArrayList<Movie> movies = new ArrayList<>();
    private ArrayList<String> dates = new ArrayList<>(Arrays.asList("Today", "Tomorrow", "In 2 days"));
    private HashMap<String, String> users = new HashMap<>();           // username -> password
    private HashMap<String, boolean[][]> seatMap = new HashMap<>();    // "title|date|time" -> booked seats
    private Stack<String> selectedSeats = new Stack<>();
    private Queue<Booking> paymentQueue = new LinkedList<>();
    private LinkedList<Booking> history = new LinkedList<>();

    private String currentUser;
    private Movie currentMovie;
    private String currentDate;
    private String currentTime;

    public BookingManager() {
        users.put("user", "1234");

        movies.add(new Movie("Space Odyssey X", "Sci-Fi", 125, 350.00, "Grand Cinema 1", "10:00 AM", "1:30 PM", "7:00 PM"));
        movies.add(new Movie("Love in the City", "Romance", 110, 300.00, "City Cinema 2", "11:00 AM", "3:00 PM", "6:30 PM"));
        movies.add(new Movie("Haunted Hallway", "Horror", 98, 320.00, "Plaza Cinema 3", "12:00 PM", "5:00 PM", "9:00 PM"));
        movies.add(new Movie("Laugh Out Loud", "Comedy", 105, 280.00, "Grand Cinema 4", "10:30 AM", "2:00 PM", "7:30 PM"));

        // A few seats are already booked so the seat map looks realistic
        for (String seat : new String[]{"A1", "A2", "D5", "D6"}) {
            markSeat("Space Odyssey X", "Today", "7:00 PM", seat, true);
        }
    }

    // ----- Sign in -----

    public boolean login(String username, String password) {
        if (users.containsKey(username) && users.get(username).equals(password)) {
            currentUser = username;
            return true;
        }
        return false;
    }

    /** Returns false if the username is already taken. */
    public boolean createAccount(String username, String password) {
        if (users.containsKey(username)) {
            return false;
        }
        users.put(username, password);
        return true;
    }

    public void logout() {
        currentUser = null;
        selectedSeats.clear();
    }

    // ----- Movies -----

    public ArrayList<Movie> getMovies() { return movies; }
    public ArrayList<String> getDates() { return dates; }
    public Movie getCurrentMovie() { return currentMovie; }
    public String getCurrentDate() { return currentDate; }
    public String getCurrentTime() { return currentTime; }

    /** Called when the user chooses a movie, a date and a time. */
    public void selectMovie(Movie movie, String date, String time) {
        currentMovie = movie;
        currentDate = date;
        currentTime = time;
        selectedSeats.clear();
    }

    // ----- Seats -----

    private boolean[][] getSeats(String title, String date, String time) {
        String key = title + "|" + date + "|" + time;
        if (!seatMap.containsKey(key)) {
            seatMap.put(key, new boolean[ROWS][COLS]);
        }
        return seatMap.get(key);
    }

    /** Marks a seat such as "C4" as booked (true) or free (false). */
    private void markSeat(String title, String date, String time, String label, boolean booked) {
        int row = label.charAt(0) - 'A';
        int col = Integer.parseInt(label.substring(1)) - 1;
        getSeats(title, date, time)[row][col] = booked;
    }

    /** Row 0, column 0 gives "A1". Row 2, column 3 gives "C4". */
    public String seatLabel(int row, int col) {
        return "" + (char) ('A' + row) + (col + 1);
    }

    public boolean isBooked(int row, int col) {
        return getSeats(currentMovie.title, currentDate, currentTime)[row][col];
    }

    public boolean isSelected(String label) {
        return selectedSeats.contains(label);
    }

    /** Clicking a seat: select it (push on the stack) or unselect it if it is already selected. */
    public void toggleSeat(String label) {
        if (selectedSeats.contains(label)) {
            selectedSeats.remove(label);
        } else {
            selectedSeats.push(label);
        }
    }

    /** Undo: removes the most recently picked seat (pop from the stack). */
    public void undoLastSeat() {
        if (!selectedSeats.isEmpty()) {
            selectedSeats.pop();
        }
    }

    public ArrayList<String> getSelectedSeats() {
        return new ArrayList<>(selectedSeats);
    }

    public double getTotal() {
        return selectedSeats.size() * currentMovie.price;
    }

    // ----- Payment queue and history -----

    /** Step 1: the new booking waits at the back of the payment queue. */
    public void addToPaymentQueue(String email) {
        paymentQueue.offer(new Booking(currentUser, currentMovie.title, currentDate, currentTime,
                getSelectedSeats(), getTotal(), email));
    }

    /** Step 2: bookings leave the front of the queue one by one and go to the history. */
    public void processPayments() {
        while (!paymentQueue.isEmpty()) {
            Booking booking = paymentQueue.poll();
            for (String seat : booking.seats) {
                markSeat(booking.movieTitle, booking.date, booking.time, seat, true);
            }
            history.addFirst(booking);   // newest booking goes first
        }
        selectedSeats.clear();
    }

    /** Bookings of the signed-in user, newest first. */
    public ArrayList<Booking> getHistory() {
        ArrayList<Booking> mine = new ArrayList<>();
        for (Booking booking : history) {
            if (booking.username.equals(currentUser)) {
                mine.add(booking);
            }
        }
        return mine;
    }
}

/** A movie that can be booked. */
class Movie {
    String title, genre, cinema;
    int duration;       // minutes
    double price;       // per seat
    ArrayList<String> showtimes = new ArrayList<>();

    Movie(String title, String genre, int duration, double price, String cinema, String... times) {
        this.title = title;
        this.genre = genre;
        this.duration = duration;
        this.price = price;
        this.cinema = cinema;
        showtimes.addAll(Arrays.asList(times));
    }
}

/** One ticket booking. */
class Booking {
    private static int counter = 1000;   // gives every booking a new ID

    int id;
    String username, movieTitle, date, time, email;
    ArrayList<String> seats;
    double total;

    Booking(String username, String movieTitle, String date, String time,
            ArrayList<String> seats, double total, String email) {
        counter++;
        this.id = counter;
        this.username = username;
        this.movieTitle = movieTitle;
        this.date = date;
        this.time = time;
        this.seats = seats;
        this.total = total;
        this.email = email;
    }
}
