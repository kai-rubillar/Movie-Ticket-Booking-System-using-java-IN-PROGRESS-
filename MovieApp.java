import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;

/**
 * The screens of the Movie Booking System. Run this file (it has the main method).
 *
 * Every screen is built by one makeXxxScreen() method, and a CardLayout shows one screen at a time:
 *   signin -> dashboard -> seats -> checkout -> confirmation
 */
public class MovieApp extends JPanel {
    private static final Color DARK = new Color(24, 28, 48);
    private static final Color RED = new Color(178, 34, 34);
    private static final Color GRAY = new Color(120, 126, 145);
    private static final Color GOLD = new Color(245, 180, 30);
    private static final Color SEAT_FREE = new Color(205, 210, 225);
    private static final Font BIG = new Font("SansSerif", Font.BOLD, 24);

    private BookingManager manager = new BookingManager();
    private CardLayout cards = new CardLayout();

    // Things that change while the program runs
    private JTextField userField = new JTextField(18);
    private JPasswordField passField = new JPasswordField(18);
    private JLabel signInMessage = new JLabel(" ", JLabel.CENTER);
    private JPanel moviesPanel = new JPanel(new GridLayout(0, 1, 0, 10));
    private JLabel seatTitle = new JLabel("", JLabel.CENTER);
    private JPanel seatGrid = new JPanel(new GridLayout(BookingManager.ROWS, BookingManager.COLS, 6, 6));
    private JLabel selectedLabel = new JLabel();
    private JLabel orderLabel = new JLabel();
    private JTextField numberField = new JTextField(20);
    private JTextField nameField = new JTextField(20);
    private JTextField emailField = new JTextField(20);
    private JPasswordField ccvField = new JPasswordField(20);
    private JLabel checkoutMessage = new JLabel(" ", JLabel.CENTER);
    private JLabel confirmMessage = new JLabel("", JLabel.CENTER);
    private JTextArea historyArea = new JTextArea(6, 40);

