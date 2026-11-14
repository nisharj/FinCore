
package com.fincore.transaction_service.service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.Objects;
import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;

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

    private final ObjectMapper objectMapper;
    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final EntityManager entityManager;

    @Value("${fincore.internal.secret}")
    private String internalSecret;

    public TransactionService(TransactionRepository transactionRepository, AccountClient accountClient, ObjectMapper objectMapper,EntityManager entityManager) {
        this.transactionRepository = transactionRepository;
        this.accountClient = accountClient;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
    }

    public TransactionResponse deposit(DepositRequest request, String username, String idempotencyKey) {
        lockIdempotencyKey(idempotencyKey);

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

            // transactionRepository.saveAndFlush(transaction);
            transaction = saveOrGetExisting(
                    transaction, username,
                    TransactionType.DEPOSIT,null,
                    request.accountId(),request.amount(),
                    request.description()
            );
        }

        if(transaction.getStatus() != TransactionStatus.PENDING) {
            return toResponse(transaction);
        }

        try {
            AccountOperationResponse operationStatus = accountClient.getOperationStatus(transaction.getReferenceId(), AccountOperationType.CREDIT, internalSecret);

            if (operationStatus != null  && "ALREADY_PROCESSED".equals(operationStatus.status())) {
                transaction.setStatus(TransactionStatus.SUCCESS);
            } 
            else if(operationStatus !=null && "NOT_FOUND".equals(operationStatus.status())){
                AccountOperationRequest operationRequest = new AccountOperationRequest(transaction.getInitiatedBy(), transaction.getReferenceId(), transaction.getAmount());
                AccountOperationResponse response = accountClient.credit(transaction.getToAccountId(), operationRequest, internalSecret);

                if(response !=null && ("SUCCESS".equals(response.status()) || "ALREADY_PROCESSED".equals(response.status()))){
                    transaction.setStatus(TransactionStatus.SUCCESS);
                }
            }
        } 
        catch (FeignException ex) {

            AccountFailure failure = extractAccountFailure(ex);
            if (isConfirmedBusinessFailure(failure)) {
                markFailed(transaction, failure);
                return toResponse(transaction);
            }

            System.err.println("Deposit outcome uncertain. HTTP Status: " + ex.status());
        }

        if (transaction.getStatus() == TransactionStatus.SUCCESS && transaction.getCompletedAt() == null) {
            transaction.setCompletedAt(LocalDateTime.now());
        }

        transactionRepository.save(transaction);
        return toResponse(transaction);
    }


    public TransactionResponse withdraw(WithdrawalRequest request,String username, String idempotencyKey) {
        lockIdempotencyKey(idempotencyKey);

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

            // transactionRepository.saveAndFlush(transaction);
            transaction = saveOrGetExisting(
                    transaction,username,
                    TransactionType.WITHDRAWAL,request.accountId(),
                    null,request.amount(), request.description()
            );
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

        } 
        catch (FeignException ex) {

            AccountFailure failure = extractAccountFailure(ex);

            if (isConfirmedBusinessFailure(failure)) {
                markFailed(transaction, failure);
                return toResponse(transaction);
            }

            System.err.println("Withdrawal outcome uncertain. HTTP Status: " + ex.status());
        }

        if (transaction.getStatus() == TransactionStatus.SUCCESS && transaction.getCompletedAt()==null) {
            transaction.setCompletedAt(LocalDateTime.now());
        }

        transactionRepository.save(transaction);
        return toResponse(transaction);
    }

    
    public TransactionResponse transfer(TransferRequest request, String username, String idempotencyKey) {
        lockIdempotencyKey(idempotencyKey);

        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination accounts must be different");
        }

        // 2. Find an existing transaction using the idempotency key
        Transaction transaction = findExistingTransaction(
                idempotencyKey, username,
                TransactionType.TRANSFER, request.fromAccountId(),
                request.toAccountId(), request.amount(),
                request.description()
        );

        // Return only if the previous transaction is already completed
        if (transaction != null && transaction.getStatus() != TransactionStatus.PENDING) {
            return toResponse(transaction);
        }

        // 3. Create a new transaction only if one doesn't already exist
        if (transaction == null) {
            transaction = new Transaction();
            transaction.setReferenceId(UUID.randomUUID());
            transaction.setIdempotencyKey(idempotencyKey);
            transaction.setFromAccountId(request.fromAccountId());
            transaction.setToAccountId(request.toAccountId());
            transaction.setAmount(request.amount());
            transaction.setTransactionType(TransactionType.TRANSFER);
            transaction.setStatus(TransactionStatus.PENDING);
            transaction.setDescription(request.description());
            transaction.setInitiatedBy(username);

            // transactionRepository.saveAndFlush(transaction);
            transaction = saveOrGetExisting(
                    transaction,username,
                    TransactionType.TRANSFER,
                    request.fromAccountId(), request.toAccountId(),
                    request.amount(), request.description()
            );
        }

        return processTransfer(transaction);
    }

    private TransactionResponse processTransfer(Transaction transaction) {

        UUID referenceId = transaction.getReferenceId();
        AccountOperationRequest operationRequest = new AccountOperationRequest(transaction.getInitiatedBy(), referenceId, transaction.getAmount());
        boolean debitConfirmed = false;

        // STEP 1: Verify sender debit
        try {
            AccountOperationResponse debitStatus =
                    accountClient.getOperationStatus(referenceId, AccountOperationType.DEBIT, internalSecret);

            if (debitStatus == null) {
                return toResponse(transaction);
            }

            if ("NOT_FOUND".equals(debitStatus.status())) {

                AccountOperationResponse debitResponse =
                        accountClient.debit(
                                transaction.getFromAccountId(),
                                operationRequest, internalSecret
                        );

                if (debitResponse == null || !("SUCCESS".equals(debitResponse.status()) || "ALREADY_PROCESSED".equals(debitResponse.status()))) {
                    return toResponse(transaction);
                }

            } 
            else if (!"ALREADY_PROCESSED".equals(debitStatus.status())) {
                return toResponse(transaction);
            }

            debitConfirmed = true;

        } 
        catch (FeignException ex) {
            AccountFailure failure = extractAccountFailure(ex);

            if (!debitConfirmed && isConfirmedBusinessFailure(failure)) {
                markFailed(transaction, failure);
                return toResponse(transaction);
            }

            System.err.println("Transfer debit outcome uncertain. HTTP Status: " + ex.status());
            return toResponse(transaction);
        }


        // STEP 2: Verify recipient credit
        try {
            AccountOperationResponse creditStatus =
                    accountClient.getOperationStatus(
                            referenceId,
                            AccountOperationType.CREDIT,
                            internalSecret
                    );

            if (creditStatus == null) {
                return toResponse(transaction);
            }

            if ("NOT_FOUND".equals(creditStatus.status())) {

                AccountOperationResponse creditResponse =
                        accountClient.creditTransfer(
                                transaction.getToAccountId(),
                                operationRequest, internalSecret
                        );

                if (creditResponse == null || !("SUCCESS".equals(creditResponse.status()) || "ALREADY_PROCESSED".equals(creditResponse.status()))) {
                    return toResponse(transaction);
                }

            } 
            else if (!"ALREADY_PROCESSED".equals(creditStatus.status())) {
                return toResponse(transaction);
            }

        }
        catch (FeignException ex) {
            AccountFailure failure = extractAccountFailure(ex);

            if (failure != null) {
                String reason = "Destination credit pending: " + failure.errorCode() + ": " + failure.message();

                transaction.setFailureReason(reason.substring(0, Math.min(reason.length(), 500)));
                transactionRepository.save(transaction);
            }

            System.err.println("Transfer credit pending. HTTP Status: " + ex.status());
            return toResponse(transaction);
        }

        // STEP 3: Both operations are confirmed
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setFailureReason(null);

        if (transaction.getCompletedAt() == null) {
            transaction.setCompletedAt(LocalDateTime.now());
        }

        transactionRepository.save(transaction);

        return toResponse(transaction);
    }

    
    public void reconcilePendingTransfers() {
        var pendingTransfers =
                transactionRepository.findTop50ByStatusAndTransactionTypeOrderByCreatedAtAsc(
                    TransactionStatus.PENDING,
                    TransactionType.TRANSFER
                );

        for (Transaction transaction : pendingTransfers) {
            try {
                processTransfer(transaction);
            } 
            catch (Exception ex) {
                System.err.println("Failed to reconcile transfer " + transaction.getReferenceId() + ": " + ex.getMessage());
            }
        }
    }

    public void reconcilePendingDeposits() {

        var pendingDeposits =
                transactionRepository.findTop50ByStatusAndTransactionTypeOrderByCreatedAtAsc(
                        TransactionStatus.PENDING,
                        TransactionType.DEPOSIT
                );

        for (Transaction transaction : pendingDeposits) {

            try {
                DepositRequest request = new DepositRequest(
                        transaction.getToAccountId(),
                        transaction.getAmount(),
                        transaction.getDescription()
                );

                deposit(
                        request,
                        transaction.getInitiatedBy(),
                        transaction.getIdempotencyKey()
                );

            } catch (Exception ex) {

                System.err.println(
                        "Failed to reconcile deposit "
                        + transaction.getReferenceId()
                        + ": " + ex.getMessage()
                );
            }
        }
    }


    public void reconcilePendingWithdrawals() {

        var pendingWithdrawals =
                transactionRepository.findTop50ByStatusAndTransactionTypeOrderByCreatedAtAsc(
                        TransactionStatus.PENDING,
                        TransactionType.WITHDRAWAL
                );

        for (Transaction transaction : pendingWithdrawals) {

            try {
                WithdrawalRequest request = new WithdrawalRequest(
                        transaction.getFromAccountId(),
                        transaction.getAmount(),
                        transaction.getDescription()
                );

                withdraw(
                        request,
                        transaction.getInitiatedBy(),
                        transaction.getIdempotencyKey()
                );

            } catch (Exception ex) {

                System.err.println(
                        "Failed to reconcile withdrawal "
                        + transaction.getReferenceId()
                        + ": " + ex.getMessage()
                );
            }
        }
    }

    private Transaction saveOrGetExisting(
            Transaction transaction, String username,
            TransactionType type,Long fromAccountId,
            Long toAccountId,BigDecimal amount, String description) {

        try {
            return transactionRepository.saveAndFlush(transaction);
        } 
        catch (DataIntegrityViolationException ex) {

            Transaction existing = findExistingTransaction(
                    transaction.getIdempotencyKey(),
                    username,type,
                    fromAccountId,toAccountId,
                    amount, description
            );

            if (existing != null) {
                return existing;
            }

            // The constraint violation was not caused by an existing
            // matching idempotency key.
            throw ex;
        }
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

    private record AccountFailure(String errorCode, String message) {}

    private AccountFailure extractAccountFailure(FeignException ex) {

        try {
            String responseBody = ex.contentUTF8();

            if (responseBody == null || responseBody.isBlank()) {
                return null;
            }

            JsonNode root = objectMapper.readTree(responseBody);

            String errorCode = root.hasNonNull("errorCode")
                    ? root.get("errorCode").asText() : null;

            String message = root.hasNonNull("message")
                    ? root.get("message").asText() : null;

            if (errorCode == null || message == null) {
                return null;
            }

            return new AccountFailure(errorCode, message);

        } catch (Exception ex2) {
            System.err.println("Unable to parse Account Service error response");
            return null;
        }
    }

    private boolean isConfirmedBusinessFailure(AccountFailure failure) {

        if (failure == null) { return false; }

        return switch (failure.errorCode()) {
            case "ACCOUNT_NOT_FOUND", "INSUFFICIENT_BALANCE",
                "ACCOUNT_INACTIVE", "ACCOUNT_ACCESS_DENIED",
                "CUSTOMER_NOT_VERIFIED" -> true;

            default -> false;
        };
    }


    private void markFailed(Transaction transaction, AccountFailure failure) {

        if (!isConfirmedBusinessFailure(failure)) {
            return;
        }

        String reason = failure.errorCode() + ": " + failure.message();

        if (reason.length() > 500) {
            reason = reason.substring(0, 500);
        }

        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureReason(reason);
        transaction.setCompletedAt(LocalDateTime.now());

        transactionRepository.save(transaction);
    }


    private void lockIdempotencyKey(String idempotencyKey) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(CAST(:lockKey AS text), 0))")
                .setParameter("lockKey", idempotencyKey)
                .getSingleResult();
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
                transaction.getFailureReason(),
                transaction.getCreatedAt(),
                transaction.getCompletedAt());
    }
}
