package com.banking.backend.transfer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank String fromAccountNo,
        @NotBlank String toAccountNo,
        @NotNull @DecimalMin(value = "0.01", inclusive = true)
        @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @Size(max = 200) String note
) {}
