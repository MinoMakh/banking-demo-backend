package com.banking.backend.dashboard;

import com.banking.backend.account.Account;
import com.banking.backend.account.AccountRepository;
import com.banking.backend.customer.Customer;
import com.banking.backend.customer.CustomerRepository;
import com.banking.backend.transaction.Transaction;
import com.banking.backend.transaction.TransactionRepository;
import com.banking.backend.transaction.TransactionType;
import com.banking.backend.transaction.dto.TransactionDto;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public DashboardService(CustomerRepository customerRepository,
                             AccountRepository accountRepository,
                             TransactionRepository transactionRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary(String customerNo) {
        Customer customer = customerRepository.findByCustomerNo(customerNo)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        List<Account> accounts = accountRepository.findByCustomerId(customer.getId());
        Set<Long> accountIds = accounts.stream().map(Account::getId).collect(Collectors.toSet());

        List<AccountSummaryDto> accountSummaries = accounts.stream()
                .map(AccountSummaryDto::from)
                .toList();

        List<TransactionDto> recentTransactions = transactionRepository
                .findAll(accountIn(accountIds),
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "transactionDate")))
                .map(TransactionDto::from)
                .toList();

        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate firstOfNextMonth = firstOfMonth.plusMonths(1);

        BigDecimal monthlyDebit = sumForType(accountIds, TransactionType.DEBIT, firstOfMonth, firstOfNextMonth);
        BigDecimal monthlyCredit = sumForType(accountIds, TransactionType.CREDIT, firstOfMonth, firstOfNextMonth);

        return new DashboardSummaryDto(accountSummaries, recentTransactions, monthlyDebit, monthlyCredit);
    }

    private BigDecimal sumForType(Set<Long> accountIds, TransactionType type,
                                   LocalDate from, LocalDate to) {
        Specification<Transaction> spec = accountIn(accountIds)
                .and((root, query, cb) -> cb.equal(root.get("type"), type))
                .and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("transactionDate"), from.atStartOfDay()))
                .and((root, query, cb) -> cb.lessThan(root.get("transactionDate"), to.atStartOfDay()));

        return transactionRepository.findAll(spec)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Specification<Transaction> accountIn(Set<Long> ids) {
        return (root, query, cb) -> root.get("account").get("id").in(ids);
    }
}
