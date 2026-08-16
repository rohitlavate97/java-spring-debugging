package com.eopis.customer.service;

import com.eopis.customer.dto.CustomerResponseDto;
import java.util.List;

public interface CustomerService {
    CustomerResponseDto getCustomerById(Long id);
    CustomerResponseDto getCustomerByNumber(String customerNumber);
    List<CustomerResponseDto> getAllCustomers();
}
