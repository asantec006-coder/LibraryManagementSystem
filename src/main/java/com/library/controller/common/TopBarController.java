package com.library.controller.common;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Controller for the shared TopBar.fxml. Each page calls setTitle(...)
 * after including the top bar, e.g.:
 * topBarController.setTitle("Books", "Manage your library collection");
 */
public class TopBarController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;

    public void setTitle(String title, String subtitle) {
        titleLabel.setText(title);
        subtitleLabel.setText(subtitle);
    }
}
