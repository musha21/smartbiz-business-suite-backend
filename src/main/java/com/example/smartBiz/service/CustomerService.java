package com.example.smartBiz.service;

import com.example.smartBiz.dto.CustomerDto;

import java.util.List;

public interface CustomerService {
    CustomerDto createCustomer(CustomerDto customerDto);

    CustomerDto updateCustomer(Long id, CustomerDto customerDto);

    void deleteCustomer(Long id);

    CustomerDto getCustomerById(Long id);

    List<CustomerDto> getAllCustomers();

    List<CustomerDto> getArchivedCustomers();

    void archiveCustomer(Long id);

    void restoreCustomer(Long id);

}
