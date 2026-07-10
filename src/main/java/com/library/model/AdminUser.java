package com.library.model;

/**
 * An account that can sign in to the Library Management System.
 * Passwords are never stored or compared in plain text — only the
 * bcrypt hash is persisted (see AuthService).
 */
public class AdminUser {

    private int id;
    private String username;
    private String passwordHash;
    private String fullName;

    public AdminUser() {
    }

    public AdminUser(int id, String username, String passwordHash, String fullName) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
