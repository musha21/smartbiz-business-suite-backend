package com.example.smartBiz.repository;

import com.example.smartBiz.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceItemRepo extends JpaRepository<InvoiceItem, Long> {

}
