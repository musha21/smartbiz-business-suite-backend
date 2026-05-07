package com.example.smartBiz.repository;

import com.example.smartBiz.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepo extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderId(String orderId);

    boolean existsByOrderIdAndStatus(String orderId, String status);

    List<PaymentOrder> findByBusinessIdOrderByCreatedAtDesc(Long businessId);
}

