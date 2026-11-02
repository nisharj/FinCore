package com.fincore.account_service.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fincore.account_service.client.CustomerClient;
import com.fincore.account_service.entity.Account;
import com.fincore.account_service.repository.AccountRepo;
import com.fincore.account_service.exception.ResourceNotFoundException;

@Service
public class AccountService {

    private final AccountRepo accountRepo;
    private final CustomerClient customerClient;

    public AccountService(AccountRepo accountRepo, CustomerClient customerClient) {
        this.accountRepo = accountRepo;
        this.customerClient = customerClient;
    }

    public Account createAccount(Account account) {
        customerClient.getCustomerById(account.getCustomerId());
        return accountRepo.save(account);
    }

    public List<Account> getAllAccounts() {
        return accountRepo.findAll();
    }

    public Account getAccountById(Long id) {
        return accountRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + id));
    }

    public List<Account> getAccountsByCustomer(Long customerId) {
        return accountRepo.findByCustomerId(customerId);
    }

    public Account updateAccount(Long id, Account account) {
        Account existing = getAccountById(id);

        customerClient.getCustomerById(account.getCustomerId());

        existing.setAccountNumber(account.getAccountNumber());
        existing.setCustomerId(account.getCustomerId());
        existing.setAccountType(account.getAccountType());
        existing.setBalance(account.getBalance());
        existing.setStatus(account.getStatus());

        return accountRepo.save(existing);
    }

    public void deleteAccount(Long id) {
        Account existing = getAccountById(id);
        accountRepo.delete(existing);
    }
}
