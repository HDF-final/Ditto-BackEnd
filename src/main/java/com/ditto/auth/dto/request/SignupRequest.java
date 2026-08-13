package com.ditto.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = false)
public class SignupRequest {

    @NotBlank
    private String userEmail;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    @NotBlank
    private String countryCode;
}
