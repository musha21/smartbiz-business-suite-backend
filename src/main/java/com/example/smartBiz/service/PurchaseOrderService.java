package com.example.smartBiz.service;

import com.example.smartBiz.dto.PurchaseOrderCreateRequest;
import com.example.smartBiz.dto.PurchaseOrderResponse;
import com.example.smartBiz.dto.ReceiveGoodsRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PurchaseOrderService {

    PurchaseOrderResponse createPurchaseOrder(PurchaseOrderCreateRequest request);

    PurchaseOrderResponse sendPurchaseOrder(Long poId);

    PurchaseOrderResponse receiveGoods(Long poId, ReceiveGoodsRequest request);

    PurchaseOrderResponse cancelPurchaseOrder(Long poId);

    PurchaseOrderResponse getPurchaseOrderById(Long id);

    Page<PurchaseOrderResponse> getAllPurchaseOrders(Pageable pageable);

    List<PurchaseOrderResponse> getPurchaseOrdersByStatus(String status);

    List<PurchaseOrderResponse> getOverduePurchaseOrders();

    String generatePONumber();
}
