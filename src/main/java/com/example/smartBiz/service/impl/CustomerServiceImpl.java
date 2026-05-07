package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.CustomerDto;
import com.example.smartBiz.entity.Customer;
import com.example.smartBiz.entity.Subscription;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.CustomerRepo;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.CustomerService;
import com.example.smartBiz.service.PlanLimitService;
import com.example.smartBiz.service.SubscriptionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepo customerRepository;
    private final SubscriptionService subscriptionService;
    private final PlanLimitService planLimitService;

    public CustomerServiceImpl(CustomerRepo customerRepository,
            SubscriptionService subscriptionService,
            PlanLimitService planLimitService) {
        this.customerRepository = customerRepository;
        this.subscriptionService = subscriptionService;
        this.planLimitService = planLimitService;
    }

    // ✅ reduce duplicates
    private Long requireBusinessId() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        if (principal == null || principal.getBusinessId() == null) {
            throw new ResourceNotFoundException("Business context missing (JWT required)");
        }
        return principal.getBusinessId();
    }

    // ✅ ownership check
    private Customer requireOwnedCustomer(Long id, Long businessId) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + id));

        if (c.getBusinessId() == null || !c.getBusinessId().equals(businessId)) {
            throw new ResourceNotFoundException("Access denied: customer not in your business");
        }
        return c;
    }

    @Override
    public CustomerDto createCustomer(CustomerDto dto) {
        Long businessId = requireBusinessId();

        // ✅ Plan Limit Check: MAX_CUSTOMERS
        Subscription sub = subscriptionService.getBusinessSubscription(businessId);
        if (sub != null && sub.getPlan() != null) {
            Long limit = planLimitService.getLimitValueOrDefault(sub.getPlan().getId(), "MAX_CUSTOMERS", -1L);
                if (limit != -1) {
                    long currentCount = customerRepository.countByBusinessIdAndArchivedFalse(businessId);
                if (currentCount >= limit) {
                    throw new ResourceNotFoundException("Customer limit reached (" + limit + "). Upgrade your plan.");
                }
            }
        }

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
        // customerRepository.delete(customer); 
        // Logic moved to archiveCustomer as per requirement
        archiveCustomer(id);
    }

    @Override
    public List<CustomerDto> getArchivedCustomers() {
        Long businessId = requireBusinessId();
        return customerRepository.findByBusinessIdAndArchivedTrue(businessId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public void archiveCustomer(Long id) {
        Long businessId = requireBusinessId();
        Customer customer = requireOwnedCustomer(id, businessId);
        customer.setArchived(true);
        customer.setArchivedAt(java.time.LocalDateTime.now());
        customerRepository.save(customer);
    }

    @Override
    public void restoreCustomer(Long id) {
        Long businessId = requireBusinessId();
        // Since we explicitly want to find archived ones, requireOwnedCustomer works as it uses findById
        Customer customer = requireOwnedCustomer(id, businessId);
        customer.setArchived(false);
        customer.setArchivedAt(null);
        customerRepository.save(customer);
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
        return customerRepository.findByBusinessIdAndArchivedFalse(businessId)
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
        dto.setArchived(customer.getArchived());
        return dto;
    }
}
