package com.fincore.account_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record AccountOperationRequest(
        @NotBlank String username,
        @NotNull UUID transactionReference,
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {}