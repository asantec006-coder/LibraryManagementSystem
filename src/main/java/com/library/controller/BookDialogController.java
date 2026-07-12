package com.library.controller;

import com.library.model.Book;
import com.library.model.EBook;
import com.library.model.PhysicalBook;
import com.library.repository.BookRepository;
import com.library.service.BookService;
import com.library.service.cover.CoverImageService;
import com.library.util.CoverImageLoader;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for BookDialog.fxml. Handles both the book detail fields and
 * the cover preview/search/upload/remove workflow. Cover search runs on a
 * background thread (CompletableFuture, matching the pattern already used
 * by OnlineLibraryService) so the dialog never freezes while waiting on a
 * network call.
 */
public class BookDialogController {

    private final CoverImageService coverImageService = new CoverImageService(new BookService(new BookRepository()));

    @FXML private ImageView coverImageView;
    @FXML private ProgressIndicator coverSpinner;
    @FXML private Label coverStatusLabel;
    @FXML private Button searchCoverBtn;
    @FXML private Button uploadCoverBtn;
    @FXML private Button removeCoverBtn;

    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField isbnField;
    @FXML private TextField genreField;

    @FXML private VBox physicalGroup;
    @FXML private TextField totalCopiesField;
    @FXML private TextField availableCopiesField;

    @FXML private VBox ebookGroup;
    @FXML private TextField downloadUrlField;
    @FXML private TextField maxLicensesField;
    @FXML private TextField activeLoansField;

    @FXML private Label errorLabel;

    private Book existingBook;
    /** Cover path chosen so far in this dialog session (found, uploaded, or carried over from the existing book) — null means no cover. */
    private String pendingCoverPath;
    private boolean searchInFlight;

