package com.library.controller;

import com.library.controller.common.SidebarController;
import com.library.controller.common.TopBarController;
import com.library.model.Book;
import com.library.model.EBook;
import com.library.model.PhysicalBook;
import com.library.repository.BookRepository;
import com.library.service.BookService;
import com.library.service.cover.CoverImageService;
import com.library.util.CoverImageLoader;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for Books.fxml. Backed by real data via BookService — no
 * mock/sample data. Add/Edit opens BookDialog.fxml, which also owns cover
 * search/upload/remove. Cards show the book's actual cached cover (falling
 * back to the shared default placeholder) via CoverImageLoader.
 */
public class BooksController {

    private final BookService bookService = new BookService(new BookRepository());
    private final CoverImageService coverImageService = new CoverImageService(bookService);

    @FXML private SidebarController sidebarController;
    @FXML private TopBarController topBarController;

    @FXML private FlowPane bookCardsPane;
    @FXML private Label summaryLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;

    private List<Book> currentBooks = List.of();

    @FXML
    public void initialize() {
        sidebarController.setActive(SidebarController.NavItem.BOOKS);
        topBarController.setTitle("Books", "Manage your library collection");

        filterCombo.setItems(javafx.collections.FXCollections.observableArrayList("All", "Physical", "EBook"));
        filterCombo.setValue("All");
        filterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());

        reload();

