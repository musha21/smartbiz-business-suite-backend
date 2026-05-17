package com.example.smartBiz.service;

import com.example.smartBiz.dto.ReorderAlertResponse;

import java.util.List;

public interface ReorderAlertService {

    List<ReorderAlertResponse> getActiveAlerts();

    List<ReorderAlertResponse> getAllAlerts();

    ReorderAlertResponse getAlertById(Long id);

    void dismissAlert(Long id);

    com.example.smartBiz.dto.PurchaseOrderResponse createPurchaseOrderFromAlert(Long alertId, Long supplierId, Boolean allowUnlinkedSupplier);

    void checkAndGenerateAlerts();

    Long getActiveAlertCount();
}
