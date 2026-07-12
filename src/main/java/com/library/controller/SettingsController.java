package com.library.controller;

import com.library.controller.common.SidebarController;
import com.library.controller.common.TopBarController;
import com.library.repository.LibrarySettingsRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.Map;

/**
 * Controller for Settings.fxml.
 *
 * General tab reads/writes real values via LibrarySettingsRepository — an
 * unconfigured library shows blank fields with placeholder examples rather
 * than someone else's fabricated name/address. Notifications/Security/Backup
 * show an honest "not built yet" panel rather than doing nothing, so
 * clicking them is visibly responsive instead of looking broken.
 */
public class SettingsController {

    @FXML private SidebarController sidebarController;
    @FXML private TopBarController topBarController;

    @FXML private Button generalTabBtn;
    @FXML private Button notificationsTabBtn;
    @FXML private Button securityTabBtn;
    @FXML private Button backupTabBtn;

    @FXML private VBox generalPanel;
    @FXML private VBox notificationsPanel;
    @FXML private VBox securityPanel;
    @FXML private VBox backupPanel;

    @FXML private TextField libraryNameField;
    @FXML private TextField addressField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;

    private final LibrarySettingsRepository settingsRepository = new LibrarySettingsRepository();

    @FXML
    public void initialize() {
        sidebarController.setActive(SidebarController.NavItem.SETTINGS);
        topBarController.setTitle("Settings", "System configuration");
        loadSettings();
    }

    private void loadSettings() {
        try {
            Map<String, String> settings = settingsRepository.getAll();
            libraryNameField.setText(settings.getOrDefault("library_name", ""));
            addressField.setText(settings.getOrDefault("address", ""));
            emailField.setText(settings.getOrDefault("email", ""));
            phoneField.setText(settings.getOrDefault("phone", ""));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load settings: " + e.getMessage());
        }
    }

    @FXML
    private void showGeneral() {
        setActive(generalTabBtn, generalPanel);
    }

    @FXML
    private void showNotifications() {
        setActive(notificationsTabBtn, notificationsPanel);
    }

    @FXML
    private void showSecurity() {
        setActive(securityTabBtn, securityPanel);
    }

    @FXML
    private void showBackup() {
        setActive(backupTabBtn, backupPanel);
    }

    private void setActive(Button activeButton, VBox activePanel) {
        for (Button b : new Button[]{generalTabBtn, notificationsTabBtn, securityTabBtn, backupTabBtn}) {
            b.getStyleClass().remove("settings-tab-active");
        }
        activeButton.getStyleClass().add("settings-tab-active");

        for (VBox panel : new VBox[]{generalPanel, notificationsPanel, securityPanel, backupPanel}) {
            panel.setVisible(false);
            panel.setManaged(false);
        }
        activePanel.setVisible(true);
        activePanel.setManaged(true);
    }

    @FXML
    private void onSave() {
        try {
            settingsRepository.saveAll(Map.of(
                    "library_name", libraryNameField.getText() == null ? "" : libraryNameField.getText().trim(),
                    "address", addressField.getText() == null ? "" : addressField.getText().trim(),
                    "email", emailField.getText() == null ? "" : emailField.getText().trim(),
                    "phone", phoneField.getText() == null ? "" : phoneField.getText().trim()
            ));
            showAlert(Alert.AlertType.INFORMATION, "Saved", "Settings saved successfully.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not save settings: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
