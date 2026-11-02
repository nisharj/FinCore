package com.fincore.account_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fincore.account_service.client.AuthClient;
import com.fincore.account_service.client.CustomerClient;
import com.fincore.account_service.dto.CustomerLinkResponse;
import com.fincore.account_service.entity.Account;
import com.fincore.account_service.exception.ResourceNotFoundException;
import com.fincore.account_service.repository.AccountRepo;
import com.fincore.account_service.dto.AccountUpdateRequest;

@Service
public class AccountService {

    private final AccountRepo accountRepo;
    private final CustomerClient customerClient;
    private final AuthClient authClient;

    @Value("${fincore.internal.secret}")
    private String internalSecret;

    public AccountService(AccountRepo accountRepo, CustomerClient customerClient, AuthClient authClient) {
        this.accountRepo = accountRepo;
        this.customerClient = customerClient;
        this.authClient = authClient;
    }

    private Long getVerifiedCustomerId(String username) {

        CustomerLinkResponse response = authClient.getCustomerLink(username, internalSecret);

        if (response == null || !response.isVerified() || response.getCustomerId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Customer verification is required");
        }

        return response.getCustomerId();
    }

    public Account createAccount(
            Account account,
            String username,
            boolean isAdmin) {

        if (!isAdmin) {
            Long customerId = getVerifiedCustomerId(username);
            account.setCustomerId(customerId);
        }

        if (account.getCustomerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer ID is required");
        }

        customerClient.getCustomerById(account.getCustomerId());

        return accountRepo.save(account);
    }


    public List<Account> getAllAccounts(String username, boolean isAdmin) {
        if (isAdmin) {
            return accountRepo.findAll();
        }

        Long customerId = getVerifiedCustomerId(username);

        return accountRepo.findByCustomerId(customerId);
    }

    // Get account by ID
    public Account getAccountById(
            Long id,
            String username,
            boolean isAdmin) {

        Account account = findAccount(id);

        checkOwnership(account, username, isAdmin);

        return account;
    }

    // Get accounts by customer ID
    public List<Account> getAccountsByCustomer(
            Long customerId,
            String username,
            boolean isAdmin) {

        if (!isAdmin) {
            Long loggedInCustomerId = getVerifiedCustomerId(username);

            if (!loggedInCustomerId.equals(customerId)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You cannot access another customer's accounts");
            }
        }

        return accountRepo.findByCustomerId(customerId);
    }

    public Account updateAccount(
            Long id,
            AccountUpdateRequest request,
            String username,
            boolean isAdmin) {

        Account existing = findAccount(id);

        checkOwnership(existing, username, isAdmin);

        if (request.getAccountType() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Account type is required");
        }

        existing.setAccountType(request.getAccountType());

        return accountRepo.save(existing);
    }

    public void deleteAccount(
            Long id,
            String username,
            boolean isAdmin) {

        Account existing = findAccount(id);

        checkOwnership(existing, username, isAdmin);

        accountRepo.delete(existing);
    }

    private Account findAccount(Long id) {
        return accountRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with ID: " + id));
    }

    private void checkOwnership(
            Account account,
            String username,
            boolean isAdmin) {

        if (isAdmin) {
            return;
        }

        Long customerId = getVerifiedCustomerId(username);

        if (!customerId.equals(account.getCustomerId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to access this account");
        }
    }
}