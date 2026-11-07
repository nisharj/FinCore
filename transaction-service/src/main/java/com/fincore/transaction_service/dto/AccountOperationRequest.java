package com.fincore.transaction_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountOperationRequest(
    String username,
    UUID transactionReference,
    BigDecimal amount
) {}