package com.fincore.account_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.fincore.account_service.entity.Account;
import com.fincore.account_service.service.AccountService;
import com.fincore.account_service.dto.AccountUpdateRequest;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    private boolean isAdmin(Jwt jwt) {
        return "ADMIN".equalsIgnoreCase(jwt.getClaimAsString("role"));
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody Account account, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                accountService.createAccount(
                        account,
                        jwt.getSubject(),
                        isAdmin(jwt)));
    }

    @GetMapping
    public ResponseEntity<List<Account>> getAllAccounts(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                accountService.getAllAccounts(
                        jwt.getSubject(),
                        isAdmin(jwt)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(
                accountService.getAccountById(
                        id,
                        jwt.getSubject(),
                        isAdmin(jwt)));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Account>> getAccountsByCustomerId(
            @PathVariable Long customerId,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(
                accountService.getAccountsByCustomer(
                        customerId,
                        jwt.getSubject(),
                        isAdmin(jwt)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Account> updateAccount(
            @PathVariable Long id,
            @RequestBody AccountUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(accountService.updateAccount(
                        id,
                        request,
                        jwt.getSubject(),
                        isAdmin(jwt)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        accountService.deleteAccount(
                id,
                jwt.getSubject(),
                isAdmin(jwt));

        return ResponseEntity.noContent().build();
    }
}