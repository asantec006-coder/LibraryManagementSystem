package com.library.controller;

import com.library.controller.common.SidebarController;
import com.library.model.DownloadedBook;
import com.library.repository.DownloadedBookRepository;
import com.library.service.online.OnlineBook;
import com.library.service.online.OnlineLibraryService;
import javafx.application.Platform;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.sql.SQLException;

public class OnlineLibraryController {

    @FXML private SidebarController sidebarController;

    @FXML private TextField searchField;
    @FXML private ListView<OnlineBook> resultsListView;
    @FXML private Label statusLabel;

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

        resultsListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(OnlineBook item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTitle() + " - " + item.getAuthor());
                }
            }
        });

        resultsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            displayBookDetails(newVal);
        });

        clearDetails();
    }

    @FXML
    public void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        statusLabel.setText("Searching...");
        resultsListView.getItems().clear();
        clearDetails();

        // Run search asynchronously
        onlineService.searchAll(query).thenAccept(results -> {
            Platform.runLater(() -> {
                resultsListView.getItems().setAll(results);
                statusLabel.setText("Found " + results.size() + " results.");
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> statusLabel.setText("Error occurred during search."));
            return null;
        });
    }

    private void displayBookDetails(OnlineBook book) {
        if (book == null) {
            clearDetails();
            return;
        }

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
        OnlineBook selected = resultsListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getDownloadUrl() == null) return;

        downloadBtn.setDisable(true);
        progressBox.setVisible(true);
        progressBox.setManaged(true);
        progressBar.setProgress(0);
        progressLabel.setText("Downloading...");

        onlineService.downloadBook(selected, progress -> {
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
        OnlineBook selected = resultsListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getReadOnlineUrl() == null) return;

        try {
            Desktop.getDesktop().browse(new URI(selected.getReadOnlineUrl()));
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
