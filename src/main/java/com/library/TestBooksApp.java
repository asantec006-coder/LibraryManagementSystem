package com.library;

import com.library.repository.BookRepository;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Standalone launcher used ONLY to test the Books module before the
 * Login/Dashboard screens exist. Once the Dashboard module is built,
 * Books.fxml will be loaded from there instead and this class can be
 * deleted.
 */
public class TestBooksApp extends Application {

    private static final Logger LOGGER = Logger.getLogger(TestBooksApp.class.getName());

    @Override
    public void start(Stage stage) throws Exception {
        // Make sure the table exists before the UI tries to read from it.
        try {
            new BookRepository().createTableIfNotExists();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to create books table", e);
        }

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Books.fxml"));
        stage.setTitle("Library — Books Module Test");
        stage.setScene(new Scene(root, 900, 600));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
