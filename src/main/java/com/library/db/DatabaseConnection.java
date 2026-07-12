package com.library.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Opens a JDBC connection to the SQLite database file.
 *
 * IMPORTANT: this returns a brand-new connection on every call rather than
 * sharing one static connection. SQLite/JDBC connections are not safe to
 * share across threads, and this app legitimately needs to touch the
 * database from more than one thread — e.g. OnlineLibraryController
 * downloads books on a background thread (CompletableFuture) while the FX
 * Application Thread may simultaneously be running a query for Reports,
 * Dashboard, etc.
 *
 * An earlier version of this class kept a single static Connection and
 * handed the same instance to every caller. Because every repository
 * method wraps its Connection in try-with-resources (which calls close()
 * at the end of the block), that shared connection was being closed after
 * every single query — so two threads racing to use it could close it out
 * from under each other mid-query, causing intermittent SQLException /
 * "database is locked" failures (e.g. Reports silently failing to load
 * right after downloading a book in Online Library).
 *
 * Giving every caller its own connection (with a busy_timeout so
 * simultaneous readers/writers wait briefly instead of erroring
 * immediately) removes that race entirely. Each repository method already
 * closes its own connection via try-with-resources, so no repository code
 * needs to change.
 */
public final class DatabaseConnection {

    private static final String DB_URL = "jdbc:sqlite:library_prod.db";

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL);
        try (var stmt = connection.createStatement()) {
            // Enforce foreign key constraints (off by default in SQLite).
            stmt.execute("PRAGMA foreign_keys = ON");
            // Wait up to 5s for a lock instead of throwing SQLITE_BUSY
            // immediately when another thread's connection is mid-write.
            stmt.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }
}
