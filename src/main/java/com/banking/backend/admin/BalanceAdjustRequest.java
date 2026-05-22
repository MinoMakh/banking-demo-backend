package com.banking.backend.admin;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record BalanceAdjustRequest(
        @NotNull @Digits(integer = 15, fraction = 4) BigDecimal delta,
        @Size(max = 200) String note
) {}
