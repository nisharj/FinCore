package com.fincore.account_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fincore.account_service.entity.Account;

public interface AccountRepo extends JpaRepository<Account, Long> {
    List<Account> findByCustomerId(Long customerId);
}
