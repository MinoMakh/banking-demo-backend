package com.banking.backend.transaction.dto;

import com.banking.backend.transaction.Transaction;
import com.banking.backend.transaction.TransactionStatus;
import com.banking.backend.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionDto(
        Long id,
        String transactionRef,
        String accountNo,
        String currency,
        TransactionType type,
        BigDecimal amount,
        TransactionStatus status,
        LocalDateTime transactionDate
) {
    public static TransactionDto from(Transaction t) {
        return new TransactionDto(
                t.getId(),
                t.getTransactionRef(),
                t.getAccount().getAccountNo(),
                t.getAccount().getCurrency(),
                t.getType(),
                t.getAmount(),
                t.getStatus(),
                t.getTransactionDate()
        );
    }
}
