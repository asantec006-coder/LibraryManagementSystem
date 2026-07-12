package com.library;

import com.library.repository.AdminUserRepository;
import com.library.repository.BookRepository;
import com.library.repository.DownloadedBookRepository;
import com.library.repository.LibrarySettingsRepository;
import com.library.repository.LoanRepository;
import com.library.repository.MemberRepository;
import com.library.service.cover.ImageCacheManager;
import com.library.util.Navigator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for the Library Management System UI.
 * Starts on the Login screen; the shared Sidebar (via Navigator) handles
 * moving between the other screens from there.
 *
 * The window is sized from the actual screen's visible bounds (not a
 * hard-coded 1440x900) and opens maximized, so it fits laptops, external
 * monitors, and anything in between without clipping content.
 */
public class LibraryApp extends Application {

    private static final Logger LOGGER = Logger.getLogger(LibraryApp.class.getName());

    // Below this, some layouts (e.g. two-column screens) will show a
    // horizontal scrollbar instead of squeezing content unreadably thin.
    private static final double MIN_WIDTH = 1024;
    private static final double MIN_HEIGHT = 700;

    @Override
    public void start(Stage stage) throws Exception {
        try {
            initDatabase();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize the database", e);
            new Alert(Alert.AlertType.ERROR,
                    "Could not set up the database:\n" + e.getMessage()).showAndWait();
            return;
        }

        Navigator.init(stage);

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
        stage.setTitle("LibraryMS — Management System");
        stage.setScene(new Scene(root, screenBounds.getWidth(), screenBounds.getHeight()));
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.setMaximized(true);
        stage.show();
    }

    /** Creates every table the app needs (idempotent — safe to call on every launch). */
    private void initDatabase() throws SQLException {
        new BookRepository().createTableIfNotExists();
        new MemberRepository().createTableIfNotExists();
        new LoanRepository().createTableIfNotExists();
        new AdminUserRepository().createTableIfNotExists();
        new DownloadedBookRepository().createTableIfNotExists();
        new LibrarySettingsRepository().createTableIfNotExists();
        new ImageCacheManager().init();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
