package com.library.service;

import com.library.model.AdminUser;
import com.library.repository.AdminUserRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Business logic for authenticating librarians/admins.
 * Controllers should only ever talk to this service, never to
 * AdminUserRepository or BCrypt directly.
 */
public class AuthService {

    private final AdminUserRepository adminUserRepository;

    public AuthService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    /**
     * @return the authenticated user, or empty if the username doesn't
     *         exist or the password doesn't match.
     */
    public Optional<AdminUser> login(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return Optional.empty();
        }
        Optional<AdminUser> user = adminUserRepository.findByUsername(username.trim());
        if (user.isEmpty()) {
            return Optional.empty();
        }
        boolean matches = BCrypt.checkpw(password, user.get().getPasswordHash());
        return matches ? user : Optional.empty();
    }
}
