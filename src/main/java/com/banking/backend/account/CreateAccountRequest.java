package com.banking.backend.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateAccountRequest(
        @NotBlank @Pattern(regexp = "ILS|USD", message = "Currency must be ILS or USD") String currency,
        @NotNull AccountType accountType
) {}
