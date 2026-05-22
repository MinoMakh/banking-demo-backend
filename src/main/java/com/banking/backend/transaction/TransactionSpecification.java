package com.banking.backend.transaction;

import com.banking.backend.transaction.dto.TransactionFilter;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

public class TransactionSpecification {

    public static Specification<Transaction> forFilter(TransactionFilter f, Set<Long> allowedAccountIds) {
        Specification<Transaction> base = allowedAccountIds != null
                ? accountIn(allowedAccountIds)
                : (root, query, cb) -> cb.conjunction();
        return base
                .and(hasAccount(f.accountId()))
                .and(hasType(f.type()))
                .and(hasStatus(f.status()))
                .and(dateFrom(f.from()))
                .and(dateTo(f.to()));
    }

    private static Specification<Transaction> accountIn(Set<Long> ids) {
        return (root, query, cb) -> root.get("account").get("id").in(ids);
    }

    private static Specification<Transaction> hasAccount(Long accountId) {
        return (root, query, cb) ->
                accountId == null ? null : cb.equal(root.get("account").get("id"), accountId);
    }

    private static Specification<Transaction> hasType(TransactionType type) {
        return (root, query, cb) ->
                type == null ? null : cb.equal(root.get("type"), type);
    }

    private static Specification<Transaction> hasStatus(TransactionStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    private static Specification<Transaction> dateFrom(java.time.LocalDate from) {
        return (root, query, cb) ->
                from == null ? null : cb.greaterThanOrEqualTo(root.get("transactionDate"), from.atStartOfDay());
    }

    private static Specification<Transaction> dateTo(java.time.LocalDate to) {
        return (root, query, cb) ->
                to == null ? null : cb.lessThan(root.get("transactionDate"), to.plusDays(1).atStartOfDay());
    }
}
