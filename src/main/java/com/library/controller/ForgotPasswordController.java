package com.library.controller;

import com.library.model.AdminUser;
import com.library.repository.AdminUserRepository;
import com.library.util.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Random;

public class ForgotPasswordController {

    @FXML private VBox emailPanel;
    @FXML private VBox codePanel;
    @FXML private VBox resetPanel;

    @FXML private TextField emailField;
    @FXML private TextField codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;

    private final AdminUserRepository adminUserRepository = new AdminUserRepository();
    private String generatedCode;
    private AdminUser targetUser;

    @FXML
    public void initialize() {
        showPanel(emailPanel);
    }

    @FXML
    private void onSendCode() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        if (email.isEmpty()) {
            showError("Please enter your email address.");
            return;
        }

        try {
            Optional<AdminUser> user = adminUserRepository.findByUsername(email);
            if (user.isEmpty()) {
                // To prevent email enumeration, we should theoretically show success anyway,
                // but for this offline demo, showing an error helps the user know they typed it wrong.
                showError("No account found with that email address.");
                return;
            }

            targetUser = user.get();
            
            // Generate a random 6-digit code
            generatedCode = String.format("%06d", new Random().nextInt(999999));
            
            hideError();
            showPanel(codePanel);

            // Simulate email delivery by showing the code in a popup
            Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                "DEMO MODE: An email would normally be sent to " + email + ".\n\n" +
                "Your recovery code is: " + generatedCode, 
                ButtonType.OK);
            alert.setHeaderText("Recovery Code Generated");
            alert.showAndWait();

        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void onVerifyCode() {
        String code = codeField.getText() == null ? "" : codeField.getText().trim();
        if (code.isEmpty()) {
            showError("Please enter the 6-digit code.");
            return;
        }

        if (code.equals(generatedCode)) {
            hideError();
            showPanel(resetPanel);
        } else {
            showError("Invalid code. Please try again.");
        }
    }

    @FXML
    private void onResetPassword() {
        String newPwd = newPasswordField.getText();
        String confirmPwd = confirmPasswordField.getText();

        if (newPwd == null || newPwd.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (!newPwd.equals(confirmPwd)) {
            showError("Passwords do not match.");
            return;
        }

        try {
            String hash = BCrypt.hashpw(newPwd, BCrypt.gensalt());
            adminUserRepository.updatePassword(targetUser.getId(), hash);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                "Your password has been successfully reset. You can now log in.", 
                ButtonType.OK);
            alert.setHeaderText("Password Reset Successful");
            alert.showAndWait();

            Navigator.goTo("/fxml/Login.fxml");
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void onBackToLogin() {
        Navigator.goTo("/fxml/Login.fxml");
    }

    private void showPanel(VBox panel) {
        emailPanel.setVisible(false); emailPanel.setManaged(false);
        codePanel.setVisible(false); codePanel.setManaged(false);
        resetPanel.setVisible(false); resetPanel.setManaged(false);

        panel.setVisible(true);
        panel.setManaged(true);
    }

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #ef4444; -fx-background-color: #fee2e2; -fx-padding: 8 12; -fx-background-radius: 4;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void hideError() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }
}
