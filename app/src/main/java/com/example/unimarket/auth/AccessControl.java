package com.example.unimarket.auth;

import android.text.TextUtils;

import com.example.unimarket.data.model.User;

public final class AccessControl {
    public static final String ROLE_USER = "user";
    public static final String ROLE_MODERATOR = "moderator";
    public static final String ROLE_ADMIN = "admin";

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_SUSPENDED = "suspended";

    private AccessControl() {
    }

    public static boolean isActive(User user) {
        if (user == null) {
            return false;
        }
        return TextUtils.isEmpty(user.getAccount_status())
                || STATUS_ACTIVE.equalsIgnoreCase(user.getAccount_status());
    }

    public static boolean isModerator(User user) {
        return hasRole(user, ROLE_MODERATOR) || isAdmin(user);
    }

    public static boolean isAdmin(User user) {
        return hasRole(user, ROLE_ADMIN);
    }

    public static boolean canSell(User user) {
        return isActive(user) && (user.isVerified() || isModerator(user));
    }

    private static boolean hasRole(User user, String role) {
        return user != null
                && !TextUtils.isEmpty(user.getRole())
                && role.equalsIgnoreCase(user.getRole());
    }
}
