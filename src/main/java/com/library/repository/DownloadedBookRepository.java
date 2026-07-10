package com.library.repository;

import com.library.db.DatabaseConnection;
import com.library.model.DownloadedBook;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DownloadedBookRepository {

    public void createTableIfNotExists() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS downloaded_books (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    title         TEXT    NOT NULL,
                    author        TEXT,
                    source        TEXT    NOT NULL,
                    format        TEXT    NOT NULL,
                    local_path    TEXT    NOT NULL,
                    download_url  TEXT,
                    downloaded_at TEXT    NOT NULL
                )""";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void save(DownloadedBook book) throws SQLException {
        String sql = """
                INSERT INTO downloaded_books (title, author, source, format, local_path, download_url, downloaded_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)""";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getSource());
            ps.setString(4, book.getFormat());
            ps.setString(5, book.getLocalPath());
            ps.setString(6, book.getDownloadUrl());
            ps.setString(7, book.getDownloadedAt().toString());
            
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) book.setId(keys.getInt(1));
            }
        }
    }

    public List<DownloadedBook> findAll() throws SQLException {
        List<DownloadedBook> list = new ArrayList<>();
        String sql = "SELECT * FROM downloaded_books ORDER BY downloaded_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }
    
    public Optional<DownloadedBook> findByDownloadUrl(String url) throws SQLException {
        if (url == null) return Optional.empty();
        String sql = "SELECT * FROM downloaded_books WHERE download_url = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, url);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    private DownloadedBook map(ResultSet rs) throws SQLException {
        DownloadedBook book = new DownloadedBook();
        book.setId(rs.getInt("id"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setSource(rs.getString("source"));
        book.setFormat(rs.getString("format"));
        book.setLocalPath(rs.getString("local_path"));
        book.setDownloadUrl(rs.getString("download_url"));
        book.setDownloadedAt(LocalDateTime.parse(rs.getString("downloaded_at")));
        return book;
    }
}
