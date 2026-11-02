package com.fincore.account_service.dto;

import com.fincore.account_service.entity.AccountType;

public class AccountUpdateRequest {

    private AccountType accountType;

    public AccountType getAccountType() {return accountType;}

    public void setAccountType(AccountType accountType) { this.accountType = accountType; }
}