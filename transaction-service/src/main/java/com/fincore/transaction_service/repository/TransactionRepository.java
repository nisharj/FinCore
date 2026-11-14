package com.fincore.transaction_service.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fincore.transaction_service.entity.Transaction;
import com.fincore.transaction_service.enums.TransactionStatus;
import com.fincore.transaction_service.enums.TransactionType;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByReferenceId(UUID referenceId);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    List<Transaction> findTop50ByStatusAndTransactionTypeOrderByCreatedAtAsc(TransactionStatus status, TransactionType transactionType);
}