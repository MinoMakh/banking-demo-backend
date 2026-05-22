package com.banking.backend.admin;

public record AdminStatsDto(
        long customerCount,
        long accountCount,
        long transactionCount
) {}
