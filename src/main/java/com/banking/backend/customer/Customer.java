package com.banking.backend.customer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_no", nullable = false, unique = true)
    private String customerNo;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String mobile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerRole role;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
}
