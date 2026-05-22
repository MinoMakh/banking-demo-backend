package com.banking.backend.admin;

import com.banking.backend.customer.CustomerStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull CustomerStatus status) {}
