package com.library.controller;

import com.library.controller.common.SidebarController;
import com.library.controller.common.TopBarController;
import com.library.db.DatabaseConnection;
import com.library.model.AdminUser;
import com.library.repository.AdminUserRepository;
import com.library.repository.LibrarySettingsRepository;
import com.library.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Controller for Settings.fxml.
 *
 * - General:       reads/writes library info via LibrarySettingsRepository.
 * - Notifications: saves checkbox preferences via the same settings table.
 * - Security:      change password + update profile (real bcrypt hashing).
 * - Backup:        export DB as SQL/CSV, restore from backup, reset data.
 */
public class SettingsController {

    public static String initialTab = "General";

    /* ── Shared sub-controllers ────────────────────────────── */
    @FXML private SidebarController sidebarController;
    @FXML private TopBarController topBarController;

    /* ── Tab buttons ───────────────────────────────────────── */
    @FXML private Button generalTabBtn;
    @FXML private Button notificationsTabBtn;
    @FXML private Button securityTabBtn;
    @FXML private Button backupTabBtn;

    /* ── Panels ────────────────────────────────────────────── */
    @FXML private VBox generalPanel;
    @FXML private VBox notificationsPanel;
    @FXML private VBox securityPanel;
    @FXML private VBox backupPanel;

    /* ── General fields ────────────────────────────────────── */
    @FXML private TextField libraryNameField;
    @FXML private TextField addressField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;

    /* ── Notification checkboxes ────────────────────────────── */
    @FXML private CheckBox notifOverdueBooks;
    @FXML private CheckBox notifNewBooks;
    @FXML private CheckBox notifMemberExpiry;
    @FXML private CheckBox notifReturnReminder;
    @FXML private CheckBox notifLowStock;
    @FXML private CheckBox notifSystemUpdates;

    /* ── Security fields ───────────────────────────────────── */
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label passwordStrengthLabel;
    @FXML private TextField usernameDisplayField;
    @FXML private TextField fullNameField;

    /* ── Backup labels ─────────────────────────────────────── */
    @FXML private Label dbSizeLabel;
    @FXML private Label lastBackupLabel;
    @FXML private Label totalRecordsLabel;
    @FXML private Label restoreFileLabel;

    /* ── Repositories ──────────────────────────────────────── */
    private final LibrarySettingsRepository settingsRepository = new LibrarySettingsRepository();
    private final AdminUserRepository adminUserRepository = new AdminUserRepository();

    /* ═══════════════════════════════════════════════════════
       INITIALIZE
       ═══════════════════════════════════════════════════════ */

    @FXML
    public void initialize() {
        sidebarController.setActive(SidebarController.NavItem.SETTINGS);
        topBarController.setTitle("Settings", "System configuration");
        loadGeneralSettings();
        loadNotificationPreferences();
        loadSecurityInfo();
        loadBackupInfo();

        if ("Notifications".equals(initialTab)) {
            showNotifications();
        } else if ("Security".equals(initialTab)) {
            showSecurity();
        } else if ("Backup".equals(initialTab)) {
            showBackup();
        } else {
            showGeneral();
        }
        initialTab = "General"; // reset after use

        // Live password-strength feedback
        newPasswordField.textProperty().addListener((obs, oldVal, newVal) -> updatePasswordStrength(newVal));
    }

    /* ═══════════════════════════════════════════════════════
       TAB SWITCHING
       ═══════════════════════════════════════════════════════ */

    @FXML private void showGeneral()       { setActive(generalTabBtn, generalPanel); }
    @FXML private void showNotifications() { setActive(notificationsTabBtn, notificationsPanel); }
    @FXML private void showSecurity()      { setActive(securityTabBtn, securityPanel); }
    @FXML private void showBackup()        { setActive(backupTabBtn, backupPanel); loadBackupInfo(); }

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

    /* ═══════════════════════════════════════════════════════
       GENERAL TAB
       ═══════════════════════════════════════════════════════ */

