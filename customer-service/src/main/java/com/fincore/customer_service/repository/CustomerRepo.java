package com.fincore.customer_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fincore.customer_service.entity.Customer;
import java.util.Optional;

public interface CustomerRepo extends JpaRepository<Customer, Long>{
    Optional<Customer> findByEmail(String email);
}
