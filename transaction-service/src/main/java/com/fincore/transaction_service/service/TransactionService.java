
package com.fincore.transaction_service.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fincore.transaction_service.client.AccountClient;
import com.fincore.transaction_service.dto.AccountOperationRequest;
import com.fincore.transaction_service.dto.AccountOperationResponse;
import com.fincore.transaction_service.dto.DepositRequest;
import com.fincore.transaction_service.dto.TransactionResponse;
import com.fincore.transaction_service.entity.Transaction;
import com.fincore.transaction_service.enums.TransactionStatus;
import com.fincore.transaction_service.enums.TransactionType;
import com.fincore.transaction_service.repository.TransactionRepository;

import feign.FeignException;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;

    @Value("${fincore.internal.secret}")
    private String internalSecret;

    public TransactionService(TransactionRepository transactionRepository, AccountClient accountClient) {
        this.transactionRepository = transactionRepository;
        this.accountClient = accountClient;
    }

    public TransactionResponse deposit(DepositRequest request, String username, String idempotencyKey) {

        // 1. Check idempotency
        var existing = transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        // 2. Create transaction
        UUID referenceId = UUID.randomUUID();

        Transaction transaction = new Transaction();
        transaction.setReferenceId(referenceId);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setToAccountId(request.accountId());
        transaction.setAmount(request.amount());
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setDescription(request.description());

        // Save PENDING before calling Account Service
        transactionRepository.saveAndFlush(transaction);

        try {
            // 3. Call Account Service to credit the account
            AccountOperationRequest operationRequest = new AccountOperationRequest(username, referenceId, request.amount());
            AccountOperationResponse operationResponse = accountClient.credit( request.accountId(), operationRequest, internalSecret);

            // 4. Update transaction status
            if (operationResponse == null ||
                    !("SUCCESS".equals(operationResponse.status())
                            || "ALREADY_PROCESSED".equals(operationResponse.status()))) {

                transaction.setStatus(TransactionStatus.FAILED);
            } 
            else {
                transaction.setStatus(TransactionStatus.SUCCESS);
            }

        } 
        catch (FeignException ex) {
            transaction.setStatus(TransactionStatus.FAILED);
        }

        // 5. Persist final status
        transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getReferenceId(),
                transaction.getFromAccountId(),
                transaction.getToAccountId(),
                transaction.getAmount(),
                transaction.getTransactionType(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getCreatedAt(),
                transaction.getCompletedAt());
    }
}
