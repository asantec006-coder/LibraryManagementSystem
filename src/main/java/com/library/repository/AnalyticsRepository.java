package com.library.repository;

import com.library.db.DatabaseConnection;
import com.library.model.Loan;
import com.library.model.dto.DashboardMetrics;
import com.library.model.dto.LoanDetails;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository dedicated to read-only aggregation queries for dashboards and analytics.
 * Offloads heavy cross-referencing and counting to the SQLite database engine.
 */
public class AnalyticsRepository {

    /**
     * Executes highly optimized COUNT queries to get overview statistics.
     */
    public DashboardMetrics getDashboardMetrics() throws SQLException {
        int totalBooks = 0;
        int availableBooks = 0;
        long activeMembers = 0;
        long activeLoans = 0;
        long overdueLoans = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            try (ResultSet rs = stmt.executeQuery("SELECT SUM(total_copies), SUM(available_copies) FROM books")) {
                if (rs.next()) {
                    totalBooks = rs.getInt(1);
                    availableBooks = rs.getInt(2);
                }
            }
            
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM members WHERE status = 'Active'")) {
                if (rs.next()) activeMembers = rs.getLong(1);
            }
            
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM loans WHERE returned = 0")) {
                if (rs.next()) activeLoans = rs.getLong(1);
            }
            
            String today = LocalDate.now().toString();
            String sql = "SELECT COUNT(*) FROM loans WHERE returned = 0 AND due_date IS NOT NULL AND due_date < ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, today);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) overdueLoans = rs.getLong(1);
                }
            }
        }
        
        return new DashboardMetrics(totalBooks, availableBooks, activeMembers, activeLoans, overdueLoans);
    }

    /**
     * Gets a trend of loans aggregated by month for the last N months.
     * Returns a map like {"2026-01" -> 4, "2026-02" -> 10, ...}
     */
    public Map<String, Integer> getLoanTrend(int months) throws SQLException {
        // Create an ordered map so the chart displays sequentially
        Map<String, Integer> trend = new LinkedHashMap<>();
        
        // Initialize the last N months with 0
        LocalDate current = LocalDate.now().minusMonths(months - 1).withDayOfMonth(1);
        for (int i = 0; i < months; i++) {
            String monthKey = current.toString().substring(0, 7); // e.g. "2026-07"
            trend.put(monthKey, 0);
            current = current.plusMonths(1);
        }
        
        String startDate = LocalDate.now().minusMonths(months - 1).withDayOfMonth(1).toString();
        
        String sql = """
                SELECT substr(loan_date, 1, 7) as month, COUNT(*) as count 
                FROM loans 
                WHERE loan_date >= ?
                GROUP BY month 
                ORDER BY month ASC
                """;
                
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, startDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String month = rs.getString("month");
                    int count = rs.getInt("count");
                    if (trend.containsKey(month)) {
                        trend.put(month, count);
                    }
                }
            }
        }
        return trend;
    }

    /**
     * Gets the most popular genres based on loan history.
     */
    public Map<String, Integer> getPopularCategories(int limit) throws SQLException {
        Map<String, Integer> categories = new LinkedHashMap<>();
        String sql = """
                SELECT b.genre, COUNT(l.id) as count
                FROM loans l
                JOIN books b ON l.book_id = b.id
                WHERE b.genre IS NOT NULL AND b.genre != ''
                GROUP BY b.genre
                ORDER BY count DESC
                LIMIT ?
                """;
                
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    categories.put(rs.getString("genre"), rs.getInt("count"));
                }
            }
        }
        return categories;
    }

    /**
     * Fetches recent loans joined with book titles and member names.
     */
    public List<LoanDetails> getRecentLoans(int limit, boolean returnedOnly) throws SQLException {
        List<LoanDetails> list = new ArrayList<>();
        String sql = """
                SELECT l.*, b.title as book_title, m.name as member_name
                FROM loans l
                JOIN books b ON l.book_id = b.id
                JOIN members m ON l.member_id = m.id
                WHERE l.returned = ?
                ORDER BY l.id DESC
                LIMIT ?
                """;
                
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, returnedOnly ? 1 : 0);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Loan loan = new Loan();
                    loan.setId(rs.getInt("id"));
                    loan.setBookId(rs.getInt("book_id"));
                    loan.setMemberId(rs.getInt("member_id"));
                    loan.setLoanDate(parseDate(rs.getString("loan_date")));
                    loan.setDueDate(parseDate(rs.getString("due_date")));
                    loan.setReturnDate(parseDate(rs.getString("return_date")));
                    loan.setReturned(rs.getBoolean("returned"));
                    
                    list.add(new LoanDetails(loan, rs.getString("book_title"), rs.getString("member_name")));
                }
            }
        }
        return list;
    }
    
    private LocalDate parseDate(String s) {
        return (s == null || s.isBlank()) ? null : LocalDate.parse(s);
    }
}
