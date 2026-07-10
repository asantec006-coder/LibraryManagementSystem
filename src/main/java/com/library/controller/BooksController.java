package com.library.controller;

import com.library.controller.common.SidebarController;
import com.library.controller.common.TopBarController;
import com.library.model.Book;
import com.library.model.EBook;
import com.library.model.PhysicalBook;
import com.library.repository.BookRepository;
import com.library.service.BookService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller for Books.fxml. Backed by real data via BookService —
 * no mock/sample data. Add/Edit uses an in-code dialog (kept simple:
 * no separate FXML) that adapts its fields to Physical vs EBook.
 */
public class BooksController {

    private final BookService bookService = new BookService(new BookRepository());

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

        filterCombo.setItems(FXCollections.observableArrayList("All", "Physical", "EBook"));
        filterCombo.setValue("All");
        filterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());

        reload();
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

        Label coverIcon = new Label("\uD83D\uDCD6");
        coverIcon.getStyleClass().add("book-card-cover-icon");

        StackPane cover = new StackPane(coverIcon);
        cover.getStyleClass().addAll("book-card-cover", isPhysical ? "cover-blue" : "cover-purple");
        StackPane.setAlignment(typeBadge, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(genreBadge, Pos.TOP_RIGHT);
        cover.getChildren().addAll(genreBadge, typeBadge);
        cover.setPadding(new Insets(10));

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

    /** Add/Edit dialog. Pass null for "existing" to add a new book. */
    private void openBookDialog(Book existing) {
        boolean isEdit = existing != null;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Edit Book" : "Add New Book");

        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("Physical", "EBook"));
        typeCombo.setValue(isEdit ? (existing instanceof PhysicalBook ? "Physical" : "EBook") : "Physical");
        typeCombo.setDisable(isEdit); // changing type of an existing book is not supported

        TextField titleField = new TextField(isEdit ? existing.getTitle() : "");
        TextField authorField = new TextField(isEdit ? existing.getAuthor() : "");
        TextField isbnField = new TextField(isEdit ? existing.getIsbn() : "");
        TextField genreField = new TextField(isEdit ? existing.getGenre() : "");

        TextField totalCopiesField = new TextField(isEdit && existing instanceof PhysicalBook pb ? String.valueOf(pb.getTotalCopies()) : "1");
        TextField availableCopiesField = new TextField(isEdit && existing instanceof PhysicalBook pb ? String.valueOf(pb.getAvailableCopies()) : "1");

        TextField downloadUrlField = new TextField(isEdit && existing instanceof EBook eb ? eb.getDownloadUrl() : "");
        TextField maxLicensesField = new TextField(isEdit && existing instanceof EBook eb ? String.valueOf(eb.getMaxConcurrentLicenses()) : "1");
        TextField activeLoansField = new TextField(isEdit && existing instanceof EBook eb ? String.valueOf(eb.getActiveLoans()) : "0");

        VBox physicalGroup = new VBox(6, new Label("Total Copies"), totalCopiesField, new Label("Available Copies"), availableCopiesField);
        VBox ebookGroup = new VBox(6, new Label("Download URL"), downloadUrlField, new Label("Max Concurrent Licenses"), maxLicensesField, new Label("Active Loans"), activeLoansField);
        ebookGroup.setManaged(false);
        ebookGroup.setVisible(false);

        typeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean physical = "Physical".equals(newVal);
            physicalGroup.setVisible(physical);
            physicalGroup.setManaged(physical);
            ebookGroup.setVisible(!physical);
            ebookGroup.setManaged(!physical);
        });
        if ("EBook".equals(typeCombo.getValue())) {
            physicalGroup.setVisible(false);
            physicalGroup.setManaged(false);
            ebookGroup.setVisible(true);
            ebookGroup.setManaged(true);
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));
        int row = 0;
        grid.addRow(row++, new Label("Type"), typeCombo);
        grid.addRow(row++, new Label("Title"), titleField);
        grid.addRow(row++, new Label("Author"), authorField);
        grid.addRow(row++, new Label("ISBN"), isbnField);
        grid.addRow(row++, new Label("Genre"), genreField);
        grid.add(physicalGroup, 0, row, 2, 1);
        grid.add(ebookGroup, 0, row, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) {
                return;
            }
            try {
                Book book;
                if ("Physical".equals(typeCombo.getValue())) {
                    int totalCopies = parseIntOrThrow(totalCopiesField.getText(), "Total copies");
                    int availableCopies = parseIntOrThrow(availableCopiesField.getText(), "Available copies");
                    PhysicalBook pb = new PhysicalBook(isEdit ? existing.getId() : 0,
                            titleField.getText().trim(), authorField.getText().trim(),
                            isbnField.getText().trim(), genreField.getText().trim(),
                            totalCopies, availableCopies);
                    book = pb;
                } else {
                    int maxLicenses = parseIntOrThrow(maxLicensesField.getText(), "Max concurrent licenses");
                    int activeLoans = parseIntOrThrow(activeLoansField.getText(), "Active loans");
                    EBook eb = new EBook(isEdit ? existing.getId() : 0,
                            titleField.getText().trim(), authorField.getText().trim(),
                            isbnField.getText().trim(), genreField.getText().trim(),
                            downloadUrlField.getText().trim(), maxLicenses, activeLoans);
                    book = eb;
                }

                if (isEdit) {
                    bookService.updateBook(book);
                } else {
                    bookService.addBook(book);
                }
                reload();
            } catch (IllegalArgumentException e) {
                showError("Validation Error", e.getMessage());
            } catch (SQLException e) {
                showError("Database Error", "Failed to save book: " + e.getMessage());
            }
        });
    }

    private int parseIntOrThrow(String text, String fieldName) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a whole number.");
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
}
