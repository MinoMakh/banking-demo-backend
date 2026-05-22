package com.banking.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Pattern(regexp = "^\\+?\\d{8,15}$", message = "Invalid mobile") String mobile,
        @NotBlank @Size(min = 4, max = 64) String password
) {}
