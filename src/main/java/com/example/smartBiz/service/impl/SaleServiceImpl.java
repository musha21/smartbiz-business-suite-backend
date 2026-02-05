package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.SaleDto;
import com.example.smartBiz.entity.Customer;
import com.example.smartBiz.entity.Products;
import com.example.smartBiz.entity.Sale;
import com.example.smartBiz.repository.CustomerRepo;
import com.example.smartBiz.repository.ProductRepo;
import com.example.smartBiz.repository.SaleRepo;
import com.example.smartBiz.service.SaleService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SaleServiceImpl implements SaleService {
    private final SaleRepo saleRepository;
    private final CustomerRepo customerRepository;
    private final ProductRepo productRepo;

    public SaleServiceImpl(SaleRepo saleRepository, CustomerRepo customerRepository, ProductRepo productRepo) {
        this.saleRepository = saleRepository;
        this.customerRepository = customerRepository;
        this.productRepo = productRepo;
    }


    @Override
    public void createSale(SaleDto saleDto) {

        Customer customer = customerRepository.findById(saleDto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Products product = productRepo.findById(saleDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getStock_qty() < saleDto.getQuantity()) {
            throw new RuntimeException("Insufficient stock");
        }

        // 🔻 Reduce stock
        product.setStock_qty(product.getStock_qty() - saleDto.getQuantity());
        productRepo.save(product);

        // 💰 Create Sale
        Sale sale = new Sale();
        sale.setCustomer(customer);
        sale.setProduct(product);
        sale.setQuantity(saleDto.getQuantity());
        sale.setTotalAmount(product.getPrice() * saleDto.getQuantity());
        sale.setSaleDate(LocalDateTime.now());

        saleRepository.save(sale);
    }
    }

