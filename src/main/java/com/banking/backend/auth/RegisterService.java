package com.banking.backend.auth;

import com.banking.backend.account.Account;
import com.banking.backend.account.AccountRepository;
import com.banking.backend.customer.Customer;
import com.banking.backend.customer.CustomerRepository;
import com.banking.backend.customer.CustomerRole;
import com.banking.backend.customer.CustomerStatus;
import com.banking.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class RegisterService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long expirationMs;

    public RegisterService(CustomerRepository customerRepository,
                           AccountRepository accountRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.expirationMs = expirationMs;
    }

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        if (customerRepository.existsByMobile(req.mobile())) {
            throw new DataIntegrityViolationException("Mobile already registered");
        }

        Customer customer = new Customer();
        customer.setCustomerNo(nextCustomerNo());
        customer.setFullName(req.fullName().trim());
        customer.setMobile(req.mobile());
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setRole(CustomerRole.USER);
        customer.setPasswordHash(passwordEncoder.encode(req.password()));
        customer = customerRepository.save(customer);

        Account account = new Account();
        account.setAccountNo(nextAccountNo());
        account.setCustomer(customer);
        account.setBalance(BigDecimal.ZERO);
        account.setReservedBalance(BigDecimal.ZERO);
        account.setCurrency("ILS");
        accountRepository.save(account);

        String token = jwtService.generateToken(customer.getCustomerNo(), customer.getRole().name());
        return new LoginResponse(
                token,
                Instant.now().plusMillis(expirationMs),
                customer.getCustomerNo(),
                customer.getFullName(),
                customer.getRole());
    }

    private String nextCustomerNo() {
        return String.format("C%03d", customerRepository.findMaxUserCustomerNumber() + 1);
    }

    private String nextAccountNo() {
        return "ACC" + (accountRepository.findMaxAccountNumber() + 1);
    }
}
