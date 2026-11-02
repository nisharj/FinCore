package com.fincore.customer_service.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fincore.customer_service.entity.Customer;
import com.fincore.customer_service.repository.CustomerRepo;
import com.fincore.customer_service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepo customerRepo;

    public Customer createCustomer(Customer customer) {
        return customerRepo.save(customer);
    }

    public List<Customer> getAllCustomers() {
        return customerRepo.findAll();
    }

    public Customer getCustomerById(Long id) {
        return customerRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));
    }

    public Customer updateCustomer(Long id, Customer customer) {
        Customer existingCustomer = getCustomerById(id);

        existingCustomer.setName(customer.getName());
        existingCustomer.setEmail(customer.getEmail());
        existingCustomer.setPhoneNumber(customer.getPhoneNumber());
        existingCustomer.setAddress(customer.getAddress());
        existingCustomer.setKycStatus(customer.getKycStatus());

        return customerRepo.save(existingCustomer);
    }

    public void deleteCustomer(Long id) {
        customerRepo.deleteById(id);
    }

    public Customer getCustomerByEmail(String email) {
        return customerRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with email: " + email));
    }
}
