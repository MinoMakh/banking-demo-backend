package com.banking.backend.transaction;

import com.banking.backend.transaction.dto.TransactionDto;
import com.banking.backend.transaction.dto.TransactionFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<Page<TransactionDto>> list(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @PageableDefault(size = 20, sort = "transactionDate") Pageable pageable) {

        String customerNo = SecurityContextHolder.getContext().getAuthentication().getName();
        TransactionFilter filter = new TransactionFilter(accountId, type, status, from, to);
        return ResponseEntity.ok(transactionService.getTransactions(filter, pageable, customerNo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> get(@PathVariable Long id) {
        String customerNo = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(transactionService.getTransaction(id, customerNo));
    }
}
