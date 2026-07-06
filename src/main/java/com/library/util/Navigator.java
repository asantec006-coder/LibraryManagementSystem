package com.library.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Very small navigation helper for the visual prototype stage of the UI.
 * Each screen is still its own top-level FXML (with its own <fx:include>
 * of the shared Sidebar/TopBar), so "navigating" just means loading a
 * different FXML into the same Stage.
 *
 * Once real controllers/services are wired module-by-module, this can be
 * replaced with a proper single-shell + swappable-content-pane approach
 * if desired — nothing else in the screens depends on how navigation is
 * implemented internally.
 */
public final class Navigator {

    private static Stage primaryStage;

    private Navigator() {
    }

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    /**
     * @param fxmlPath path relative to the classpath root, e.g. "/fxml/Dashboard.fxml"
     */
    public static void goTo(String fxmlPath) {
        if (primaryStage == null) {
            throw new IllegalStateException("Navigator.init(stage) was never called.");
        }
        try {
            boolean wasMaximized = primaryStage.isMaximized();
            double width = primaryStage.getScene() != null ? primaryStage.getScene().getWidth() : 1280;
            double height = primaryStage.getScene() != null ? primaryStage.getScene().getHeight() : 800;

            Parent root = FXMLLoader.load(Navigator.class.getResource(fxmlPath));
            primaryStage.setScene(new Scene(root, width, height));

            // setScene can drop the maximized flag on some platforms — restore it.
            if (wasMaximized) {
                primaryStage.setMaximized(true);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load screen: " + fxmlPath, e);
        }
    }
}
