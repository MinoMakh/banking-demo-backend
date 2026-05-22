package com.banking.backend.transaction.dto;

import com.banking.backend.transaction.TransactionStatus;
import com.banking.backend.transaction.TransactionType;

import java.time.LocalDate;

public record TransactionFilter(
        Long accountId,
        TransactionType type,
        TransactionStatus status,
        LocalDate from,
        LocalDate to
) {}
