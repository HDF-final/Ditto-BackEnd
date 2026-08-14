package com.ditto.user.domain;

public enum UserRole {
    ROLE_CUSTOMER,
    ROLE_ADMIN;

    public static UserRole fromSignupRole(String role) {
        if ("CUSTOMER".equals(role)) {
            return ROLE_CUSTOMER;
        }
        if ("ADMIN".equals(role)) {
            return ROLE_ADMIN;
        }
        throw new IllegalArgumentException("Unsupported signup role: " + role);
    }
}
