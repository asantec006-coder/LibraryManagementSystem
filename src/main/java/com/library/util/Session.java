package com.library.util;

import com.library.model.AdminUser;

/**
 * Holds the currently signed-in user for the lifetime of the running
 * application. Set once by LoginController on a successful sign-in;
 * read by the shared Sidebar/TopBar so the UI shows the real user
 * instead of a hard-coded name, and cleared on logout.
 */
public final class Session {

    private static AdminUser currentUser;

    private Session() {
    }

    public static void setCurrentUser(AdminUser user) {
        currentUser = user;
    }

    public static AdminUser getCurrentUser() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
    }
}
