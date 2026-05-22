package com.banking.backend.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.banking.backend.customer.Customer;
import com.banking.backend.customer.CustomerRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {
   private final CustomerRepository customerRepository;

   public CustomUserDetailsService(CustomerRepository customerRepository) {
      this.customerRepository = customerRepository;
   }

   @Override
   public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
      Customer customer = customerRepository.findByCustomerNo(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));

      return new User(
         customer.getCustomerNo(),
         customer.getPasswordHash(),
         List.of(new SimpleGrantedAuthority("ROLE_" + customer.getRole().name())));
   }
}
