package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.ProductsDto;
import com.example.smartBiz.entity.Products;
import com.example.smartBiz.entity.Supplier;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.ProductRepo;
import com.example.smartBiz.repository.SupplierRepo;
import com.example.smartBiz.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
  private final ProductRepo productRepo;
  private final SupplierRepo supplierRepo;

    @Autowired
    public ProductServiceImpl(ProductRepo productRepo, SupplierRepo supplierRepo) {
        this.productRepo = productRepo;
        this.supplierRepo = supplierRepo;
    }


    @Override
    public ProductsDto createProduct(ProductsDto productDto) {
      Products product = new Products(
              productDto.getName(),
              productDto.getPrice(),
              productDto.getStockQty(),
              productDto.getLowStockLimit(),
              productDto.getBusinessId()
      );

      // ✅ SET SUPPLIER (very important)
      if (productDto.getSupplierId() != null) {
        Supplier supplier = supplierRepo.findById(productDto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supplier not found with id " + productDto.getSupplierId()
                ));
        product.setSupplier(supplier);
      }

      Products savedProduct = productRepo.save(product);

      return new ProductsDto(
              savedProduct.getId(),
              savedProduct.getName(),
              savedProduct.getPrice(),
              savedProduct.getStock_qty(),
              savedProduct.getLow_stock_limit(),
              savedProduct.getBusiness_id(),
              savedProduct.getSupplier() != null ? savedProduct.getSupplier().getId() : null
      );
    }

    @Override
    public ProductsDto updateProduct(Long id, ProductsDto productDto) {
      // Find product by id
      Products existingProduct = productRepo.findById(id)
              .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));

      // Update fields
      existingProduct.setName(productDto.getName());
      existingProduct.setPrice(productDto.getPrice());
      existingProduct.setStock_qty(productDto.getStockQty());
      existingProduct.setLow_stock_limit(productDto.getLowStockLimit());
      existingProduct.setBusiness_id(productDto.getBusinessId());
      if (productDto.getSupplierId() != null) {
        Supplier supplier = supplierRepo.findById(productDto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supplier not found with id " + productDto.getSupplierId()
                ));
        existingProduct.setSupplier(supplier);
      }
      // Save updated product
      Products updatedProduct = productRepo.save(existingProduct);

      // Return DTO
      return new ProductsDto(
              updatedProduct.getId(),
              updatedProduct.getName(),
              updatedProduct.getPrice(),
              updatedProduct.getStock_qty(),
              updatedProduct.getLow_stock_limit(),
              updatedProduct.getBusiness_id(),
              updatedProduct.getSupplier() != null ? updatedProduct.getSupplier().getId() : null

      );
    }

    @Override
    public void deleteProduct(Long id) {
      Products product = productRepo.findById(id)
              .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
      productRepo.delete(product);

    }

    @Override
    public ProductsDto getProductById(Long id) {
      Products product = productRepo.findById(id)
              .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
      return new ProductsDto(
              product.getId(),
              product.getName(),
              product.getPrice(),
              product.getStock_qty(),
              product.getLow_stock_limit(),
              product.getBusiness_id(),
              product.getSupplier() != null ? product.getSupplier().getId() : null
      );
    }

    @Override
    public List<ProductsDto> getAllProducts() {
      return productRepo.findAll().stream()
              .map(product -> new ProductsDto(
                      product.getId(),
                      product.getName(),
                      product.getPrice(),
                      product.getStock_qty(),
                      product.getLow_stock_limit(),
                      product.getBusiness_id(),
                      product.getSupplier() != null ? product.getSupplier().getId() : null
              ))
              .toList();
    }
}