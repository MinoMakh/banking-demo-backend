package com.banking.backend.account;

import com.banking.backend.customer.Customer;
import com.banking.backend.customer.CustomerRepository;
import com.banking.backend.dashboard.AccountSummaryDto;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
public class AccountService {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("ILS", "USD");

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public AccountService(AccountRepository accountRepository,
                          CustomerRepository customerRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public AccountSummaryDto openAccount(String customerNo, String currency) {
        Customer customer = customerRepository.findByCustomerNo(customerNo)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
        Account account = create(customer, currency);
        return AccountSummaryDto.from(account);
    }

    @Transactional
    public Account create(Customer customer, String currency) {
        String normalized = currency == null ? "" : currency.trim().toUpperCase();
        if (!SUPPORTED_CURRENCIES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported currency: " + currency);
        }
        Account account = new Account();
        account.setAccountNo(nextAccountNo());
        account.setCustomer(customer);
        account.setBalance(BigDecimal.ZERO);
        account.setReservedBalance(BigDecimal.ZERO);
        account.setCurrency(normalized);
        return accountRepository.save(account);
    }

    private String nextAccountNo() {
        return "ACC" + (accountRepository.findMaxAccountNumber() + 1);
    }
}
