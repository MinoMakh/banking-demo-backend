package com.banking.backend.transfer;

import com.banking.backend.transaction.dto.TransactionDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransactionDto> transfer(@Valid @RequestBody TransferRequest request) {
        String customerNo = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(transferService.transfer(request, customerNo));
    }
}
