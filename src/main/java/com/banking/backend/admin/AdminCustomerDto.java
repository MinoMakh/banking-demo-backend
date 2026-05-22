package com.banking.backend.admin;

import com.banking.backend.customer.Customer;
import com.banking.backend.customer.CustomerRole;
import com.banking.backend.customer.CustomerStatus;

public record AdminCustomerDto(
        Long id,
        String customerNo,
        String fullName,
        String mobile,
        CustomerStatus status,
        CustomerRole role,
        int accountCount
) {
    public static AdminCustomerDto from(Customer c, int accountCount) {
        return new AdminCustomerDto(c.getId(), c.getCustomerNo(), c.getFullName(),
                c.getMobile(), c.getStatus(), c.getRole(), accountCount);
    }
}
