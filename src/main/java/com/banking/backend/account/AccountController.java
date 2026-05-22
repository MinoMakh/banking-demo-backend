package com.banking.backend.account;

import com.banking.backend.dashboard.AccountSummaryDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountSummaryDto> create(@Valid @RequestBody CreateAccountRequest request) {
        String customerNo = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(accountService.openAccount(customerNo, request.currency()));
    }
}
