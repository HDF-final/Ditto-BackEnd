package com.ditto.auth.dto.response;

public record SignupResponse(
        String userEmail,
        String role) {
}
