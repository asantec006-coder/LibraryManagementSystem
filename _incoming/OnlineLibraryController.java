package com.library.controller;

import com.library.controller.common.SidebarController;
import com.library.model.DownloadedBook;
import com.library.repository.DownloadedBookRepository;
import com.library.service.online.OnlineBook;
import com.library.service.online.OnlineLibraryService;
import javafx.application.Platform;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;

/**
 * Controller for OnlineLibrary.fxml ("Discover"-style cover grid). All the
 * search/download/open-local/read-online logic is unchanged from before —
 * only how results are displayed and selected has changed: instead of a
 * ListView<OnlineBook>, results render as clickable cover cards in a
 * FlowPane, and the previous "selected item" concept is now a plain
 * `selectedBook` field set when a card is clicked.
 */
public class OnlineLibraryController {

    @FXML private SidebarController sidebarController;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sourceFilterCombo;
    @FXML private FlowPane resultsGrid;
    @FXML private Label statusLabel;

    @FXML private VBox detailBar;
    @FXML private ImageView coverImageView;
    @FXML private Label titleLabel;
    @FXML private Label authorLabel;
    @FXML private Label yearLabel;
    @FXML private Label sourceLabel;

    @FXML private Button downloadBtn;
    @FXML private Button readOnlineBtn;
    @FXML private Button openLocalBtn;

    @FXML private VBox progressBox;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;

    private OnlineLibraryService onlineService;
    private DownloadedBookRepository downloadedBookRepo;
    private DownloadedBook currentDownloadedBook; // Tracks if the selected book is already downloaded

    private List<OnlineBook> lastResults = List.of();
    private OnlineBook selectedBook;
    private VBox selectedCard;

    @FXML
    public void initialize() {
        if (sidebarController != null) {
            sidebarController.setActive(SidebarController.NavItem.ONLINE_LIBRARY);
        }

        downloadedBookRepo = new DownloadedBookRepository();
        onlineService = new OnlineLibraryService(downloadedBookRepo);

        try {
            downloadedBookRepo.createTableIfNotExists();
        } catch (SQLException e) {
            showError("Database Error", "Could not initialize downloaded_books table.");
        }

        sourceFilterCombo.setItems(FXCollections.observableArrayList("All Sources", "Open Library", "Project Gutenberg"));
        sourceFilterCombo.getSelectionModel().select("All Sources");
        sourceFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> renderResultsGrid(lastResults));

