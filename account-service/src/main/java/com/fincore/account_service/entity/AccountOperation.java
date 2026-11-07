package com.fincore.account_service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fincore.account_service.enums.AccountOperationType;

import jakarta.persistence.*;

@Entity
@Table(
    name = "account_operations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_account_operation_reference_type",
            columnNames = {"transaction_reference", "operation_type"}
        )
    }
)
public class AccountOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_reference", nullable = false)
    private UUID transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private AccountOperationType operationType;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected AccountOperation() {}

    public AccountOperation(UUID transactionReference, AccountOperationType operationType, Long accountId, BigDecimal amount) {
        this.transactionReference = transactionReference;
        this.operationType = operationType;
        this.accountId = accountId;
        this.amount = amount;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public UUID getTransactionReference() { return transactionReference; }
    public AccountOperationType getOperationType() { return operationType; }
    public Long getAccountId() { return accountId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}