package com.library.repository;

import com.library.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple key-value store for library configuration (name, address, contact
 * info, etc. shown on the Settings > General tab). Nothing here is
 * pre-seeded with sample data — an empty table just means "not configured
 * yet", and the UI shows blank fields with placeholder text rather than
 * inventing a fake library name/address.
 */
public class LibrarySettingsRepository {

    public void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS library_settings (" +
                "key TEXT PRIMARY KEY, " +
                "value TEXT" +
                ")";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public Map<String, String> getAll() throws SQLException {
        Map<String, String> settings = new HashMap<>();
        String sql = "SELECT key, value FROM library_settings";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                settings.put(rs.getString("key"), rs.getString("value"));
            }
        }
        return settings;
    }

    /** Upserts every entry in {@code settings}, leaving any keys not included untouched. */
    public void saveAll(Map<String, String> settings) throws SQLException {
        String sql = "INSERT INTO library_settings (key, value) VALUES (?, ?) " +
                "ON CONFLICT(key) DO UPDATE SET value = excluded.value";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                ps.setString(1, entry.getKey());
                ps.setString(2, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
