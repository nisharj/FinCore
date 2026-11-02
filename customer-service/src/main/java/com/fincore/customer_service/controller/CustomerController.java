package com.fincore.customer_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fincore.customer_service.entity.Customer;
import com.fincore.customer_service.service.CustomerService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.fincore.customer_service.dto.InternalCustomerResponse;

import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    @Value("${fincore.internal.secret}")
    private String internalSecret;

    private final CustomerService customerService;
    
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<Customer> createCustomer(
            @RequestBody Customer customer) {

        return ResponseEntity.ok(
                customerService.createCustomer(customer)
        );
    }

    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {

        return ResponseEntity.ok(
                customerService.getAllCustomers()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomer(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                customerService.getCustomerById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(
            @PathVariable Long id,
            @RequestBody Customer customer) {

        return ResponseEntity.ok(
                customerService.updateCustomer(id, customer)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable Long id) {

        customerService.deleteCustomer(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<Customer> getCustomerByEmail(@PathVariable String email) {
        return ResponseEntity.ok(customerService.getCustomerByEmail(email));
    }


    @GetMapping("/internal/by-email")
    public ResponseEntity<InternalCustomerResponse> getCustomerForVerification(@RequestParam String email, @RequestHeader("X-Internal-Secret") String suppliedSecret) {
        if (suppliedSecret == null || !java.security.MessageDigest.isEqual(
                    internalSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    suppliedSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal secret");
        }

        Customer customer = customerService.getCustomerByEmail(email);

        return ResponseEntity.ok(
                new InternalCustomerResponse(
                        customer.getId(),
                        customer.getEmail()));
    }
}
