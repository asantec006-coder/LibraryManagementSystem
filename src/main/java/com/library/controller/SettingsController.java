package com.library.controller;

import com.library.controller.common.SidebarController;
import com.library.controller.common.TopBarController;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;

/**
 * Controller for Settings.fxml. Only the "General" tab has real content,
 * matching the Figma reference. The other tabs just highlight themselves
 * and show a "coming soon" note — build their panels out when Settings
 * becomes a real module.
 */
public class SettingsController {

    @FXML private SidebarController sidebarController;
    @FXML private TopBarController topBarController;

    @FXML private Button generalTabBtn;
    @FXML private Button notificationsTabBtn;
    @FXML private Button securityTabBtn;
    @FXML private Button backupTabBtn;

    @FXML
    public void initialize() {
        sidebarController.setActive(SidebarController.NavItem.SETTINGS);
        topBarController.setTitle("Settings", "System configuration");
    }

    @FXML
    private void showGeneral() {
        setActiveTab(generalTabBtn);
    }

    @FXML
    private void showNotifications() {
        setActiveTab(notificationsTabBtn);
    }

    @FXML
    private void showSecurity() {
        setActiveTab(securityTabBtn);
    }

    @FXML
    private void showBackup() {
        setActiveTab(backupTabBtn);
    }

    private void setActiveTab(Button active) {
        for (Button b : new Button[]{generalTabBtn, notificationsTabBtn, securityTabBtn, backupTabBtn}) {
            b.getStyleClass().remove("settings-tab-active");
        }
        active.getStyleClass().add("settings-tab-active");
    }

    @FXML
    private void onSave() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Settings saved successfully.",
                ButtonType.OK);
        alert.setHeaderText("Success");
        alert.showAndWait();
    }
}
