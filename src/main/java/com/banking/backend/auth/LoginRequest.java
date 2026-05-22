package com.banking.backend.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String customerNo,
        @NotBlank String password
) {}
