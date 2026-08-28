package com.fincore.customer_service.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "customers")
public class Customer{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String phoneNumber;
    private String address;
    private String kycStatus;

    public Customer(){}

    public Customer(String name, String email, String phoneNumber, String address, String kycStatus) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.kycStatus = kycStatus;
    }

    public Long getId() {
        return id;
    }
}