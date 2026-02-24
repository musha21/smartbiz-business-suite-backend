package com.example.smartBiz.dto;

import com.example.smartBiz.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpiringSubscriptionDTO {
    private String businessName;
    private String planName;
    private LocalDateTime endAt;
    private SubscriptionStatus status;
}
