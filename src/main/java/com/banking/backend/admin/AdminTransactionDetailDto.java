package com.banking.backend.admin;

import com.banking.backend.transaction.Transaction;
import com.banking.backend.transaction.TransactionType;
import com.banking.backend.customer.Customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminTransactionDetailDto(
        Long id,
        String transactionRef,
        String accountNo,
        String currency,
        TransactionType type,
        BigDecimal amount,
        LocalDateTime transactionDate,
        Long customerId,
        String customerNo,
        String customerFullName
) {
    public static AdminTransactionDetailDto from(Transaction t) {
        Customer c = t.getAccount().getCustomer();
        return new AdminTransactionDetailDto(
                t.getId(),
                t.getTransactionRef(),
                t.getAccount().getAccountNo(),
                t.getAccount().getCurrency(),
                t.getType(),
                t.getAmount(),
                t.getTransactionDate(),
                c.getId(),
                c.getCustomerNo(),
                c.getFullName());
    }
}
