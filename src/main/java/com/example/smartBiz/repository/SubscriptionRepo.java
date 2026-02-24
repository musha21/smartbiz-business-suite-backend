package com.example.smartBiz.repository;

import com.example.smartBiz.entity.Subscription;
import com.example.smartBiz.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepo extends JpaRepository<Subscription, Long> {

    /** Find the active subscription for a business */
    Optional<Subscription> findByBusinessIdAndStatus(Long businessId, SubscriptionStatus status);

    /** Find all subscriptions for a business (history) */
    List<Subscription> findByBusinessIdOrderByCreatedAtDesc(Long businessId);
}
