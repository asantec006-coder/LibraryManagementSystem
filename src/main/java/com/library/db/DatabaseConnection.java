package com.library.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Single shared JDBC connection to the SQLite database file.
 * Every repository should go through this class instead of opening
 * its own connection.
 */
public final class DatabaseConnection {

    private static final String DB_URL = "jdbc:sqlite:library_prod.db";
    private static Connection connection;

    private DatabaseConnection() {
    }

    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            try (var stmt = connection.createStatement()) {
                // Enforce foreign key constraints (off by default in SQLite).
                stmt.execute("PRAGMA foreign_keys = ON");
            }
        }
        return connection;
    }

    public static synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                connection = null;
            }
        }
    }
}
