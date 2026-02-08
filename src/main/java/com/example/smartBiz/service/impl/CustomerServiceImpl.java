package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.CustomerDto;
import com.example.smartBiz.entity.Customer;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.CustomerRepo;
import com.example.smartBiz.security.RequestContext;
import com.example.smartBiz.service.CustomerService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepo customerRepository;
    private final RequestContext requestContext;

    public CustomerServiceImpl(CustomerRepo customerRepository, RequestContext requestContext) {
        this.customerRepository = customerRepository;
        this.requestContext = requestContext;
    }

    // ✅ reduce duplicates
    private Long requireBusinessId() {
        Long businessId = requestContext.getBusinessId();
        if (businessId == null) {
            throw new RuntimeException("Business context missing (JWT required)");
        }
        return businessId;
    }

    // ✅ ownership check
    private Customer requireOwnedCustomer(Long id, Long businessId) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + id));

        if (c.getBusinessId() == null || !c.getBusinessId().equals(businessId)) {
            throw new RuntimeException("Access denied: customer not in your business");
        }
        return c;
    }

    @Override
    public CustomerDto createCustomer(CustomerDto dto) {
        Long businessId = requireBusinessId();

        Customer customer = mapToEntity(dto);
        customer.setBusinessId(businessId);

        return mapToDto(customerRepository.save(customer));
    }

    @Override
    public CustomerDto updateCustomer(Long id, CustomerDto dto) {
        Long businessId = requireBusinessId();
        Customer customer = requireOwnedCustomer(id, businessId);

        customer.setName(dto.getName());
        customer.setEmail(dto.getEmail());
        customer.setPhone(dto.getPhone());
        customer.setAddress(dto.getAddress());

        return mapToDto(customerRepository.save(customer));
    }

    @Override
    public void deleteCustomer(Long id) {
        Long businessId = requireBusinessId();
        Customer customer = requireOwnedCustomer(id, businessId);
        customerRepository.delete(customer);
    }

    @Override
    public CustomerDto getCustomerById(Long id) {
        Long businessId = requireBusinessId();
        Customer customer = requireOwnedCustomer(id, businessId);
        return mapToDto(customer);
    }

    @Override
    public List<CustomerDto> getAllCustomers() {
        Long businessId = requireBusinessId();

        // ✅ IMPORTANT: never use findAll() in multi-business
        return customerRepository.findByBusinessId(businessId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    // -------------------------
    // Mapping
    // -------------------------
    private Customer mapToEntity(CustomerDto dto) {
        Customer customer = new Customer();
        customer.setName(dto.getName());
        customer.setEmail(dto.getEmail());
        customer.setPhone(dto.getPhone());
        customer.setAddress(dto.getAddress());
        return customer;
    }

    private CustomerDto mapToDto(Customer customer) {
        CustomerDto dto = new CustomerDto();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setAddress(customer.getAddress());
        return dto;
    }
}
