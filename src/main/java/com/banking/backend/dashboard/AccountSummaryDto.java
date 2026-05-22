package com.banking.backend.dashboard;

import com.banking.backend.account.Account;

import java.math.BigDecimal;

public record AccountSummaryDto(
        Long id,
        String accountNo,
        String currency,
        BigDecimal balance,
        BigDecimal reservedBalance,
        BigDecimal availableBalance
) {
    public static AccountSummaryDto from(Account a) {
        return new AccountSummaryDto(
                a.getId(),
                a.getAccountNo(),
                a.getCurrency(),
                a.getBalance(),
                a.getReservedBalance(),
                a.getAvailableBalance()
        );
    }
}
