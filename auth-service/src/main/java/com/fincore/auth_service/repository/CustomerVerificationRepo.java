package com.fincore.auth_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fincore.auth_service.entity.CustomerVerification;

public interface CustomerVerificationRepo extends JpaRepository<CustomerVerification, Long> {

    Optional<CustomerVerification> findTopByUsernameAndUsedFalseOrderByIdDesc(String username);

    void deleteByUsername(String username);
}
