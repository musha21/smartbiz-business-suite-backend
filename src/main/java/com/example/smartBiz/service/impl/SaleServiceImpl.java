package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.SaleDto;
import com.example.smartBiz.entity.Customer;
import com.example.smartBiz.entity.Products;
import com.example.smartBiz.entity.Sale;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.CustomerRepo;
import com.example.smartBiz.repository.ProductRepo;
import com.example.smartBiz.repository.SaleRepo;
import com.example.smartBiz.security.RequestContext;
import com.example.smartBiz.service.SaleService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SaleServiceImpl implements SaleService {
    private final SaleRepo saleRepository;
    private final CustomerRepo customerRepository;
    private final ProductRepo productRepo;
    private final RequestContext requestContext;

    public SaleServiceImpl(SaleRepo saleRepository, CustomerRepo customerRepository, ProductRepo productRepo, RequestContext requestContext) {
        this.saleRepository = saleRepository;
        this.customerRepository = customerRepository;
        this.productRepo = productRepo;
        this.requestContext = requestContext;
    }


    @Override
    public void createSale(SaleDto saleDto) {

        Long businessId = requestContext.getBusinessId();
        if (businessId == null) {
            throw new RuntimeException("Business context missing (JWT required)");
        }

        Customer customer = customerRepository.findById(saleDto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // ✅ customer must belong to this business
        if (!customer.getBusinessId().equals(businessId)) {
            throw new RuntimeException("Access denied: customer not in your business");
        }

        Products product = productRepo.findById(saleDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // ✅ product must belong to this business (your field name is business_id)
        if (!product.getBusinessId().equals(businessId)) {
            throw new ResourceNotFoundException("Access denied: product not in your business");
        }

        Integer currentStock = product.getStock_qty();
        if (currentStock == null) currentStock = 0;

        int qty = saleDto.getQuantity();
        if (currentStock < qty) {
            throw new RuntimeException("Insufficient stock");
        }

        // 🔻 Reduce stock safely
        product.setStock_qty(currentStock - qty);
        productRepo.save(product);

        // 💰 Create Sale
        Sale sale = new Sale();
        sale.setBusinessId(businessId); // ✅ store businessId in sale
        sale.setCustomer(customer);
        sale.setProduct(product);
        sale.setQuantity(qty);
        sale.setTotalAmount(product.getPrice() * qty);
        sale.setSaleDate(LocalDateTime.now());

        saleRepository.save(sale);
    }

}

