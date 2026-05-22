package com.banking.backend.auth;

import com.banking.backend.customer.Customer;
import com.banking.backend.customer.CustomerRepository;
import com.banking.backend.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
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
    private final long expirationMs;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          CustomerRepository customerRepository,
                          RegisterService registerService,
                          @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.customerRepository = customerRepository;
        this.registerService = registerService;
        this.expirationMs = expirationMs;
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

        String token = jwtService.generateToken(customer.getCustomerNo(), customer.getRole().name());
        Instant expiresAt = Instant.now().plusMillis(expirationMs);

        return ResponseEntity.ok(new LoginResponse(
                token, expiresAt, customer.getCustomerNo(), customer.getFullName(), customer.getRole()));
    }
}
