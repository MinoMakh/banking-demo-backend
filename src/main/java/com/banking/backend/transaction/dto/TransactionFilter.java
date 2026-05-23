package com.banking.backend.transaction.dto;

import com.banking.backend.transaction.TransactionType;

import java.time.LocalDate;

public record TransactionFilter(
        Long accountId,
        TransactionType type,
        LocalDate from,
        LocalDate to
) {}
