
package com.fincore.transaction_service.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.annotation.Validated;

import com.fincore.transaction_service.dto.TransactionResponse;
import com.fincore.transaction_service.dto.DepositRequest;
import com.fincore.transaction_service.dto.WithdrawalRequest;
import com.fincore.transaction_service.dto.TransferRequest;
import com.fincore.transaction_service.service.TransactionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/transactions")
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // =========================
    // DEPOSIT
    // =========================

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody DepositRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication) {

        if (idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key must contain 1 to 100 characters");
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        TransactionResponse response = transactionService.deposit(
                request,
                authentication.getName(),
                idempotencyKey);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @Valid @RequestBody WithdrawalRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication) {

        if (idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid Idempotency-Key");
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required");
        }

        TransactionResponse response = transactionService.withdraw(request, authentication.getName(),idempotencyKey);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication) {

        if (idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid Idempotency-Key");
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        TransactionResponse response = transactionService.transfer(request, authentication.getName(),idempotencyKey);

        return ResponseEntity.ok(response);
    }


    // =========================
    // TRANSACTION HISTORY
    // =========================

    @GetMapping("/history")
    public ResponseEntity<Page<TransactionResponse>> getMyTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required");
        }

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid pagination parameters");
        }

        Pageable pageable = PageRequest.of(
                page,size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<TransactionResponse> response =transactionService.getMyTransactions(authentication.getName(),pageable);

        return ResponseEntity.ok(response);
    }


    // =========================
    // TRANSACTION BY REFERENCE
    // =========================

    @GetMapping("/{referenceId}")
    public ResponseEntity<TransactionResponse> getTransactionByReference(@PathVariable UUID referenceId,Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        TransactionResponse response = transactionService.getTransactionByReference(
                                    referenceId,
                                    authentication.getName()
                                );

        return ResponseEntity.ok(response);
    }
}
