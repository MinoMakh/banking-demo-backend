package com.banking.backend.auth;

import java.time.Instant;

public record RefreshResponse(
        String token,
        Instant expiresAt,
        String refreshToken,
        Instant refreshExpiresAt
) {}