    @FXML
    public void initialize() {
        typeCombo.getSelectionModel().select("Physical");
        typeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean physical = "Physical".equals(newVal);
            physicalGroup.setVisible(physical);
            physicalGroup.setManaged(physical);
            ebookGroup.setVisible(!physical);
            ebookGroup.setManaged(!physical);
        });

        // Auto-search once the librarian finishes typing an ISBN or a title (spec: "After the ISBN or
        // Title is entered, automatically search"). Only fires if there's no cover chosen yet, so it
        // doesn't clobber a manual upload or a cover found moments earlier.
        isbnField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && pendingCoverPath == null) {
                triggerAutoSearch();
            }
        });
        titleField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && pendingCoverPath == null) {
                triggerAutoSearch();
            }
        });

        showCoverPreview(null);
        removeCoverBtn.setDisable(true);
    }

    public void setBook(Book book) {
        this.existingBook = book;
        if (book == null) {
            return;
        }

        typeCombo.getSelectionModel().select(book instanceof PhysicalBook ? "Physical" : "EBook");
        typeCombo.setDisable(true); // changing type of an existing book is not supported

        titleField.setText(book.getTitle());
        authorField.setText(book.getAuthor());
        isbnField.setText(book.getIsbn());
        genreField.setText(book.getGenre());

        if (book instanceof PhysicalBook pb) {
            totalCopiesField.setText(String.valueOf(pb.getTotalCopies()));
            availableCopiesField.setText(String.valueOf(pb.getAvailableCopies()));
        } else if (book instanceof EBook eb) {
            downloadUrlField.setText(eb.getDownloadUrl());
            maxLicensesField.setText(String.valueOf(eb.getMaxConcurrentLicenses()));
            activeLoansField.setText(String.valueOf(eb.getActiveLoans()));
        }

        pendingCoverPath = book.getCoverImage();
        showCoverPreview(pendingCoverPath);
    }

    public Book getBook() {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String author = authorField.getText() == null ? "" : authorField.getText().trim();
        String isbn = isbnField.getText() == null ? "" : isbnField.getText().trim();
        String genre = genreField.getText() == null ? "" : genreField.getText().trim();

        if (title.isEmpty() || author.isEmpty()) {
            throw new IllegalArgumentException("Title and author are required.");
        }
        if (isbn.isEmpty()) {
            // ISBN is optional per the form, but the database requires a unique, non-null value —
            // generate a stable placeholder so this book doesn't collide with any other blank-ISBN entry.
            isbn = "NOISBN-" + System.currentTimeMillis();
        }

        int id = existingBook != null ? existingBook.getId() : 0;
        Book book;
        if ("Physical".equals(typeCombo.getValue())) {
            int totalCopies = parseIntOrThrow(totalCopiesField.getText(), "Total copies");
            int availableCopies = parseIntOrThrow(availableCopiesField.getText(), "Available copies");
            book = new PhysicalBook(id, title, author, isbn, genre, totalCopies, availableCopies);
        } else {
            int maxLicenses = parseIntOrThrow(maxLicensesField.getText(), "Max concurrent licenses");
            int activeLoans = parseIntOrThrow(activeLoansField.getText(), "Active loans");
            book = new EBook(id, title, author, isbn, genre,
                    downloadUrlField.getText() == null ? "" : downloadUrlField.getText().trim(),
                    maxLicenses, activeLoans);
        }
        book.setCoverImage(pendingCoverPath);
        return book;
    }

    private int parseIntOrThrow(String text, String fieldName) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a whole number.");
        }
    }

    private void triggerAutoSearch() {
        String isbn = isbnField.getText() == null ? "" : isbnField.getText().trim();
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        if (isbn.isEmpty() && title.isEmpty()) {
            return;
        }
        runSearch();
    }

    @FXML
    private void onSearchCover() {
        runSearch();
    }

    private void runSearch() {
        if (searchInFlight) {
            return;
        }
        String isbn = isbnField.getText() == null ? "" : isbnField.getText().trim();
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String author = authorField.getText() == null ? "" : authorField.getText().trim();
        if (isbn.isEmpty() && title.isEmpty()) {
            coverStatusLabel.setText("Enter a title or ISBN first.");
            return;
        }

        int keyId = existingBook != null ? existingBook.getId() : (int) (System.nanoTime() & 0x7FFFFFFF);

        searchInFlight = true;
        setSearching(true);
        coverStatusLabel.setText("Searching...");

        CompletableFuture
                .supplyAsync(() -> coverImageService.searchAndDownloadCover(isbn, title, author, keyId))
                .thenAccept(result -> Platform.runLater(() -> {
                    searchInFlight = false;
                    setSearching(false);
                    if (result.found()) {
                        pendingCoverPath = result.coverPath();
                        showCoverPreview(pendingCoverPath);
                        coverStatusLabel.setText("Cover found (" + result.source() + ")");
                    } else {
                        coverStatusLabel.setText("No Cover Found — you can upload one instead.");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        searchInFlight = false;
                        setSearching(false);
                        coverStatusLabel.setText("No Cover Found — you can upload one instead.");
                    });
                    return null;
                });
    }

    @FXML
    private void onUploadCover() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Cover Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images", "*.jpg", "*.jpeg", "*.png", "*.webp"));
        File selected = chooser.showOpenDialog(uploadCoverBtn.getScene().getWindow());
        if (selected == null) {
            return;
        }

        String isbn = isbnField.getText() == null ? "" : isbnField.getText().trim();
        int keyId = existingBook != null ? existingBook.getId() : (int) (System.nanoTime() & 0x7FFFFFFF);

        try {
            pendingCoverPath = coverImageService.saveUploadedCover(selected, isbn, keyId);
            showCoverPreview(pendingCoverPath);
            coverStatusLabel.setText("Cover Uploaded");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (java.io.IOException e) {
            showError("Could not save the uploaded image: " + e.getMessage());
        }
    }

    @FXML
    private void onRemoveCover() {
        pendingCoverPath = null;
        showCoverPreview(null);
        coverStatusLabel.setText("");
    }

    private void showCoverPreview(String relativePath) {
        coverImageView.setImage(CoverImageLoader.load(relativePath));
        removeCoverBtn.setDisable(relativePath == null || relativePath.isBlank());
    }

    private void setSearching(boolean searching) {
        coverSpinner.setVisible(searching);
        coverSpinner.setManaged(searching);
        searchCoverBtn.setDisable(searching);
        uploadCoverBtn.setDisable(searching);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
