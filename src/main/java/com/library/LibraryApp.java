package com.library;

import com.library.util.Navigator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

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

    // Below this, some layouts (e.g. two-column screens) will show a
    // horizontal scrollbar instead of squeezing content unreadably thin.
    private static final double MIN_WIDTH = 1024;
    private static final double MIN_HEIGHT = 700;

    @Override
    public void start(Stage stage) throws Exception {
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

    public static void main(String[] args) {
        launch(args);
    }
}
