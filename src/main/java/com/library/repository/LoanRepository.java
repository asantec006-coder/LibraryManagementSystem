package com.library.repository;

import com.library.db.DatabaseConnection;
import com.library.model.Loan;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for loan persistence operations.
 * All DB access goes through DatabaseConnection; no business logic here.
 */
public class LoanRepository {

    /** Creates the loans table (with due_date column) if it doesn't exist yet. */
    public void createTableIfNotExists() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS loans (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    book_id     INTEGER NOT NULL,
                    member_id   INTEGER NOT NULL,
                    loan_date   TEXT NOT NULL,
                    due_date    TEXT,
                    return_date TEXT,
                    returned    INTEGER NOT NULL DEFAULT 0
                )""";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            // Add due_date column to existing tables that pre-date this schema.
            try { stmt.execute("ALTER TABLE loans ADD COLUMN due_date TEXT"); }
            catch (SQLException ignored) { /* column already exists */ }
        }
    }

    // ── write operations ─────────────────────────────────────────────────

    public void save(Loan loan) throws SQLException {
        String sql = """
                INSERT INTO loans (book_id, member_id, loan_date, due_date, return_date, returned)
                VALUES (?, ?, ?, ?, ?, ?)""";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, loan.getBookId());
            ps.setInt(2, loan.getMemberId());
            ps.setString(3, loan.getLoanDate() != null ? loan.getLoanDate().toString() : null);
            ps.setString(4, loan.getDueDate() != null ? loan.getDueDate().toString() : null);
            ps.setString(5, loan.getReturnDate() != null ? loan.getReturnDate().toString() : null);
            ps.setBoolean(6, loan.isReturned());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) loan.setId(keys.getInt(1));
            }
        }
    }

    public void update(Loan loan) throws SQLException {
        String sql = """
                UPDATE loans
                SET book_id = ?, member_id = ?, loan_date = ?,
                    due_date = ?, return_date = ?, returned = ?
                WHERE id = ?""";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, loan.getBookId());
            ps.setInt(2, loan.getMemberId());
            ps.setString(3, loan.getLoanDate() != null ? loan.getLoanDate().toString() : null);
            ps.setString(4, loan.getDueDate() != null ? loan.getDueDate().toString() : null);
            ps.setString(5, loan.getReturnDate() != null ? loan.getReturnDate().toString() : null);
            ps.setBoolean(6, loan.isReturned());
            ps.setInt(7, loan.getId());
            ps.executeUpdate();
        }
    }

    // ── queries ──────────────────────────────────────────────────────────

    public List<Loan> findAll() throws SQLException {
        return query("SELECT * FROM loans ORDER BY id DESC", ps -> {});
    }

    /** All loans that have not yet been returned, newest first. */
    public List<Loan> findActive() throws SQLException {
        return query("SELECT * FROM loans WHERE returned = 0 ORDER BY loan_date DESC", ps -> {});
    }

    /** Recently returned loans, newest first, up to {@code limit}. */
    public List<Loan> findRecentlyReturned(int limit) throws SQLException {
        return query(
                "SELECT * FROM loans WHERE returned = 1 ORDER BY return_date DESC LIMIT ?",
                ps -> ps.setInt(1, limit));
    }

    public List<Loan> findByMemberId(int memberId) throws SQLException {
        return query("SELECT * FROM loans WHERE member_id = ? ORDER BY id DESC",
                ps -> ps.setInt(1, memberId));
    }

    public List<Loan> findByBookId(int bookId) throws SQLException {
        return query("SELECT * FROM loans WHERE book_id = ? ORDER BY id DESC",
                ps -> ps.setInt(1, bookId));
    }

    public Loan findById(int id) throws SQLException {
        List<Loan> result = query("SELECT * FROM loans WHERE id = ?",
                ps -> ps.setInt(1, id));
        return result.isEmpty() ? null : result.get(0);
    }



    // ── private helpers ───────────────────────────────────────────────────

    @FunctionalInterface
    private interface Binder { void bind(PreparedStatement ps) throws SQLException; }

    private List<Loan> query(String sql, Binder binder) throws SQLException {
        List<Loan> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }



    private Loan map(ResultSet rs) throws SQLException {
        Loan loan = new Loan();
        loan.setId(rs.getInt("id"));
        loan.setBookId(rs.getInt("book_id"));
        loan.setMemberId(rs.getInt("member_id"));
        loan.setLoanDate(parseDate(rs.getString("loan_date")));
        loan.setDueDate(parseDate(rs.getString("due_date")));
        loan.setReturnDate(parseDate(rs.getString("return_date")));
        loan.setReturned(rs.getBoolean("returned"));
        return loan;
    }

    private LocalDate parseDate(String s) {
        return (s == null || s.isBlank()) ? null : LocalDate.parse(s);
    }
}
