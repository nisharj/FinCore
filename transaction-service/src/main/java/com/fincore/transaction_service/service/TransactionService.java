
package com.fincore.transaction_service.service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.Objects;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fincore.transaction_service.client.AccountClient;
import com.fincore.transaction_service.dto.AccountOperationRequest;
import com.fincore.transaction_service.dto.AccountOperationResponse;
import com.fincore.transaction_service.dto.DepositRequest;
import com.fincore.transaction_service.dto.TransactionResponse;
import com.fincore.transaction_service.dto.WithdrawalRequest;
import com.fincore.transaction_service.dto.TransferRequest;
import com.fincore.transaction_service.entity.Transaction;
import com.fincore.transaction_service.enums.TransactionStatus;
import com.fincore.transaction_service.enums.TransactionType;
import com.fincore.transaction_service.enums.AccountOperationType;
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
        Transaction transaction = findExistingTransaction(idempotencyKey, username, TransactionType.DEPOSIT, null, request.accountId(), request.amount(), request.description());

        if (transaction == null) {
            transaction = new Transaction();
            transaction.setReferenceId(UUID.randomUUID());
            transaction.setIdempotencyKey(idempotencyKey);
            transaction.setToAccountId(request.accountId());
            transaction.setAmount(request.amount());
            transaction.setTransactionType(TransactionType.DEPOSIT);
            transaction.setStatus(TransactionStatus.PENDING);
            transaction.setDescription(request.description());
            transaction.setInitiatedBy(username);

            transactionRepository.saveAndFlush(transaction);
        }

        if(transaction.getStatus() != TransactionStatus.PENDING) {
            return toResponse(transaction);
        }

        try {
            AccountOperationResponse operationStatus = accountClient.getOperationStatus(transaction.getReferenceId(), AccountOperationType.CREDIT, internalSecret);

            if (operationStatus == null || "ALREADY_PROCESSED".equals(operationStatus.status())) {
                transaction.setStatus(TransactionStatus.SUCCESS);
            } 
            else if(operationStatus !=null && "NOT_FOUND".equals(operationStatus.status())){
                AccountOperationRequest operationRequest = new AccountOperationRequest(username, transaction.getReferenceId(), transaction.getAmount());

                AccountOperationResponse response = accountClient.credit(transaction.getToAccountId(), operationRequest, internalSecret);

                if(response !=null && ("SUCCESS".equals(response.status()) || "ALREADY_PROCESSED".equals(response.status()))){
                    transaction.setStatus(TransactionStatus.SUCCESS);
                }
            }
        } 
        catch (FeignException ex) {
            System.err.println("Deposit reconciliation uncertain. HTTP Status: " + ex.status());
        }

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            transaction.setCompletedAt(LocalDateTime.now());
        }

        transactionRepository.save(transaction);
        return toResponse(transaction);
    }


    public TransactionResponse withdraw(WithdrawalRequest request,String username, String idempotencyKey) {
        Transaction transaction = findExistingTransaction(idempotencyKey, username, TransactionType.WITHDRAWAL,request.accountId(),null, request.amount(),request.description());

        if (transaction == null) {
            transaction = new Transaction();
            transaction.setReferenceId(UUID.randomUUID());
            transaction.setIdempotencyKey(idempotencyKey);
            transaction.setFromAccountId(request.accountId());
            transaction.setAmount(request.amount());
            transaction.setTransactionType(TransactionType.WITHDRAWAL);
            transaction.setStatus(TransactionStatus.PENDING);
            transaction.setDescription(request.description());
            transaction.setInitiatedBy(username);

            transactionRepository.saveAndFlush(transaction);
        }

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            return toResponse(transaction);
        }

        try {
            AccountOperationResponse operationStatus =accountClient.getOperationStatus(transaction.getReferenceId(), AccountOperationType.DEBIT, internalSecret);

            if (operationStatus != null && "ALREADY_PROCESSED".equals(operationStatus.status())) {
                transaction.setStatus(TransactionStatus.SUCCESS);
            }

            else if (operationStatus != null && "NOT_FOUND".equals(operationStatus.status())) {
                AccountOperationRequest operationRequest = new AccountOperationRequest(username, transaction.getReferenceId(), transaction.getAmount());
                AccountOperationResponse response = accountClient.debit(transaction.getFromAccountId(), operationRequest, internalSecret);

                if (response != null && ("SUCCESS".equals(response.status())
                        || "ALREADY_PROCESSED".equals(response.status()))) {

                    transaction.setStatus(TransactionStatus.SUCCESS);
                }
            }

        } catch (FeignException ex) {
            System.err.println("Withdrawal reconciliation uncertain. HTTP Status: " + ex.status());
        }

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            transaction.setCompletedAt(LocalDateTime.now());
        }

        transactionRepository.save(transaction);
        return toResponse(transaction);
    }


    public TransactionResponse transfer(TransferRequest request, String username, String idempotencyKey) {
        // 1. Validate source and destination
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination accounts must be different");
        }

        // 2. Check existing transaction
        Transaction existing = findExistingTransaction(
                idempotencyKey, username, TransactionType.TRANSFER, request.fromAccountId(),
                request.toAccountId(), request.amount(), request.description()
        );

        if (existing != null) { return toResponse(existing); }

        // 3. Create transaction
        UUID referenceId = UUID.randomUUID();

        Transaction transaction = new Transaction();
        transaction.setReferenceId(referenceId);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setFromAccountId(request.fromAccountId());
        transaction.setToAccountId(request.toAccountId());
        transaction.setAmount(request.amount());
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setDescription(request.description());
        transaction.setInitiatedBy(username);

        transactionRepository.saveAndFlush(transaction);

        AccountOperationRequest operationRequest = new AccountOperationRequest(username, referenceId, request.amount());

        try {
            // 4. Debit source account
            AccountOperationResponse debitResponse = accountClient.debit(request.fromAccountId(), operationRequest, internalSecret);

            if (debitResponse == null || !("SUCCESS".equals(debitResponse.status()) ||
                            "ALREADY_PROCESSED".equals(debitResponse.status()))) {

                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setCompletedAt(LocalDateTime.now());

                transactionRepository.save(transaction);
                return toResponse(transaction);
            }

            // 5. Credit destination account
            AccountOperationResponse creditResponse = accountClient.credit(request.toAccountId(), operationRequest, internalSecret);

            if (creditResponse != null && ("SUCCESS".equals(creditResponse.status()) ||
                "ALREADY_PROCESSED".equals(creditResponse.status()))) {

                transaction.setStatus(TransactionStatus.SUCCESS);
            } 
            else {
                return toResponse(transaction);
            }
        } 
        catch (FeignException ex) {
            return toResponse(transaction);
        }

        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        return toResponse(transaction);
    }

    private Transaction findExistingTransaction(
            String idempotencyKey, String username, TransactionType type,
            Long fromAccountId, Long toAccountId, BigDecimal amount, String description) {

        var existing = transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isEmpty()) { return null;}

        Transaction transaction = existing.get();

        boolean sameRequest = Objects.equals(transaction.getInitiatedBy(), username)
                && transaction.getTransactionType() == type
                && Objects.equals(transaction.getFromAccountId(), fromAccountId)
                && Objects.equals(transaction.getToAccountId(), toAccountId)
                && transaction.getAmount().compareTo(amount) == 0
                && Objects.equals(transaction.getDescription(), description);

        if (!sameRequest) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key has already been used with a different request");
        }

        return transaction;
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
