package com.banking.backend.admin;

import com.banking.backend.transaction.TransactionStatus;
import com.banking.backend.transaction.TransactionType;
import com.banking.backend.transaction.dto.TransactionDto;
import com.banking.backend.transaction.dto.TransactionFilter;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> stats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/customers")
    public ResponseEntity<Page<AdminCustomerDto>> listCustomers(
            @PageableDefault(size = 20, sort = "customerNo") Pageable pageable) {
        return ResponseEntity.ok(adminService.listCustomers(pageable));
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<AdminCustomerDetailDto> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getCustomer(id));
    }

    @PatchMapping("/customers/{id}/status")
    public ResponseEntity<AdminCustomerDetailDto> updateStatus(@PathVariable Long id,
                                                                @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateStatus(id, request));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionDto>> listTransactions(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @PageableDefault(size = 20, sort = "transactionDate") Pageable pageable) {
        TransactionFilter filter = new TransactionFilter(accountId, type, status, from, to);
        return ResponseEntity.ok(adminService.listTransactions(filter, pageable));
    }

    @PostMapping("/accounts/{id}/adjust")
    public ResponseEntity<TransactionDto> adjustBalance(@PathVariable Long id,
                                                         @Valid @RequestBody BalanceAdjustRequest request) {
        return ResponseEntity.ok(adminService.adjustBalance(id, request));
    }
}
