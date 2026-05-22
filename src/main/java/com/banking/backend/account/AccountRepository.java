package com.banking.backend.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByCustomerId(Long customerId);

    Optional<Account> findByAccountNo(String accountNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNo = :accountNo")
    Optional<Account> findByAccountNoForUpdate(@Param("accountNo") String accountNo);

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(account_no, 4) AS UNSIGNED)), 1000) " +
                   "FROM accounts WHERE account_no LIKE 'ACC%'",
           nativeQuery = true)
    long findMaxAccountNumber();
}
