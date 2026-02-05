package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.ProductsDto;
import com.example.smartBiz.entity.Products;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.ProductRepo;
import com.example.smartBiz.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
  private final ProductRepo productRepo;

    @Autowired
    public ProductServiceImpl(ProductRepo productRepo) {
        this.productRepo = productRepo;
    }


    @Override
    public ProductsDto createProduct(ProductsDto productDto) {
      // Convert DTO to Entity
      Products product = new Products(
              productDto.getName(),
              productDto.getPrice(),
              productDto.getStockQty(),
              productDto.getLowStockLimit(),
              productDto.getBusinessId()
      );

      // Save to database
      Products savedProduct = productRepo.save(product);

      // Convert Entity back to DTO to return
      return new ProductsDto(
              savedProduct.getId(),
              savedProduct.getName(),
              savedProduct.getPrice(),
              savedProduct.getStock_qty(),
              savedProduct.getLow_stock_limit(),
              savedProduct.getBusiness_id()
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

      // Save updated product
      Products updatedProduct = productRepo.save(existingProduct);

      // Return DTO
      return new ProductsDto(
              updatedProduct.getId(),
              updatedProduct.getName(),
              updatedProduct.getPrice(),
              updatedProduct.getStock_qty(),
              updatedProduct.getLow_stock_limit(),
              updatedProduct.getBusiness_id()
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
              product.getBusiness_id()
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
                      product.getBusiness_id()
              ))
              .toList();
    }
}