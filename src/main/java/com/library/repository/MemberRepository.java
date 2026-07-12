package com.library.repository;

import com.library.db.DatabaseConnection;
import com.library.model.Member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository layer for Member. Talks directly to SQLite. Contains no
 * business rules — that lives in MemberService.
 */
public class MemberRepository {

    /**
     * Creates the members table if it doesn't already exist.
     * Call once at application startup.
     */
    public void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS members (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "email TEXT NOT NULL UNIQUE, " +
                "phone TEXT, " +
                "student_id TEXT, " +
                "join_date TEXT NOT NULL, " +
                "books_borrowed INTEGER NOT NULL DEFAULT 0, " +
                "membership_type TEXT NOT NULL CHECK(membership_type IN ('Standard','Premium')), " +
                "status TEXT NOT NULL CHECK(status IN ('Active','Inactive'))" +
                ")";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            // Add student_id to existing tables that pre-date this column.
            try { stmt.execute("ALTER TABLE members ADD COLUMN student_id TEXT"); }
            catch (SQLException ignored) { /* column already exists */ }
        }
    }

    public Member save(Member member) throws SQLException {
        String sql = "INSERT INTO members (name, email, phone, student_id, join_date, books_borrowed, membership_type, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, member.getName());
            ps.setString(2, member.getEmail());
            ps.setString(3, member.getPhone());
            ps.setString(4, member.getStudentId());
            ps.setString(5, member.getJoinDate() == null ? null : member.getJoinDate().toString());
            ps.setInt(6, member.getBooksBorrowed());
            ps.setString(7, member.getMembershipType());
            ps.setString(8, member.getStatus());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    member.setId(keys.getInt(1));
                }
            }
        }
        return member;
    }

    public boolean update(Member member) throws SQLException {
        String sql = "UPDATE members SET name = ?, email = ?, phone = ?, student_id = ?, join_date = ?, " +
                "books_borrowed = ?, membership_type = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, member.getName());
            ps.setString(2, member.getEmail());
            ps.setString(3, member.getPhone());
            ps.setString(4, member.getStudentId());
            ps.setString(5, member.getJoinDate() == null ? null : member.getJoinDate().toString());
            ps.setInt(6, member.getBooksBorrowed());
            ps.setString(7, member.getMembershipType());
            ps.setString(8, member.getStatus());
            ps.setInt(9, member.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteById(int id) throws SQLException {
        String sql = "DELETE FROM members WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public Optional<Member> findById(int id) throws SQLException {
        String sql = "SELECT * FROM members WHERE id = ?";
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

    public Optional<Member> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM members WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Member> findAll() throws SQLException {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT * FROM members ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                members.add(mapRow(rs));
            }
        }
        return members;
    }

    public List<Member> search(String keyword) throws SQLException {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT * FROM members WHERE name LIKE ? OR email LIKE ? OR phone LIKE ? OR student_id LIKE ? ORDER BY name";
        String like = "%" + keyword + "%";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(mapRow(rs));
                }
            }
        }
        return members;
    }

    private Member mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        String studentId = rs.getString("student_id");
        LocalDate joinDate = parseJoinDate(rs.getString("join_date"));
        int booksBorrowed = rs.getInt("books_borrowed");
        String membershipType = rs.getString("membership_type");
        String status = rs.getString("status");
        return new Member(id, name, email, phone, studentId, joinDate, booksBorrowed, membershipType, status);
    }

    /**
     * Parses join_date, which is stored as ISO text ("2026-07-10") going
     * forward. Rows written by an earlier version of this repository (which
     * used JDBC's setDate/getDate) may still hold a raw epoch-millis number
     * as text — this falls back to parsing that so old rows keep working.
     */
    private LocalDate parseJoinDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.chars().allMatch(Character::isDigit)) {
            return java.time.Instant.ofEpochMilli(Long.parseLong(value))
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }
        return LocalDate.parse(value);
    }
}
