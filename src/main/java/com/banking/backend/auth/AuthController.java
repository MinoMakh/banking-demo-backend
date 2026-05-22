package com.banking.backend.auth;

import com.banking.backend.customer.Customer;
import com.banking.backend.customer.CustomerRepository;
import com.banking.backend.customer.CustomerStatus;
import com.banking.backend.security.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomerRepository customerRepository;
    private final RegisterService registerService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          CustomerRepository customerRepository,
                          RegisterService registerService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.customerRepository = customerRepository;
        this.registerService = registerService;
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(registerService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.customerNo(), request.password()));

        Customer customer = customerRepository.findByCustomerNo(request.customerNo()).orElseThrow();
        return ResponseEntity.ok(buildLoginResponse(customer));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        String customerNo;
        try {
            if (!jwtService.isRefreshToken(request.refreshToken())) {
                throw new BadCredentialsException("Not a refresh token");
            }
            customerNo = jwtService.extractCustomerNo(request.refreshToken());
        } catch (JwtException e) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        Customer customer = customerRepository.findByCustomerNo(customerNo)
                .orElseThrow(() -> new BadCredentialsException("Customer not found"));
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new DisabledException("Account is disabled");
        }

        String access = jwtService.generateToken(customer.getCustomerNo(), customer.getRole().name());
        String refresh = jwtService.generateRefreshToken(customer.getCustomerNo());
        return ResponseEntity.ok(new RefreshResponse(
                access,
                Instant.now().plusMillis(jwtService.getAccessExpirationMs()),
                refresh,
                Instant.now().plusMillis(jwtService.getRefreshExpirationMs())));
    }

    private LoginResponse buildLoginResponse(Customer customer) {
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
}
