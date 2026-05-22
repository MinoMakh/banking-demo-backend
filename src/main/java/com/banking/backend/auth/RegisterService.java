package com.banking.backend.auth;

import com.banking.backend.account.AccountService;
import com.banking.backend.account.AccountType;
import com.banking.backend.customer.Customer;
import com.banking.backend.customer.CustomerRepository;
import com.banking.backend.customer.CustomerRole;
import com.banking.backend.customer.CustomerStatus;
import com.banking.backend.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RegisterService {

    private final CustomerRepository customerRepository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public RegisterService(CustomerRepository customerRepository,
                           AccountService accountService,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.customerRepository = customerRepository;
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

        accountService.create(customer, "ILS", AccountType.CURRENT);

        String access = jwtService.generateToken(customer.getCustomerNo(), customer.getRole().name());
        String refresh = jwtService.generateRefreshToken(customer.getCustomerNo());
        return new LoginResponse(
                access,
                Instant.now().plusMillis(jwtService.getAccessExpirationMs()),
                refresh,
                Instant.now().plusMillis(jwtService.getRefreshExpirationMs()),
                customer.getCustomerNo(),
                customer.getFullName(),
                customer.getRole());
    }

    private String nextCustomerNo() {
        return String.format("C%03d", customerRepository.findMaxUserCustomerNumber() + 1);
    }
}
