package com.library.controller;

import com.library.controller.common.SidebarController;
import com.library.controller.common.TopBarController;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Member;
import com.library.service.LoanService;
import com.library.service.BookService;
import com.library.service.MemberService;
import com.library.repository.LoanRepository;
import com.library.repository.BookRepository;
import com.library.repository.MemberRepository;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for Return.fxml. Backed by real data via LibraryService —
 * active loans and recent returns come straight from the database, and
 * each active loan row has a "Return" button that actually completes
 * the loan and frees up the book.
 */
public class ReturnController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final double FINE_PER_DAY_OVERDUE = 0.50;

    private final BookService bookService = new BookService(new BookRepository());
    private final MemberService memberService = new MemberService(new MemberRepository());
    private final LoanService loanService = new LoanService(new LoanRepository(), bookService, memberService);

    @FXML private SidebarController sidebarController;
    @FXML private TopBarController topBarController;

    @FXML private TextField searchField;
    @FXML private VBox activeBorrowingsBox;
    @FXML private VBox recentReturnsBox;
    @FXML private Label activeCountLabel;
    @FXML private Label activeBorrowsLabel;
    @FXML private Label overdueLabel;
    @FXML private Label returnedTodayLabel;
    @FXML private Label finesLabel;

    private List<Loan> allLoans = List.of();
    private Map<Integer, Book> booksById = Map.of();
    private Map<Integer, Member> membersById = Map.of();

    @FXML
    public void initialize() {
        sidebarController.setActive(SidebarController.NavItem.RETURN);
        topBarController.setTitle("Return Book", "Process book returns");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> buildActiveBorrowings());

        reload();
    }

    private void reload() {
        try {
            allLoans = loanService.getAllLoans();
            booksById = bookService.getAllBooks().stream()
                    .collect(Collectors.toMap(Book::getId, b -> b, (a, b) -> a));
            membersById = memberService.getAllMembers().stream()
                    .collect(Collectors.toMap(Member::getId, m -> m, (a, b) -> a));

            buildActiveBorrowings();
            buildRecentReturns();
            buildStats();
        } catch (SQLException e) {
            showError("Database Error", "Failed to load loans: " + e.getMessage());
        }
    }

    private void buildStats() {
        List<Loan> active = allLoans.stream().filter(l -> !l.isReturned()).toList();
        long overdueCount = active.stream().filter(Loan::isOverdue).count();
        long returnedToday = allLoans.stream()
                .filter(Loan::isReturned)
                .filter(l -> LocalDate.now().equals(l.getReturnDate()))
                .count();
        double totalFines = active.stream().filter(Loan::isOverdue).mapToDouble(this::fineFor).sum();

        activeBorrowsLabel.setText(String.valueOf(active.size()));
        overdueLabel.setText(String.valueOf(overdueCount));
        returnedTodayLabel.setText(String.valueOf(returnedToday));
        finesLabel.setText(String.format("$%.2f", totalFines));
    }

    private double fineFor(Loan loan) {
        if (!loan.isOverdue()) {
            return 0.0;
        }
        long daysOverdue = ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now());
        return daysOverdue * FINE_PER_DAY_OVERDUE;
    }

    private void buildActiveBorrowings() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        List<Loan> active = allLoans.stream()
                .filter(l -> !l.isReturned())
                .filter(l -> keyword.isEmpty() || matchesSearch(l, keyword))
                .sorted(Comparator.comparing(Loan::getDueDate))
                .toList();

        activeCountLabel.setText(active.size() + " active borrowings");
        activeBorrowingsBox.getChildren().clear();

        if (active.isEmpty()) {
            Label empty = new Label("No active borrowings.");
            empty.getStyleClass().add("active-borrow-subtitle");
            activeBorrowingsBox.getChildren().add(empty);
            return;
        }

        for (Loan loan : active) {
            Book book = booksById.get(loan.getBookId());
            Member member = membersById.get(loan.getMemberId());

            Label icon = new Label("\uD83D\uDCD8");
            Label title = new Label(book == null ? "Unknown book" : book.getTitle());
            title.getStyleClass().add("active-borrow-title");
            Label subtitle = new Label((member == null ? "Unknown member" : member.getName())
                    + " \u00B7 LN-" + String.format("%03d", loan.getId()));
            subtitle.getStyleClass().add("active-borrow-subtitle");
            VBox textBox = new VBox(2, title, subtitle);

            Label due = new Label("Due: " + fmt(loan.getDueDate()));
            due.getStyleClass().add("active-borrow-due");
            if (loan.isOverdue()) {
                due.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            }

            Button returnBtn = new Button("Return");
            returnBtn.getStyleClass().add("btn-primary");
            returnBtn.setOnAction(e -> completeReturn(loan));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(10, icon, textBox, spacer, due, returnBtn);
            row.setAlignment(Pos.CENTER_LEFT);
            activeBorrowingsBox.getChildren().add(row);
        }
    }

    private boolean matchesSearch(Loan loan, String keyword) {
        Book book = booksById.get(loan.getBookId());
        Member member = membersById.get(loan.getMemberId());
        String recordId = "ln-" + String.format("%03d", loan.getId());
        return recordId.contains(keyword)
                || (book != null && book.getTitle().toLowerCase().contains(keyword))
                || (member != null && member.getName().toLowerCase().contains(keyword));
    }

    private void buildRecentReturns() {
        List<Loan> returned = allLoans.stream()
                .filter(Loan::isReturned)
                .sorted(Comparator.comparing(Loan::getReturnDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .toList();

        recentReturnsBox.getChildren().clear();
        if (returned.isEmpty()) {
            Label empty = new Label("No returns yet.");
            empty.getStyleClass().add("return-item-subtitle");
            recentReturnsBox.getChildren().add(empty);
            return;
        }

        for (Loan loan : returned) {
            Book book = booksById.get(loan.getBookId());
            Member member = membersById.get(loan.getMemberId());
            boolean wasLate = loan.getDueDate() != null && loan.getReturnDate() != null
                    && loan.getReturnDate().isAfter(loan.getDueDate());

            Label title = new Label(book == null ? "Unknown book" : book.getTitle());
            title.getStyleClass().add("return-item-title");
            Label subtitle = new Label((member == null ? "Unknown member" : member.getName())
                    + " \u00B7 " + fmt(loan.getReturnDate()));
            subtitle.getStyleClass().add("return-item-subtitle");
            VBox textBox = new VBox(2, title, subtitle);

            Label badge = new Label(wasLate ? "Returned Late" : "On Time");
            badge.getStyleClass().addAll("badge", wasLate ? "badge-amber" : "badge-green");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(10, textBox, spacer, badge);
            row.setAlignment(Pos.CENTER_LEFT);
            recentReturnsBox.getChildren().add(row);
        }
    }

    private void completeReturn(Loan loan) {
        try {
            loanService.returnBook(loan.getId());
            reload();
        } catch (IllegalStateException e) {
            showError("Could Not Return Book", e.getMessage());
        } catch (SQLException e) {
            showError("Database Error", "Failed to return book: " + e.getMessage());
        }
    }

    private String fmt(LocalDate date) {
        return date == null ? "—" : date.format(DATE_FORMAT);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
}
