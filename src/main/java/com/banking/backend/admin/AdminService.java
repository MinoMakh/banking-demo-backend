package com.banking.backend.admin;

import com.banking.backend.account.Account;
import com.banking.backend.account.AccountRepository;
import com.banking.backend.customer.Customer;
import com.banking.backend.customer.CustomerRepository;
import com.banking.backend.dashboard.AccountSummaryDto;
import com.banking.backend.transaction.Transaction;
import com.banking.backend.transaction.TransactionRepository;
import com.banking.backend.transaction.TransactionSpecification;
import com.banking.backend.transaction.TransactionStatus;
import com.banking.backend.transaction.TransactionType;
import com.banking.backend.transaction.dto.TransactionDto;
import com.banking.backend.transaction.dto.TransactionFilter;
import com.banking.backend.transfer.InsufficientFundsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AdminService(CustomerRepository customerRepository,
                        AccountRepository accountRepository,
                        TransactionRepository transactionRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public AdminStatsDto getStats() {
        long customers = customerRepository.count();
        long accounts = accountRepository.count();
        long transactions = transactionRepository.count();
        return new AdminStatsDto(customers, accounts, transactions);
    }

    @Transactional(readOnly = true)
    public Page<AdminCustomerDto> listCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable)
                .map(c -> AdminCustomerDto.from(c, accountRepository.findByCustomerId(c.getId()).size()));
    }

    @Transactional(readOnly = true)
    public AdminCustomerDetailDto getCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
        List<AccountSummaryDto> accounts = accountRepository.findByCustomerId(customer.getId())
                .stream().map(AccountSummaryDto::from).toList();
        return AdminCustomerDetailDto.from(customer, accounts);
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto> listTransactions(TransactionFilter filter, Pageable pageable) {
        return transactionRepository
                .findAll(TransactionSpecification.forFilter(filter, null), pageable)
                .map(TransactionDto::from);
    }

    @Transactional
    public TransactionDto adjustBalance(Long accountId, BalanceAdjustRequest req) {
        if (req.delta().signum() == 0) {
            throw new IllegalArgumentException("Delta cannot be zero");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        BigDecimal absAmount = req.delta().abs();
        boolean credit = req.delta().signum() > 0;

        if (!credit && account.getAvailableBalance().compareTo(absAmount) < 0) {
            throw new InsufficientFundsException("Adjustment would overdraw the account");
        }

        BigDecimal newBalance = credit
                ? account.getBalance().add(absAmount)
                : account.getBalance().subtract(absAmount);
        account.setBalance(newBalance);

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setAmount(absAmount);
        tx.setType(credit ? TransactionType.CREDIT : TransactionType.DEBIT);
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setTransactionDate(LocalDateTime.now());
        tx.setTransactionRef("ADJ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        transactionRepository.save(tx);

        return TransactionDto.from(tx);
    }

    @Transactional
    public AdminCustomerDetailDto updateStatus(Long customerId, StatusUpdateRequest req) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
        customer.setStatus(req.status());
        List<AccountSummaryDto> accounts = accountRepository.findByCustomerId(customer.getId())
                .stream().map(AccountSummaryDto::from).toList();
        return AdminCustomerDetailDto.from(customer, accounts);
    }
}
