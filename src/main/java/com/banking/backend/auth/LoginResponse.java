package com.banking.backend.auth;

import com.banking.backend.customer.CustomerRole;

import java.time.Instant;

public record LoginResponse(
        String token,
        Instant expiresAt,
        String refreshToken,
        Instant refreshExpiresAt,
        String customerNo,
        String fullName,
        CustomerRole role
) {}
