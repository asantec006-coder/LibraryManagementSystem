package com.library.controller;

import com.library.controller.common.SidebarController;
import com.library.controller.common.TopBarController;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Books.fxml. Visual/static prototype using mock data laid
 * out as a card grid, matching the Figma "Books" screen.
 *
 * The real Books Module (Book/PhysicalBook/EBook models, BookRepository,
 * BookService) already exists from Phase 3 and is untouched — when this
 * screen is wired for real, replace the mock list in loadMockBooks() with
 * bookService.getAllBooks() and map each Book to a card the same way.
 */
public class BooksController {

    @FXML private SidebarController sidebarController;
    @FXML private TopBarController topBarController;

    @FXML private FlowPane bookCardsPane;
    @FXML private Label summaryLabel;

    private record BookCard(String title, String author, String genre, double rating,
                             int available, int total, String coverStyleClass) {
    }

    @FXML
    public void initialize() {
        sidebarController.setActive(SidebarController.NavItem.BOOKS);
        topBarController.setTitle("Books", "Manage your library collection");

        List<BookCard> books = loadMockBooks();
        renderCards(books);
        updateSummary(books);
    }

    private List<BookCard> loadMockBooks() {
        List<BookCard> books = new ArrayList<>();
        books.add(new BookCard("The Pragmatic Programmer", "David Thomas, Andrew Hunt", "Technology", 4.8, 3, 5, "cover-blue"));
        books.add(new BookCard("Dune", "Frank Herbert", "Fiction", 4.9, 0, 8, "cover-purple"));
        books.add(new BookCard("Atomic Habits", "James Clear", "Self-Help", 4.7, 4, 6, "cover-green"));
        books.add(new BookCard("Clean Code", "Robert C. Martin", "Technology", 4.6, 2, 4, "cover-orange"));
        books.add(new BookCard("1984", "George Orwell", "Fiction", 4.8, 5, 6, "cover-red"));
        books.add(new BookCard("Sapiens", "Yuval Noah Harari", "History", 4.7, 3, 5, "cover-cyan"));
        books.add(new BookCard("The Hobbit", "J.R.R. Tolkien", "Fiction", 4.9, 4, 5, "cover-indigo"));
        books.add(new BookCard("A Brief History of Time", "Stephen Hawking", "Science", 4.6, 3, 4, "cover-teal"));
        return books;
    }

    private void renderCards(List<BookCard> books) {
        bookCardsPane.getChildren().clear();
        for (BookCard book : books) {
            bookCardsPane.getChildren().add(buildCard(book));
        }
    }

    private VBox buildCard(BookCard book) {
        Label ratingBadge = new Label("\u2605 " + book.rating());
        ratingBadge.getStyleClass().add("book-card-rating-badge");

        Label genreBadge = new Label(book.genre());
        genreBadge.getStyleClass().add("book-card-genre-badge");

        Label coverIcon = new Label("\uD83D\uDCD6");
        coverIcon.getStyleClass().add("book-card-cover-icon");

        StackPane cover = new StackPane(coverIcon);
        cover.getStyleClass().addAll("book-card-cover", book.coverStyleClass());
        StackPane.setAlignment(ratingBadge, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(genreBadge, Pos.TOP_RIGHT);
        cover.getChildren().addAll(genreBadge, ratingBadge);
        cover.setPadding(new javafx.geometry.Insets(10));

        Label title = new Label(book.title());
        title.getStyleClass().add("book-card-title");
        title.setWrapText(true);

        Label author = new Label(book.author());
        author.getStyleClass().add("book-card-author");
        author.setWrapText(true);

        boolean isAvailable = book.available() > 0;
        Label copies = new Label(book.available() + "/" + book.total());
        copies.getStyleClass().add("book-card-copies");
        copies.setStyle(isAvailable ? "-fx-text-fill: #16a34a;" : "-fx-text-fill: #dc2626;");

        Label statusBadge = new Label(isAvailable ? "Available" : "All Borrowed");
        statusBadge.getStyleClass().addAll("badge", isAvailable ? "badge-green" : "badge-red");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.layout.HBox bottomRow = new javafx.scene.layout.HBox(8, copies, spacer, statusBadge);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(4, title, author, bottomRow);
        body.getStyleClass().add("book-card-body");

        VBox card = new VBox(cover, body);
        card.getStyleClass().add("book-card");
        return card;
    }

    private void updateSummary(List<BookCard> books) {
        int totalAvailable = books.stream().mapToInt(BookCard::available).sum();
        summaryLabel.setText(books.size() + " books found · " + totalAvailable + " copies available");
    }

    @SuppressWarnings("unused")
    @FXML
    private void onAdd() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "The Add Book form will be wired up when the Books Module is connected to real data.",
                javafx.scene.control.ButtonType.OK);
        alert.setHeaderText("Coming soon");
        alert.showAndWait();
    }
}
