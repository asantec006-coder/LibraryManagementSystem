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
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for Borrow.fxml. Backed by real data via LibraryService —
 * dropdowns list actual available books and existing members, and
 * "Issue Book" creates a real Loan row and decrements availability.
 */
public class BorrowController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final BookService bookService = new BookService(new BookRepository());
    private final MemberService memberService = new MemberService(new MemberRepository());
    private final LoanService loanService = new LoanService(new LoanRepository(), bookService, memberService);

    @FXML private SidebarController sidebarController;
    @FXML private TopBarController topBarController;

    @FXML private ComboBox<Member> memberCombo;
    @FXML private ComboBox<Book> bookCombo;
    @FXML private DatePicker issueDatePicker;
    @FXML private DatePicker dueDatePicker;
    @FXML private VBox recentlyIssuedBox;

    @FXML
    public void initialize() {
        sidebarController.setActive(SidebarController.NavItem.BORROW);
        topBarController.setTitle("Borrow Book", "Issue books to members");

        memberCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Member m) {
                return m == null ? "" : m.getName() + " (M" + String.format("%03d", m.getId()) + ")";
            }
            @Override
            public Member fromString(String s) {
                return null;
            }
        });
        memberCombo.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Member m, boolean empty) {
                super.updateItem(m, empty);
                setText(empty || m == null ? null : m.getName() + " (M" + String.format("%03d", m.getId()) + ")");
            }
        });

        bookCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Book b) {
                return b == null ? "" : b.getTitle() + " — " + b.getAuthor();
            }
            @Override
            public Book fromString(String s) {
                return null;
            }
        });
        bookCombo.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            private final javafx.scene.image.ImageView thumb = new javafx.scene.image.ImageView();
            {
                thumb.setFitWidth(24);
                thumb.setFitHeight(32);
                thumb.setPreserveRatio(false);
            }
            @Override
            protected void updateItem(Book b, boolean empty) {
                super.updateItem(b, empty);
                if (empty || b == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(b.getTitle() + " — " + b.getAuthor());
                    thumb.setImage(com.library.util.CoverImageLoader.load(b));
                    setGraphic(thumb);
                }
            }
        });

        issueDatePicker.setValue(LocalDate.now());
        issueDatePicker.setDisable(true); // always today's date

        reload();
    }

    private void reload() {
        try {
            List<Member> members = memberService.getAllMembers();
            memberCombo.setItems(FXCollections.observableArrayList(members));

            List<Book> availableBooks = bookService.getAllBooks().stream()
                    .filter(Book::isAvailable)
                    .toList();
            bookCombo.setItems(FXCollections.observableArrayList(availableBooks));

            buildRecentlyIssued();
        } catch (SQLException e) {
            showError("Database Error", "Failed to load data: " + e.getMessage());
        }
    }

    private void buildRecentlyIssued() throws SQLException {
        List<Loan> loans = loanService.getAllLoans();
        Map<Integer, Book> booksById = bookService.getAllBooks().stream()
                .collect(Collectors.toMap(Book::getId, b -> b, (a, b) -> a));
        Map<Integer, Member> membersById = memberService.getAllMembers().stream()
                .collect(Collectors.toMap(Member::getId, m -> m, (a, b) -> a));

        List<Loan> recent = loans.stream()
                .filter(l -> !l.isReturned())
                .sorted(Comparator.comparing(Loan::getLoanDate).reversed())
                .limit(10)
                .toList();

        recentlyIssuedBox.getChildren().clear();
        if (recent.isEmpty()) {
            Label empty = new Label("No active loans yet.");
            empty.getStyleClass().add("recent-item-subtitle");
            recentlyIssuedBox.getChildren().add(empty);
            return;
        }

        for (Loan loan : recent) {
            Book book = booksById.get(loan.getBookId());
            Member member = membersById.get(loan.getMemberId());

            javafx.scene.image.ImageView coverView = new javafx.scene.image.ImageView(
                    com.library.util.CoverImageLoader.load(book));
            coverView.setFitWidth(32);
            coverView.setFitHeight(44);
            coverView.setPreserveRatio(false);

            Label title = new Label(book == null ? "Unknown book" : book.getTitle());
            title.getStyleClass().add("recent-item-title");
            Label borrower = new Label(member == null ? "Unknown member" : member.getName());
            borrower.getStyleClass().add("recent-item-subtitle");
            Label dates = new Label("\uD83D\uDCC5 " + fmt(loan.getLoanDate()) + "   \u23F0 Due: " + fmt(loan.getDueDate()));
            dates.getStyleClass().add("recent-item-date");

            VBox textBox = new VBox(2, title, borrower, dates);

            Label statusBadge = new Label(loan.isOverdue() ? "Overdue" : "Active");
            statusBadge.getStyleClass().addAll("badge", loan.isOverdue() ? "badge-red" : "badge-green");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(10, coverView, textBox, spacer, statusBadge);
            row.setAlignment(Pos.CENTER_LEFT);
            recentlyIssuedBox.getChildren().add(row);
        }
    }

    private String fmt(LocalDate date) {
        return date == null ? "—" : date.format(DATE_FORMAT);
    }

    @FXML
    private void onIssue() {
        if (memberCombo.getValue() == null || bookCombo.getValue() == null || dueDatePicker.getValue() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "Select a member, a book, and a due date before issuing.", ButtonType.OK);
            alert.setHeaderText("Missing information");
            alert.showAndWait();
            return;
        }
        if (!dueDatePicker.getValue().isAfter(LocalDate.now())) {
            showError("Invalid Due Date", "The due date must be after today.");
            return;
        }
        try {
            loanService.issueBook(bookCombo.getValue().getId(), memberCombo.getValue().getId(), dueDatePicker.getValue());
            bookCombo.setValue(null);
            memberCombo.setValue(null);
            dueDatePicker.setValue(null);
            reload();
        } catch (IllegalStateException e) {
            showError("Could Not Issue Book", e.getMessage());
        } catch (SQLException e) {
            showError("Database Error", "Failed to issue book: " + e.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
}
