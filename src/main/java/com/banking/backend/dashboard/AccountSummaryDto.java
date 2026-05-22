package com.banking.backend.dashboard;

import com.banking.backend.account.Account;
import com.banking.backend.account.AccountStatus;
import com.banking.backend.account.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountSummaryDto(
        Long id,
        String accountNo,
        String currency,
        BigDecimal balance,
        BigDecimal reservedBalance,
        BigDecimal availableBalance,
        AccountType accountType,
        String iban,
        AccountStatus status,
        LocalDateTime openedAt
) {
    public static AccountSummaryDto from(Account a) {
        return new AccountSummaryDto(
                a.getId(),
                a.getAccountNo(),
                a.getCurrency(),
                a.getBalance(),
                a.getReservedBalance(),
                a.getAvailableBalance(),
                a.getAccountType(),
                a.getIban(),
                a.getStatus(),
                a.getOpenedAt()
        );
    }
}
