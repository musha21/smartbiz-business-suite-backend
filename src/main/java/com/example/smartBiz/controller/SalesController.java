package com.example.smartBiz.controller;

import com.example.smartBiz.dto.SaleDto;
import com.example.smartBiz.service.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/sales")
@CrossOrigin
public class SalesController {

    private final SaleService saleService;

    @Autowired
    public SalesController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    public ResponseEntity<String> createSale(@RequestBody SaleDto saleDto) {
        saleService.createSale(saleDto);
        return ResponseEntity.ok("Sale created successfully");
    }
}
