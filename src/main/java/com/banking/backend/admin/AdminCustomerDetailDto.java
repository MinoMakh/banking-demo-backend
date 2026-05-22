package com.banking.backend.admin;

import com.banking.backend.customer.Customer;
import com.banking.backend.customer.CustomerRole;
import com.banking.backend.customer.CustomerStatus;
import com.banking.backend.dashboard.AccountSummaryDto;

import java.util.List;

public record AdminCustomerDetailDto(
        Long id,
        String customerNo,
        String fullName,
        String mobile,
        CustomerStatus status,
        CustomerRole role,
        List<AccountSummaryDto> accounts
) {
    public static AdminCustomerDetailDto from(Customer c, List<AccountSummaryDto> accounts) {
        return new AdminCustomerDetailDto(c.getId(), c.getCustomerNo(), c.getFullName(),
                c.getMobile(), c.getStatus(), c.getRole(), accounts);
    }
}
