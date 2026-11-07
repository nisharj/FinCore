package com.fincore.transaction_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fincore.transaction_service.enums.TransactionStatus;
import com.fincore.transaction_service.enums.TransactionType;

public record TransactionResponse(

    UUID referenceId,

    Long fromAccountId,

    Long toAccountId,

    BigDecimal amount,

    TransactionType transactionType,

    TransactionStatus status,

    String description,

    LocalDateTime createdAt,

    LocalDateTime completedAt

) {}