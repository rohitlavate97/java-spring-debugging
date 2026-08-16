package com.eopis.customer.controller;

import com.eopis.customer.dto.CustomerResponseDto;
import com.eopis.customer.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponseDto>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @GetMapping("/number/{customerNumber}")
    public ResponseEntity<CustomerResponseDto> getCustomerByNumber(@PathVariable String customerNumber) {
        return ResponseEntity.ok(customerService.getCustomerByNumber(customerNumber));
    }
}
