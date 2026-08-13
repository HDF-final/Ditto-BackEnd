package com.ditto.user.domain;

public enum UserRole {
    ROLE_CUSTOMER;

    public static UserRole fromSignupRole(String role) {
        if ("CUSTOMER".equals(role)) {
            return ROLE_CUSTOMER;
        }
        throw new IllegalArgumentException("Unsupported signup role: " + role);
    }
}
