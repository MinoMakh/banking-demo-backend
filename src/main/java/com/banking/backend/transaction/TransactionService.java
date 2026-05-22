package com.banking.backend.transaction;

import com.banking.backend.account.Account;
import com.banking.backend.account.AccountRepository;
import com.banking.backend.customer.Customer;
import com.banking.backend.customer.CustomerRepository;
import com.banking.backend.transaction.dto.TransactionDto;
import com.banking.backend.transaction.dto.TransactionFilter;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public TransactionService(TransactionRepository transactionRepository,
                               AccountRepository accountRepository,
                               CustomerRepository customerRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto> getTransactions(TransactionFilter filter, Pageable pageable, String customerNo) {
        Set<Long> accountIds = getCustomerAccountIds(customerNo);
        return transactionRepository
                .findAll(TransactionSpecification.forFilter(filter, accountIds), pageable)
                .map(TransactionDto::from);
    }

    @Transactional(readOnly = true)
    public TransactionDto getTransaction(Long id, String customerNo) {
        Set<Long> accountIds = getCustomerAccountIds(customerNo);
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));
        if (!accountIds.contains(transaction.getAccount().getId())) {
            throw new EntityNotFoundException("Transaction not found");
        }
        return TransactionDto.from(transaction);
    }

    private Set<Long> getCustomerAccountIds(String customerNo) {
        Customer customer = customerRepository.findByCustomerNo(customerNo)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
        return accountRepository.findByCustomerId(customer.getId())
                .stream()
                .map(Account::getId)
                .collect(Collectors.toSet());
    }
}
