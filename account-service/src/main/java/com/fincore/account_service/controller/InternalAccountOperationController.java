package com.fincore.account_service.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

import com.fincore.account_service.enums.AccountOperationType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.fincore.account_service.dto.AccountOperationRequest;
import com.fincore.account_service.dto.AccountOperationResponse;
import com.fincore.account_service.service.InternalAccountOperationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/accounts/internal")
public class InternalAccountOperationController {

    private final InternalAccountOperationService operationService;

    @Value("${fincore.internal.secret}")
    private String internalSecret;

    public InternalAccountOperationController(InternalAccountOperationService operationService) {
        this.operationService = operationService;
    }

    private void validateInternalSecret(String suppliedSecret) {
        if (suppliedSecret == null || !MessageDigest.isEqual(
                        internalSecret.getBytes(StandardCharsets.UTF_8),
                        suppliedSecret.getBytes(StandardCharsets.UTF_8))) {

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal service credentials");
        }
    }

    @PostMapping("/{accountId}/debit")
    public AccountOperationResponse debit(
        @PathVariable Long accountId,
        @Valid @RequestBody AccountOperationRequest request, 
        @RequestHeader("X-Internal-Secret") String secret) {
        
        validateInternalSecret(secret);
        return operationService.debit(accountId, request, secret);
    }

    @PostMapping("/{accountId}/credit")
    public AccountOperationResponse credit(
            @PathVariable Long accountId,
            @Valid @RequestBody AccountOperationRequest request,
            @RequestHeader("X-Internal-Secret") String secret) {

        validateInternalSecret(secret);
        return operationService.credit(accountId, request, secret);
    }

    @GetMapping("/operations/{transactionReference}/{operationType}")
    public AccountOperationResponse getOperationStatus(
            @PathVariable UUID transactionReference,
            @PathVariable AccountOperationType operationType,
            @RequestHeader("X-Internal-Secret") String secret) {

        validateInternalSecret(secret);

        return operationService.getOperationStatus(transactionReference, operationType);
    }

    @PostMapping("/transfers/{accountId}/credit")
    public AccountOperationResponse creditTransfer(
            @PathVariable Long accountId,
            @RequestBody AccountOperationRequest request,
            @RequestHeader("X-Internal-Secret") String secret) {

        validateInternalSecret(secret);

        return operationService.creditTransfer(accountId, request);
    }
}