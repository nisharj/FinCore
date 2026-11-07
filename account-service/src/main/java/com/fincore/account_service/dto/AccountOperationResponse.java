package com.fincore.account_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountOperationResponse(
        Long accountId,
        BigDecimal balance,
        UUID transactionReference,
        String status
) {}