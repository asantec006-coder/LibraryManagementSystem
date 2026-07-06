package com.library.repository;

import com.library.db.DatabaseConnection;
import com.library.model.Book;
import com.library.model.EBook;
import com.library.model.PhysicalBook;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository layer for Book / PhysicalBook / EBook.
 * Talks directly to SQLite. Contains no business rules — that lives in
 * BookService. Uses a single "books" table with a "type" discriminator
 * column so both subtypes can share one table and one query surface.
 */
public class BookRepository {

    /**
     * Creates the books table if it doesn't already exist.
     * Call once at application startup.
     */
    public void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS books (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "author TEXT NOT NULL, " +
                "isbn TEXT NOT NULL UNIQUE, " +
                "genre TEXT, " +
                "type TEXT NOT NULL CHECK(type IN ('PHYSICAL','EBOOK')), " +
                "total_copies INTEGER NOT NULL DEFAULT 0, " +
                "available_copies INTEGER NOT NULL DEFAULT 0, " +
                "download_url TEXT" +
                ")";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public Book save(Book book) throws SQLException {
        String sql = "INSERT INTO books (title, author, isbn, genre, type, total_copies, available_copies, download_url) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getIsbn());
            ps.setString(4, book.getGenre());
            ps.setString(5, book.getType());
            bindTypeSpecificColumns(ps, book);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    book.setId(keys.getInt(1));
                }
            }
        }
        return book;
    }

    public boolean update(Book book) throws SQLException {
        String sql = "UPDATE books SET title = ?, author = ?, isbn = ?, genre = ?, " +
                "total_copies = ?, available_copies = ?, download_url = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getIsbn());
            ps.setString(4, book.getGenre());
            bindTypeSpecificColumnsForUpdate(ps, book);
            ps.setInt(8, book.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteById(int id) throws SQLException {
        String sql = "DELETE FROM books WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public Optional<Book> findById(int id) throws SQLException {
        String sql = "SELECT * FROM books WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Book> findByIsbn(String isbn) throws SQLException {
        String sql = "SELECT * FROM books WHERE isbn = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Book> findAll() throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books ORDER BY title";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                books.add(mapRow(rs));
            }
        }
        return books;
    }

    public List<Book> search(String keyword) throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE title LIKE ? OR author LIKE ? OR isbn LIKE ? ORDER BY title";
        String like = "%" + keyword + "%";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    books.add(mapRow(rs));
                }
            }
        }
        return books;
    }

    // -- helpers --------------------------------------------------------

    private void bindTypeSpecificColumns(PreparedStatement ps, Book book) throws SQLException {
        if (book instanceof PhysicalBook pb) {
            ps.setInt(6, pb.getTotalCopies());
            ps.setInt(7, pb.getAvailableCopies());
            ps.setNull(8, Types.VARCHAR);
        } else if (book instanceof EBook eb) {
            ps.setInt(6, eb.getMaxConcurrentLicenses());
            ps.setInt(7, eb.getMaxConcurrentLicenses() - eb.getActiveLoans());
            ps.setString(8, eb.getDownloadUrl());
        } else {
            throw new IllegalArgumentException("Unknown book subtype: " + book.getClass());
        }
    }

    private void bindTypeSpecificColumnsForUpdate(PreparedStatement ps, Book book) throws SQLException {
        if (book instanceof PhysicalBook pb) {
            ps.setInt(5, pb.getTotalCopies());
            ps.setInt(6, pb.getAvailableCopies());
            ps.setNull(7, Types.VARCHAR);
        } else if (book instanceof EBook eb) {
            ps.setInt(5, eb.getMaxConcurrentLicenses());
            ps.setInt(6, eb.getMaxConcurrentLicenses() - eb.getActiveLoans());
            ps.setString(7, eb.getDownloadUrl());
        } else {
            throw new IllegalArgumentException("Unknown book subtype: " + book.getClass());
        }
    }

    private Book mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String title = rs.getString("title");
        String author = rs.getString("author");
        String isbn = rs.getString("isbn");
        String genre = rs.getString("genre");
        String type = rs.getString("type");
        int totalCopies = rs.getInt("total_copies");
        int availableCopies = rs.getInt("available_copies");
        String downloadUrl = rs.getString("download_url");

        if ("EBOOK".equals(type)) {
            int activeLoans = totalCopies - availableCopies;
            return new EBook(id, title, author, isbn, genre, downloadUrl, totalCopies, activeLoans);
        }
        return new PhysicalBook(id, title, author, isbn, genre, totalCopies, availableCopies);
    }
}