        String pendingQuery = com.library.util.PendingSearch.consumeIfFor(com.library.util.PendingSearch.Target.BOOKS);
        if (pendingQuery != null) {
            searchField.setText(pendingQuery);
        }
    }

    private void reload() {
        try {
            currentBooks = bookService.getAllBooks();
            applyFilter();
        } catch (SQLException e) {
            showError("Database Error", "Failed to load books: " + e.getMessage());
        }
    }

    private void applyFilter() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String type = filterCombo.getValue() == null ? "All" : filterCombo.getValue();

        List<Book> filtered = currentBooks.stream()
                .filter(b -> keyword.isEmpty()
                        || b.getTitle().toLowerCase().contains(keyword)
                        || b.getAuthor().toLowerCase().contains(keyword)
                        || b.getIsbn().toLowerCase().contains(keyword))
                .filter(b -> switch (type) {
                    case "Physical" -> b instanceof PhysicalBook;
                    case "EBook" -> b instanceof EBook;
                    default -> true;
                })
                .toList();

        renderCards(filtered);
        updateSummary(filtered);
    }

    private void renderCards(List<Book> books) {
        bookCardsPane.getChildren().clear();
        for (Book book : books) {
            bookCardsPane.getChildren().add(buildCard(book));
        }
    }

    private VBox buildCard(Book book) {
        boolean isPhysical = book instanceof PhysicalBook;
        int available = isPhysical ? ((PhysicalBook) book).getAvailableCopies() : ((EBook) book).getMaxConcurrentLicenses() - ((EBook) book).getActiveLoans();
        int total = isPhysical ? ((PhysicalBook) book).getTotalCopies() : ((EBook) book).getMaxConcurrentLicenses();

        Label genreBadge = new Label(book.getGenre() == null ? "—" : book.getGenre());
        genreBadge.getStyleClass().add("book-card-genre-badge");

        Label typeBadge = new Label(isPhysical ? "Physical" : "EBook");
        typeBadge.getStyleClass().add("book-card-rating-badge");

        ImageView coverView = new ImageView(CoverImageLoader.load(book));
        coverView.setFitWidth(220);
        coverView.setFitHeight(140);
        coverView.setPreserveRatio(false);

        StackPane cover = new StackPane(coverView);
        cover.getStyleClass().add("book-card-cover");
        cover.setMinHeight(140);
        cover.setMaxHeight(140);
        StackPane.setAlignment(typeBadge, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(genreBadge, Pos.TOP_RIGHT);
        cover.getChildren().addAll(genreBadge, typeBadge);
        cover.setPadding(new Insets(10));
        cover.setClip(new javafx.scene.shape.Rectangle(220, 140));

        Label title = new Label(book.getTitle());
        title.getStyleClass().add("book-card-title");
        title.setWrapText(true);

        Label author = new Label(book.getAuthor());
        author.getStyleClass().add("book-card-author");
        author.setWrapText(true);

        boolean isAvailable = book.isAvailable();
        Label copies = new Label(available + "/" + total);
        copies.getStyleClass().add("book-card-copies");
        copies.setStyle(isAvailable ? "-fx-text-fill: #16a34a;" : "-fx-text-fill: #dc2626;");

        Label statusBadge = new Label(isAvailable ? "Available" : "All Borrowed");
        statusBadge.getStyleClass().addAll("badge", isAvailable ? "badge-green" : "badge-red");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bottomRow = new HBox(8, copies, spacer, statusBadge);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setOnAction(e -> openBookDialog(book));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-secondary");
        deleteBtn.setOnAction(e -> deleteBook(book));

        HBox actionsRow = new HBox(8, editBtn, deleteBtn);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(4, title, author, bottomRow, actionsRow);
        body.getStyleClass().add("book-card-body");

        VBox card = new VBox(cover, body);
        card.getStyleClass().add("book-card");
        return card;
    }

    private void updateSummary(List<Book> books) {
        int totalAvailable = books.stream().mapToInt(b -> b instanceof PhysicalBook pb
                ? pb.getAvailableCopies()
                : ((EBook) b).getMaxConcurrentLicenses() - ((EBook) b).getActiveLoans()).sum();
        summaryLabel.setText(books.size() + " books found \u00B7 " + totalAvailable + " copies available");
    }

    @FXML
    private void onAdd() {
        openBookDialog(null);
    }

    /**
     * Scans the catalog for books missing a cover and tries to fetch one for
     * each from Open Library / Google Books, in the background, showing
     * progress without freezing the UI. This is this app's nav-bar
     * equivalent of a "Fetch Missing Covers" menu item (the app doesn't
     * have a traditional menu bar — it's a sidebar-driven layout — so this
     * lives as a toolbar button on the Books page instead).
     */
    @FXML
    private void onFetchMissingCovers() {
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        Label progressLabel = new Label("Scanning catalog...");
        VBox content = new VBox(10, progressLabel, progressBar);
        content.setPadding(new Insets(16));

        Dialog<Void> progressDialog = new Dialog<>();
        progressDialog.setTitle("Fetch Missing Covers");
        progressDialog.getDialogPane().setContent(content);
        progressDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        progressDialog.show();

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return coverImageService.fetchMissingCovers((processed, total) -> Platform.runLater(() -> {
                            if (total > 0) {
                                progressBar.setProgress((double) processed / total);
                                progressLabel.setText("Fetching covers... (" + processed + "/" + total + ")");
                            }
                        }));
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .thenAccept(updatedCount -> Platform.runLater(() -> {
                    progressDialog.close();
                    reload();
                    Alert done = new Alert(Alert.AlertType.INFORMATION,
                            updatedCount + " cover(s) were found and downloaded.", ButtonType.OK);
                    done.setHeaderText("Fetch Missing Covers Complete");
                    done.showAndWait();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        progressDialog.close();
                        showError("Fetch Missing Covers Failed", ex.getMessage());
                    });
                    return null;
                });
    }

    private void deleteBook(Book book) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + book.getTitle() + "\"? This cannot be undone.", ButtonType.YES, ButtonType.CANCEL);
        confirm.setHeaderText("Confirm deletion");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.YES) {
            try {
                bookService.deleteBook(book.getId());
                reload();
            } catch (SQLException | IllegalArgumentException e) {
                showError("Delete Failed", e.getMessage());
            }
        }
    }

    /** Add/Edit dialog (BookDialog.fxml). Pass null for "existing" to add a new book. */
    private void openBookDialog(Book existing) {
        boolean isEdit = existing != null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BookDialog.fxml"));
            javafx.scene.Parent dialogContent = loader.load();
            BookDialogController controller = loader.getController();
            controller.setBook(existing);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(isEdit ? "Edit Book" : "Add New Book");
            dialog.getDialogPane().setContent(dialogContent);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                try {
                    Book book = controller.getBook();
                    if (isEdit) {
                        bookService.updateBook(book);
                    } else {
                        bookService.addBook(book);
                    }
                } catch (IllegalArgumentException | SQLException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
                    alert.setHeaderText(e instanceof SQLException ? "Database Error" : "Validation Error");
                    alert.showAndWait();
                    event.consume();
                }
            });

            dialog.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    reload();
                }
            });
        } catch (IOException e) {
            showError("Could Not Open Dialog", e.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
}
