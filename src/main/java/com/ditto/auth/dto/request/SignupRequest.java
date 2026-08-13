package com.ditto.auth.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SignupRequest(
        @NotBlank String userEmail,
        @NotBlank String password,
        @NotBlank String name,
        @NotBlank String nickname,
        @NotBlank String phone,
        @Valid @NotNull AddressRequest address,
        @NotBlank String role) {

    public record AddressRequest(
            @NotBlank String bcode,
            @NotBlank String jibunAddress,
            @NotBlank String roadAddress,
            @NotBlank String detail) {
    }
}
