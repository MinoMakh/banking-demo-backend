package com.banking.backend.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerNo(String customerNo);

    boolean existsByMobile(String mobile);

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(customer_no, 2) AS UNSIGNED)), 0) " +
                   "FROM customers WHERE customer_no LIKE 'C%'",
           nativeQuery = true)
    long findMaxUserCustomerNumber();
}
