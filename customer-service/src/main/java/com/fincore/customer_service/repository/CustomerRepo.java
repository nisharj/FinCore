package com.fincore.customer_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fincore.customer_service.entity.Customer;

public interface CustomerRepo extends JpaRepository<Customer, Long>{

}