    public MovieApp() {
        setLayout(cards);
        add(makeSignInScreen(), "signin");
        add(makeDashboardScreen(), "dashboard");
        add(makeSeatScreen(), "seats");
        add(makeCheckoutScreen(), "checkout");
        add(makeConfirmationScreen(), "confirmation");
        showScreen("signin");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Movie Booking System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new MovieApp());
            frame.getContentPane().setPreferredSize(new Dimension(900, 600));
            frame.pack();
            frame.setLocationRelativeTo(null);   // open in the middle of the screen
            frame.setVisible(true);
        });
    }

    /** Shows a screen. Screens that show changing data are refreshed first. */
    public void showScreen(String name) {
        if (name.equals("signin")) {
            userField.setText("");
            passField.setText("");
            signInMessage.setText(" ");
        } else if (name.equals("dashboard")) {
            drawMovies();
        } else if (name.equals("seats")) {
            drawSeats();
        } else if (name.equals("checkout")) {
            drawCheckout();
        } else if (name.equals("confirmation")) {
            drawConfirmation();
        }
        cards.show(this, name);
    }

    public BookingManager getManager() { return manager; }

    // =================================================================
    // Screen 1: Sign In
    // =================================================================

    private JPanel makeSignInScreen() {
        JLabel title = new JLabel("SIGN IN", JLabel.CENTER);
        title.setFont(BIG);

        JButton signInButton = makeButton("Sign In", DARK);
        JButton createButton = makeButton("Create Account", GRAY);
        signInButton.addActionListener(e -> signIn());
        passField.addActionListener(e -> signIn());   // pressing Enter also signs in
        createButton.addActionListener(e -> createAccount());

        JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 0));
        buttons.add(signInButton);
        buttons.add(createButton);

        JPanel card = new JPanel(new GridLayout(0, 1, 0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));
        card.add(title);
        card.add(new JLabel("Username"));
        card.add(userField);
        card.add(new JLabel("Password"));
        card.add(passField);
        card.add(signInMessage);
        card.add(buttons);
        card.add(new JLabel("Demo account:  user / 1234", JLabel.CENTER));

        JPanel screen = new JPanel(new GridBagLayout());   // puts the card in the middle
        screen.setBackground(new Color(217, 217, 217));
        screen.add(card);
        return screen;
    }

    private void signIn() {
        if (manager.login(userField.getText().trim(), new String(passField.getPassword()))) {
            showScreen("dashboard");
        } else {
            signInMessage.setForeground(RED);
            signInMessage.setText("Wrong username or password!");
        }
    }

    private void createAccount() {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            signInMessage.setForeground(RED);
            signInMessage.setText("Type a username and password first.");
        } else if (manager.createAccount(username, password)) {
            signInMessage.setForeground(new Color(46, 160, 100));
            signInMessage.setText("Account created! You can sign in now.");
        } else {
            signInMessage.setForeground(RED);
            signInMessage.setText("That username is already taken.");
        }
    }

    // =================================================================
    // Screen 2: Dashboard (New Release + showtimes)
    // =================================================================

    private JPanel makeDashboardScreen() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        wrapper.add(moviesPanel, BorderLayout.NORTH);

        JPanel screen = new JPanel(new BorderLayout());
        screen.add(makeHeader("New Release"), BorderLayout.NORTH);
        screen.add(wrapper, BorderLayout.CENTER);
        return screen;
    }

    /** One row for every movie: details on the left, date + time + button on the right. */
    private void drawMovies() {
        moviesPanel.removeAll();
        for (Movie movie : manager.getMovies()) {
            JLabel info = new JLabel("<html><b>" + movie.title + "</b><br>" + movie.genre + "  |  "
                    + movie.duration + " min  |  " + money(movie.price) + " per seat<br>"
                    + movie.cinema + "</html>");

            JComboBox<String> dateBox = new JComboBox<>(manager.getDates().toArray(new String[0]));
            JComboBox<String> timeBox = new JComboBox<>(movie.showtimes.toArray(new String[0]));
            JButton seatsButton = makeButton("Select Seats", RED);
            seatsButton.addActionListener(e -> {
                manager.selectMovie(movie, (String) dateBox.getSelectedItem(), (String) timeBox.getSelectedItem());
                showScreen("seats");
            });

            JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
            controls.setOpaque(false);
            controls.add(dateBox);
            controls.add(timeBox);
            controls.add(seatsButton);

            JPanel row = new JPanel(new BorderLayout());
            row.setBackground(Color.WHITE);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 203, 214)),
                    BorderFactory.createEmptyBorder(8, 14, 8, 14)));
            row.add(info, BorderLayout.WEST);
            row.add(controls, BorderLayout.EAST);
            moviesPanel.add(row);
        }
        moviesPanel.revalidate();
        moviesPanel.repaint();
    }

    // =================================================================
    // Screen 3: Movie Seats
    // =================================================================

    private JPanel makeSeatScreen() {
        JLabel screenBar = new JLabel("Movie Screen", JLabel.CENTER);
        screenBar.setOpaque(true);
        screenBar.setBackground(new Color(217, 217, 217));
        screenBar.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        seatGrid.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        seatTitle.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel middle = new JPanel(new BorderLayout(0, 8));
        middle.setBorder(BorderFactory.createEmptyBorder(15, 80, 5, 80));
        JPanel top = new JPanel(new GridLayout(0, 1, 0, 6));
        top.add(seatTitle);
        top.add(screenBar);
        middle.add(top, BorderLayout.NORTH);
        middle.add(seatGrid, BorderLayout.CENTER);

        // Bottom bar: selected seats on the left, buttons on the right
        JButton backButton = makeButton("Back", GRAY);
        JButton undoButton = makeButton("Undo Last Seat", GRAY);
        JButton confirmButton = makeButton("Confirm", RED);
        backButton.addActionListener(e -> showScreen("dashboard"));
        undoButton.addActionListener(e -> {
            manager.undoLastSeat();   // pop from the stack
            drawSeats();
        });
        confirmButton.addActionListener(e -> {
            if (manager.getSelectedSeats().isEmpty()) {
                selectedLabel.setText("Please select at least one seat.");
            } else {
                showScreen("checkout");
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(backButton);
        buttons.add(undoButton);
        buttons.add(confirmButton);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        bottom.add(selectedLabel, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);

        JPanel screen = new JPanel(new BorderLayout());
        screen.add(makeHeader("Movie Seats"), BorderLayout.NORTH);
        screen.add(middle, BorderLayout.CENTER);
        screen.add(bottom, BorderLayout.SOUTH);
        return screen;
    }

    /** Grey = free, gold = selected, red = already booked. */
    private void drawSeats() {
        Movie movie = manager.getCurrentMovie();
        seatTitle.setText(movie.title + "   |   " + movie.cinema + "   |   "
                + manager.getCurrentDate() + ", " + manager.getCurrentTime());

        seatGrid.removeAll();
        for (int row = 0; row < BookingManager.ROWS; row++) {
            for (int col = 0; col < BookingManager.COLS; col++) {
                String label = manager.seatLabel(row, col);
                JButton seat = new JButton(label);
                seat.setOpaque(true);
                seat.setBorderPainted(false);
                seat.setFocusPainted(false);

                if (manager.isBooked(row, col)) {
                    seat.setBackground(RED);
                    seat.setForeground(Color.WHITE);
                } else {
                    seat.setBackground(manager.isSelected(label) ? GOLD : SEAT_FREE);
                    seat.addActionListener(e -> {
                        manager.toggleSeat(label);
                        drawSeats();
                    });
                }
                seatGrid.add(seat);
            }
        }
        seatGrid.revalidate();
        seatGrid.repaint();

        selectedLabel.setText("<html><b>Selected seats:</b> " + String.join(", ", manager.getSelectedSeats())
                + "<br>Total: " + money(manager.getTotal()) + "</html>");
    }

    // =================================================================
    // Screen 4: Checkout
    // =================================================================

    private JPanel makeCheckoutScreen() {
        JLabel title = new JLabel("Checkout", JLabel.CENTER);
        title.setFont(BIG);

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 8));
        form.add(new JLabel("Account Number:"));
        form.add(numberField);
        form.add(new JLabel("Account Name:"));
        form.add(nameField);
        form.add(new JLabel("Email:"));
        form.add(emailField);
        form.add(new JLabel("CCV:"));
        form.add(ccvField);

        checkoutMessage.setForeground(RED);
        JButton backButton = makeButton("Back", GRAY);
        JButton confirmButton = makeButton("Confirm", RED);
        backButton.addActionListener(e -> showScreen("seats"));
        confirmButton.addActionListener(e -> confirmPayment());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(backButton);
        buttons.add(confirmButton);

        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.add(checkoutMessage, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.add(orderLabel, BorderLayout.NORTH);
        body.add(form, BorderLayout.CENTER);

        JPanel card = new JPanel(new BorderLayout(0, 15));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        card.add(title, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        card.add(south, BorderLayout.SOUTH);
        body.setOpaque(false);
        south.setOpaque(false);
        form.setOpaque(false);
        buttons.setOpaque(false);

        JPanel middle = new JPanel(new GridBagLayout());
        middle.add(card);

        JPanel screen = new JPanel(new BorderLayout());
        screen.add(makeHeader("Checkout"), BorderLayout.NORTH);
        screen.add(middle, BorderLayout.CENTER);
        return screen;
    }

    private void drawCheckout() {
        String text = "<html><b>Order</b> (" + manager.getCurrentMovie().title + ", "
                + manager.getCurrentDate() + " " + manager.getCurrentTime() + ")<br>";
        for (String seat : manager.getSelectedSeats()) {
            text += "Seat " + seat + " ........ " + money(manager.getCurrentMovie().price) + "<br>";
        }
        text += "<b>Total: " + money(manager.getTotal()) + "</b></html>";
        orderLabel.setText(text);

        numberField.setText("");
        nameField.setText("");
        emailField.setText("");
        ccvField.setText("");
        checkoutMessage.setText(" ");
    }

    private void confirmPayment() {
        String number = numberField.getText().trim();
        String email = emailField.getText().trim();
        String ccv = new String(ccvField.getPassword());

        // Check the form first
        if (!number.matches("\\d{12,16}")) {
            checkoutMessage.setText("Account number must have 12 to 16 digits.");
        } else if (nameField.getText().trim().isEmpty()) {
            checkoutMessage.setText("Please type the account name.");
        } else if (!email.contains("@")) {
            checkoutMessage.setText("Please type a valid email address.");
        } else if (!ccv.matches("\\d{3}")) {
            checkoutMessage.setText("CCV must have 3 digits.");
        } else {
            manager.addToPaymentQueue(email);   // join the back of the queue
            manager.processPayments();          // serve the queue from the front
            showScreen("confirmation");
        }
    }

    // =================================================================
    // Screen 5: Order Confirmation (also lists the booking history)
    // =================================================================

    private JPanel makeConfirmationScreen() {
        JLabel title = new JLabel("<html><div align='center'>Your order has<br>been confirmed!</div></html>", JLabel.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 30));

        historyArea.setEditable(false);
        JButton doneButton = makeButton("Done", RED);
        doneButton.addActionListener(e -> showScreen("dashboard"));

        JPanel doneRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        doneRow.setOpaque(false);
        doneRow.add(doneButton);

        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        JPanel top = new JPanel(new GridLayout(0, 1, 0, 10));
        top.setOpaque(false);
        top.add(title);
        top.add(confirmMessage);
        top.add(new JLabel("Your bookings (newest first):"));
        card.add(top, BorderLayout.NORTH);
        card.add(new JScrollPane(historyArea), BorderLayout.CENTER);
        card.add(doneRow, BorderLayout.SOUTH);

        JPanel middle = new JPanel(new GridBagLayout());
        middle.add(card);

        JPanel screen = new JPanel(new BorderLayout());
        screen.add(makeHeader("Order Confirmation"), BorderLayout.NORTH);
        screen.add(middle, BorderLayout.CENTER);
        return screen;
    }

    private void drawConfirmation() {
        ArrayList<Booking> history = manager.getHistory();   // newest booking is first
        Booking latest = history.get(0);
        confirmMessage.setText("<html><div align='center'>The tickets will be sent to you via email at<br><b>"
                + latest.email + "</b>. Please check your email for the ticket details.</div></html>");

        String text = "";
        for (Booking b : history) {
            text += "#" + b.id + "  " + b.movieTitle + "  |  " + b.date + " " + b.time
                    + "  |  Seats: " + String.join(", ", b.seats) + "  |  " + money(b.total) + "\n";
        }
        historyArea.setText(text);
    }

    // =================================================================
    // Small helpers
    // =================================================================

    /** The dark bar at the top of every screen after sign in. */
    private JPanel makeHeader(String titleText) {
        JLabel title = new JLabel(titleText);
        title.setFont(BIG);
        title.setForeground(Color.WHITE);

        JButton signOut = makeButton("Sign out", RED);
        signOut.addActionListener(e -> {
            manager.logout();
            showScreen("signin");
        });

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DARK);
        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        header.add(title, BorderLayout.WEST);
        header.add(signOut, BorderLayout.EAST);
        return header;
    }

    private JButton makeButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return button;
    }

    private String money(double amount) {
        return "PHP" + String.format("%.2f", amount);
    }
}
