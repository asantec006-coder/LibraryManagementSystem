package com.library.controller;

import com.library.model.AdminUser;
import com.library.repository.AdminUserRepository;
import com.library.service.AuthService;
import com.library.util.Navigator;
import com.library.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Controller for Login.fxml. Authenticates against the admin_users
 * table via AuthService (bcrypt password check) instead of a
 * hard-coded demo credential pair.
 */
public class LoginController {

    private final AuthService authService = new AuthService(new AdminUserRepository());

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheck;
    @FXML private Label errorLabel;

    @FXML
    private void onSignIn() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        try {
            Optional<AdminUser> user = authService.login(email, password);
            if (user.isPresent()) {
                hideError();
                Session.setCurrentUser(user.get());
                Navigator.goTo("/fxml/Dashboard.fxml");
            } else {
                showError("Invalid email or password. Try the demo credentials below.");
            }
        } catch (SQLException e) {
            showError("Could not reach the database: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
