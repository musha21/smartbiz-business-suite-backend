package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.CustomerDto;
import com.example.smartBiz.service.CustomerService;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class CustomerServiceImpl implements CustomerService {
    @Override
    public CustomerDto createCustomer(CustomerDto customerDto) {
        return null;
    }

    @Override
    public CustomerDto updateCustomer(Long id, CustomerDto customerDto) {
        return null;
    }

    @Override
    public void deleteCustomer(Long id) {

    }

    @Override
    public CustomerDto getCustomerById(Long id) {
        return null;
    }

    @Override
    public List<CustomerDto> getAllCustomers() {
        return List.of();
    }
}
