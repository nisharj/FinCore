package com.fincore.account_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fincore.account_service.entity.AccountOperation;
import com.fincore.account_service.enums.AccountOperationType;

@Repository
public interface AccountOperationRepository extends JpaRepository<AccountOperation, Long> {
    Optional<AccountOperation> findByTransactionReferenceAndOperationType(
        UUID transactionReference, AccountOperationType operationType
    );
}