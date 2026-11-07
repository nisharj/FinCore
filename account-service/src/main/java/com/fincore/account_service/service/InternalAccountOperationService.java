package com.fincore.account_service.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fincore.account_service.client.AuthClient;
import com.fincore.account_service.dto.AccountOperationRequest;
import com.fincore.account_service.dto.AccountOperationResponse;
import com.fincore.account_service.dto.CustomerLinkResponse;
import com.fincore.account_service.entity.Account;
import com.fincore.account_service.entity.AccountOperation;
import com.fincore.account_service.enums.AccountOperationType;
import com.fincore.account_service.repository.AccountOperationRepository;
import com.fincore.account_service.repository.AccountRepo;

@Service
public class InternalAccountOperationService {

    private final AccountRepo accountRepo;
    private final AccountOperationRepository operationRepo;
    private final AuthClient authClient;

    public InternalAccountOperationService(
            AccountRepo accountRepo,
            AccountOperationRepository operationRepo,
            AuthClient authClient) {

        this.accountRepo = accountRepo;
        this.operationRepo = operationRepo;
        this.authClient = authClient;
    }

    @Transactional
    public AccountOperationResponse debit(
            Long accountId,
            AccountOperationRequest request,
            String internalSecret) {

        return processOperation(
                accountId,
                request,
                internalSecret,
                AccountOperationType.DEBIT);
    }

    @Transactional
    public AccountOperationResponse credit(Long accountId, AccountOperationRequest request, String internalSecret) {
        return processOperation(
                accountId,
                request,
                internalSecret,
                AccountOperationType.CREDIT);
    }

    private AccountOperationResponse processOperation(Long accountId, AccountOperationRequest request, String internalSecret, AccountOperationType operationType) {
        if (request == null || request.username() == null ||
            request.username().isBlank() || request.transactionReference() == null ||
            request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid username, transaction reference and positive amount are required");
        }

        // Verify the customer's identity using Auth Service.
        CustomerLinkResponse customer = authClient.getCustomerLink(request.username(), internalSecret);

        if (customer == null ||
                !customer.isVerified() ||
                customer.getCustomerId() == null) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Customer verification is required");
        }

        // Lock the account row to prevent concurrent balance modifications.
        Account account = accountRepo.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account not found"));

        // Verify ownership.
        if (!customer.getCustomerId().equals(account.getCustomerId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to operate on this account");
        }

        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Account is not active");
        }

        // Idempotency check.
        var existingOperation = operationRepo.findByTransactionReferenceAndOperationType(
                        request.transactionReference(),
                        operationType);

        if (existingOperation.isPresent()) {
            AccountOperation previous = existingOperation.get();

            if (!previous.getAccountId().equals(accountId) || previous.getAmount().compareTo(request.amount()) != 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction reference was already used with different details");
            }

            return new AccountOperationResponse(
                    account.getId(),
                    account.getBalance(),
                    request.transactionReference(),
                    "ALREADY_PROCESSED");
        }

        BigDecimal newBalance;

        if (operationType == AccountOperationType.DEBIT) {

            if (account.getBalance().compareTo(request.amount()) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Insufficient account balance");
            }

            newBalance = account.getBalance().subtract(request.amount());
        } 
        else {
            newBalance = account.getBalance().add(request.amount());
        }

        account.setBalance(newBalance);

        accountRepo.save(account);

        AccountOperation operation = new AccountOperation(
                request.transactionReference(),
                operationType,
                accountId,
                request.amount());

        operationRepo.save(operation);

        return new AccountOperationResponse(
                account.getId(),
                newBalance,
                request.transactionReference(),
                "SUCCESS");
    }
}