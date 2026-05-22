package com.banking.backend.dashboard;

import com.banking.backend.transaction.dto.TransactionDto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryDto(
        List<AccountSummaryDto> accounts,
        List<TransactionDto> recentTransactions,
        BigDecimal monthlyDebit,
        BigDecimal monthlyCredit
) {}
