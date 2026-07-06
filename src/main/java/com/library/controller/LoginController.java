package com.library.controller;

import com.library.util.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for Login.fxml. This is a visual/static implementation:
 * it checks against the hard-coded demo credentials shown on screen.
 * When the Authentication Module is built, onSignIn() should delegate
 * to AuthenticationService.login(email, password) instead.
 */
public class LoginController {

    private static final String DEMO_EMAIL = "admin@library.com";
    private static final String DEMO_PASSWORD = "password";

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheck;
    @FXML private Label errorLabel;

    @FXML
    private void onSignIn() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (DEMO_EMAIL.equalsIgnoreCase(email) && DEMO_PASSWORD.equals(password)) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            Navigator.goTo("/fxml/Dashboard.fxml");
        } else {
            errorLabel.setText("Invalid email or password. Try the demo credentials below.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }
}
