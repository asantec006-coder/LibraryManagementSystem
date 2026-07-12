package com.library.repository;

import com.library.db.DatabaseConnection;
import com.library.model.AdminUser;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Repository for admin/librarian login accounts.
 * On first run (empty table) it seeds one default account so the
 * application is usable out of the box:
 *   username: admin@library.com
 *   password: password
 * The demo password is only ever stored as a bcrypt hash.
 */
public class AdminUserRepository {

    private static final String DEFAULT_USERNAME = "admin@library.com";
    private static final String DEFAULT_PASSWORD = "password";
    private static final String DEFAULT_FULL_NAME = "Admin Librarian";

    public void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS admin_users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "password_hash TEXT NOT NULL, " +
                "full_name TEXT" +
                ")";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
        seedDefaultAdminIfEmpty();
    }

    private void seedDefaultAdminIfEmpty() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM admin_users")) {
            if (rs.next() && rs.getInt(1) == 0) {
                AdminUser defaultAdmin = new AdminUser();
                defaultAdmin.setUsername(DEFAULT_USERNAME);
                defaultAdmin.setPasswordHash(BCrypt.hashpw(DEFAULT_PASSWORD, BCrypt.gensalt()));
                defaultAdmin.setFullName(DEFAULT_FULL_NAME);
                save(defaultAdmin);
            }
        }
    }

    public AdminUser save(AdminUser admin) throws SQLException {
        String sql = "INSERT INTO admin_users (username, password_hash, full_name) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, admin.getUsername());
            ps.setString(2, admin.getPasswordHash());
            ps.setString(3, admin.getFullName());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    admin.setId(keys.getInt(1));
                }
            }
        }
        return admin;
    }

    public Optional<AdminUser> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM admin_users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void updatePassword(int userId, String newPasswordHash) throws SQLException {
        String sql = "UPDATE admin_users SET password_hash = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void updateFullName(int userId, String fullName) throws SQLException {
        String sql = "UPDATE admin_users SET full_name = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private AdminUser mapRow(ResultSet rs) throws SQLException {
        return new AdminUser(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("full_name"));
    }
}