    private void loadGeneralSettings() {
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
    private void onSave() {
        try {
            settingsRepository.saveAll(Map.of(
                    "library_name", safe(libraryNameField),
                    "address", safe(addressField),
                    "email", safe(emailField),
                    "phone", safe(phoneField)
            ));
            showAlert(Alert.AlertType.INFORMATION, "Saved", "Settings saved successfully.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not save settings: " + e.getMessage());
        }
    }

    /* ═══════════════════════════════════════════════════════
       NOTIFICATIONS TAB
       ═══════════════════════════════════════════════════════ */

    private void loadNotificationPreferences() {
        try {
            Map<String, String> settings = settingsRepository.getAll();
            notifOverdueBooks.setSelected(!"false".equals(settings.getOrDefault("notif_overdue", "true")));
            notifNewBooks.setSelected(!"false".equals(settings.getOrDefault("notif_new_books", "true")));
            notifMemberExpiry.setSelected(!"false".equals(settings.getOrDefault("notif_member_expiry", "true")));
            notifReturnReminder.setSelected("true".equals(settings.getOrDefault("notif_return_reminder", "false")));
            notifLowStock.setSelected(!"false".equals(settings.getOrDefault("notif_low_stock", "true")));
            notifSystemUpdates.setSelected(!"false".equals(settings.getOrDefault("notif_system_updates", "true")));
        } catch (SQLException e) {
            // Non-critical — just use the FXML defaults
        }
    }

    @FXML
    private void onSaveNotifications() {
        try {
            settingsRepository.saveAll(Map.of(
                    "notif_overdue", String.valueOf(notifOverdueBooks.isSelected()),
                    "notif_new_books", String.valueOf(notifNewBooks.isSelected()),
                    "notif_member_expiry", String.valueOf(notifMemberExpiry.isSelected()),
                    "notif_return_reminder", String.valueOf(notifReturnReminder.isSelected()),
                    "notif_low_stock", String.valueOf(notifLowStock.isSelected()),
                    "notif_system_updates", String.valueOf(notifSystemUpdates.isSelected())
            ));
            showAlert(Alert.AlertType.INFORMATION, "Saved", "Notification preferences saved.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not save preferences: " + e.getMessage());
        }
    }

    @FXML
    private void onResetNotifications() {
        notifOverdueBooks.setSelected(true);
        notifNewBooks.setSelected(true);
        notifMemberExpiry.setSelected(true);
        notifReturnReminder.setSelected(false);
        notifLowStock.setSelected(true);
        notifSystemUpdates.setSelected(true);
    }

    /* ═══════════════════════════════════════════════════════
       SECURITY TAB
       ═══════════════════════════════════════════════════════ */

    private void loadSecurityInfo() {
        AdminUser user = Session.getCurrentUser();
        if (user != null) {
            usernameDisplayField.setText(user.getUsername());
            fullNameField.setText(user.getFullName() != null ? user.getFullName() : "");
        }
    }

    private void updatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            passwordStrengthLabel.setText("");
            return;
        }
        if (password.length() < 6) {
            passwordStrengthLabel.setText("\u26a0 Weak — must be at least 6 characters");
            passwordStrengthLabel.setStyle("-fx-text-fill: #dc2626;");
        } else if (password.length() < 10) {
            passwordStrengthLabel.setText("\u2714 Fair — consider a longer password");
            passwordStrengthLabel.setStyle("-fx-text-fill: #d97706;");
        } else {
            passwordStrengthLabel.setText("\u2714 Strong password");
            passwordStrengthLabel.setStyle("-fx-text-fill: #16a34a;");
        }
    }

    @FXML
    private void onChangePassword() {
        AdminUser user = Session.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No user session found. Please log in again.");
            return;
        }

        String current = currentPasswordField.getText();
        String newPwd = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (current == null || current.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Please enter your current password.");
            return;
        }
        if (newPwd == null || newPwd.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Validation", "New password must be at least 6 characters.");
            return;
        }
        if (!newPwd.equals(confirm)) {
            showAlert(Alert.AlertType.WARNING, "Validation", "New password and confirmation do not match.");
            return;
        }

        // Verify current password
        if (!BCrypt.checkpw(current, user.getPasswordHash())) {
            showAlert(Alert.AlertType.ERROR, "Incorrect Password", "The current password you entered is incorrect.");
            return;
        }

        // Check it's actually different
        if (BCrypt.checkpw(newPwd, user.getPasswordHash())) {
            showAlert(Alert.AlertType.WARNING, "Same Password", "New password must be different from the current one.");
            return;
        }

        try {
            String hash = BCrypt.hashpw(newPwd, BCrypt.gensalt());
            adminUserRepository.updatePassword(user.getId(), hash);
            user.setPasswordHash(hash);

            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            passwordStrengthLabel.setText("");

            showAlert(Alert.AlertType.INFORMATION, "Password Changed",
                    "Your password has been updated successfully.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not update password: " + e.getMessage());
        }
    }

    @FXML
    private void onUpdateProfile() {
        AdminUser user = Session.getCurrentUser();
        if (user == null) return;

        String name = fullNameField.getText() == null ? "" : fullNameField.getText().trim();
        if (name.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Full name cannot be empty.");
            return;
        }

        try {
            adminUserRepository.updateFullName(user.getId(), name);
            user.setFullName(name);
            showAlert(Alert.AlertType.INFORMATION, "Profile Updated", "Your display name has been updated.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not update profile: " + e.getMessage());
        }
    }

    /* ═══════════════════════════════════════════════════════
       BACKUP TAB
       ═══════════════════════════════════════════════════════ */

    private void loadBackupInfo() {
        try {
            // Database file size
            File dbFile = new File("library_prod.db");
            if (dbFile.exists()) {
                long bytes = dbFile.length();
                if (bytes < 1024) {
                    dbSizeLabel.setText(bytes + " B");
                } else if (bytes < 1024 * 1024) {
                    dbSizeLabel.setText(String.format("%.1f KB", bytes / 1024.0));
                } else {
                    dbSizeLabel.setText(String.format("%.1f MB", bytes / (1024.0 * 1024)));
                }
            } else {
                dbSizeLabel.setText("N/A");
            }

            // Last backup time from settings
            Map<String, String> settings = settingsRepository.getAll();
            String lastBackup = settings.getOrDefault("last_backup", null);
            lastBackupLabel.setText(lastBackup != null ? lastBackup : "Never");

            // Count total records across main tables
            int totalRecords = 0;
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement()) {
                for (String table : new String[]{"books", "members", "loans", "admin_users"}) {
                    try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
                        if (rs.next()) totalRecords += rs.getInt(1);
                    } catch (SQLException ignored) {
                        // Table might not exist yet
                    }
                }
            }
            totalRecordsLabel.setText(String.valueOf(totalRecords));

        } catch (SQLException e) {
            // Non-critical — leave default labels
        }
    }

    @FXML
    private void onExportSQL() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Database Backup");
        chooser.setInitialFileName("library_backup_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".db");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database", "*.db"));
        File target = chooser.showSaveDialog(generalPanel.getScene().getWindow());
        if (target == null) return;

        try {
            Path source = Path.of("library_prod.db");
            Files.copy(source, target.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Record the backup time
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            settingsRepository.saveAll(Map.of("last_backup", timestamp));
            lastBackupLabel.setText(timestamp);

            showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                    "Database exported to:\n" + target.getAbsolutePath());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Export Failed", "Could not export database: " + e.getMessage());
        }
    }

    @FXML
    private void onExportCSV() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Data as CSV");
        chooser.setInitialFileName("library_export_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File target = chooser.showSaveDialog(generalPanel.getScene().getWindow());
        if (target == null) return;

        try (PrintWriter writer = new PrintWriter(target);
             Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            String[] tables = {"books", "members", "loans"};
            for (String table : tables) {
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();

                    // Table header
                    writer.println("=== " + table.toUpperCase() + " ===");

                    // Column headers
                    StringBuilder header = new StringBuilder();
                    for (int i = 1; i <= cols; i++) {
                        if (i > 1) header.append(",");
                        header.append(meta.getColumnName(i));
                    }
                    writer.println(header);

                    // Rows
                    while (rs.next()) {
                        StringBuilder row = new StringBuilder();
                        for (int i = 1; i <= cols; i++) {
                            if (i > 1) row.append(",");
                            String val = rs.getString(i);
                            // Escape commas and quotes in CSV
                            if (val != null && (val.contains(",") || val.contains("\""))) {
                                val = "\"" + val.replace("\"", "\"\"") + "\"";
                            }
                            row.append(val != null ? val : "");
                        }
                        writer.println(row);
                    }
                    writer.println();
                } catch (SQLException ignored) {
                    // Table might not exist
                }
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            settingsRepository.saveAll(Map.of("last_backup", timestamp));
            lastBackupLabel.setText(timestamp);

            showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                    "CSV data exported to:\n" + target.getAbsolutePath());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Export Failed", "Could not export CSV: " + e.getMessage());
        }
    }

    @FXML
    private void onRestoreBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Backup File to Restore");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database", "*.db"));
        File source = chooser.showOpenDialog(generalPanel.getScene().getWindow());
        if (source == null) return;

        restoreFileLabel.setText(source.getName());

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "This will replace ALL current data with the backup.\n\n" +
                        "File: " + source.getName() + "\n" +
                        "Size: " + String.format("%.1f KB", source.length() / 1024.0) + "\n\n" +
                        "Are you sure you want to continue?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Database Restore");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    Path target = Path.of("library_prod.db");
                    Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                    showAlert(Alert.AlertType.INFORMATION, "Restore Successful",
                            "Database restored from backup. Please restart the application for changes to take effect.");
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Restore Failed", "Could not restore: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onResetDatabase() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "This will permanently delete ALL books, members, and loan records.\n\n" +
                        "This action CANNOT be undone. You may want to export a backup first.\n\n" +
                        "Are you absolutely sure?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Reset Database — Danger");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try (Connection conn = DatabaseConnection.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM loans");
                    stmt.execute("DELETE FROM books");
                    stmt.execute("DELETE FROM members");
                    loadBackupInfo();
                    showAlert(Alert.AlertType.INFORMATION, "Database Reset",
                            "All books, members, and loans have been deleted.");
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Reset Failed", "Could not reset database: " + e.getMessage());
                }
            }
        });
    }

    /* ═══════════════════════════════════════════════════════
       HELPERS
       ═══════════════════════════════════════════════════════ */

    private String safe(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private void showAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