        clearDetails();
    }

    @FXML
    public void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        statusLabel.setText("Searching...");
        resultsGrid.getChildren().clear();
        clearDetails();

        // Run search asynchronously
        onlineService.searchAll(query).thenAccept(results -> {
            Platform.runLater(() -> {
                lastResults = results;
                renderResultsGrid(lastResults);
                statusLabel.setText("Found " + results.size() + " results.");
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> statusLabel.setText("Error occurred during search."));
            return null;
        });
    }

    /** Rebuilds the card grid from lastResults, applying the source filter. */
    private void renderResultsGrid(List<OnlineBook> results) {
        resultsGrid.getChildren().clear();

        String filter = sourceFilterCombo.getValue();
        List<OnlineBook> filtered = results.stream()
                .filter(b -> "All Sources".equals(filter) || filter == null || filter.equalsIgnoreCase(b.getSource()))
                .toList();

        if (filtered.isEmpty()) {
            Label empty = new Label(results.isEmpty() ? "Search for a title or author to get started."
                    : "No results from this source.");
            empty.getStyleClass().add("section-subtitle");
            resultsGrid.getChildren().add(empty);
            return;
        }

        for (OnlineBook book : filtered) {
            resultsGrid.getChildren().add(buildCoverCard(book));
        }
    }

    private VBox buildCoverCard(OnlineBook book) {
        ImageView cover = new ImageView(com.library.util.CoverImageLoader.loadFromUrl(book.getCoverUrl()));
        cover.setFitWidth(140);
        cover.setFitHeight(200);
        cover.setPreserveRatio(false);
        cover.setStyle("-fx-effect: dropshadow(gaussian, rgba(16,24,64,0.12), 8, 0, 0, 2);");

        Label title = new Label(book.getTitle());
        title.getStyleClass().add("detail-title");
        title.setStyle("-fx-font-size: 13px;");
        title.setWrapText(true);
        title.setMaxWidth(140);

        Label author = new Label(book.getAuthor());
        author.getStyleClass().add("detail-author");
        author.setStyle("-fx-font-size: 11px;");
        author.setWrapText(true);
        author.setMaxWidth(140);

        VBox card = new VBox(8, cover, title, author);
        card.setMaxWidth(140);
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle("-fx-cursor: hand; -fx-padding: 6; -fx-background-radius: 10px;");
        card.setOnMouseClicked(e -> selectCard(card, book));
        return card;
    }

    private void selectCard(VBox card, OnlineBook book) {
        if (selectedCard != null) {
            selectedCard.setStyle("-fx-cursor: hand; -fx-padding: 6; -fx-background-radius: 10px;");
        }
        card.setStyle("-fx-cursor: hand; -fx-padding: 6; -fx-background-radius: 10px; "
                + "-fx-background-color: #eef1ff; -fx-border-color: #3b5bfd; -fx-border-width: 2px; -fx-border-radius: 10px;");
        selectedCard = card;
        selectedBook = book;
        displayBookDetails(book);
    }

    private void displayBookDetails(OnlineBook book) {
        if (book == null) {
            clearDetails();
            return;
        }

        detailBar.setVisible(true);
        detailBar.setManaged(true);

        titleLabel.setText(book.getTitle());
        authorLabel.setText(book.getAuthor());
        yearLabel.setText(book.getPublicationYear() != null ? "Published: " + book.getPublicationYear() : "");
        sourceLabel.setText("Source: " + book.getSource());

        // Load cover image (default.png if there's no cover URL or it fails to load — this must never crash)
        Image image = com.library.util.CoverImageLoader.loadFromUrl(book.getCoverUrl());
        coverImageView.setImage(image);
        image.errorProperty().addListener((obs, wasError, isError) -> {
            if (isError) {
                Platform.runLater(() -> coverImageView.setImage(com.library.util.CoverImageLoader.load((String) null)));
            }
        });

        // Configure buttons
        readOnlineBtn.setDisable(book.getReadOnlineUrl() == null);

        // Check if we already have it downloaded
        try {
            currentDownloadedBook = downloadedBookRepo.findByDownloadUrl(book.getDownloadUrl()).orElse(null);
        } catch (SQLException e) {
            currentDownloadedBook = null;
        }

        if (currentDownloadedBook != null) {
            // Already downloaded
            downloadBtn.setVisible(false);
            downloadBtn.setManaged(false);
            openLocalBtn.setVisible(true);
            openLocalBtn.setManaged(true);
        } else {
            // Not downloaded
            boolean canDownload = book.getDownloadUrl() != null;
            downloadBtn.setVisible(canDownload);
            downloadBtn.setManaged(canDownload);
            downloadBtn.setDisable(false);
            openLocalBtn.setVisible(false);
            openLocalBtn.setManaged(false);
        }
    }

    private void clearDetails() {
        selectedBook = null;
        if (selectedCard != null) {
            selectedCard.setStyle("-fx-cursor: hand; -fx-padding: 6; -fx-background-radius: 10px;");
            selectedCard = null;
        }
        detailBar.setVisible(false);
        detailBar.setManaged(false);
        titleLabel.setText("");
        authorLabel.setText("");
        yearLabel.setText("");
        sourceLabel.setText("");
        coverImageView.setImage(com.library.util.CoverImageLoader.load((String) null));
        downloadBtn.setDisable(true);
        readOnlineBtn.setDisable(true);
        openLocalBtn.setVisible(false);
        openLocalBtn.setManaged(false);
        progressBox.setVisible(false);
        progressBox.setManaged(false);
    }

    @FXML
    public void handleDownload() {
        if (selectedBook == null || selectedBook.getDownloadUrl() == null) return;

        downloadBtn.setDisable(true);
        progressBox.setVisible(true);
        progressBox.setManaged(true);
        progressBar.setProgress(0);
        progressLabel.setText("Downloading...");

        onlineService.downloadBook(selectedBook, progress -> {
            Platform.runLater(() -> {
                if (progress >= 0) {
                    progressBar.setProgress(progress);
                }
            });
        }).thenAccept(downloadedBook -> {
            Platform.runLater(() -> {
                progressLabel.setText("Download Complete!");
                currentDownloadedBook = downloadedBook;

                // Switch buttons
                downloadBtn.setVisible(false);
                downloadBtn.setManaged(false);
                openLocalBtn.setVisible(true);
                openLocalBtn.setManaged(true);
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                progressLabel.setText("Download Failed.");
                downloadBtn.setDisable(false);
                showError("Download Error", ex.getMessage());
            });
            return null;
        });
    }

    @FXML
    public void handleReadOnline() {
        if (selectedBook == null || selectedBook.getReadOnlineUrl() == null) return;

        try {
            Desktop.getDesktop().browse(new URI(selectedBook.getReadOnlineUrl()));
        } catch (Exception e) {
            showError("Error", "Could not open browser: " + e.getMessage());
        }
    }

    @FXML
    public void handleOpenLocal() {
        if (currentDownloadedBook == null) return;

        File file = new File(currentDownloadedBook.getLocalPath());
        if (file.exists()) {
            try {
                Desktop.getDesktop().open(file);
            } catch (Exception e) {
                showError("Error", "Could not open file: " + e.getMessage());
            }
        } else {
            showError("File Not Found", "The downloaded file could not be found at " + file.getAbsolutePath());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
