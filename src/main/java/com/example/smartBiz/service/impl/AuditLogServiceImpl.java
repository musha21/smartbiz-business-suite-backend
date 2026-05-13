package com.example.smartBiz.service.impl;

import com.example.smartBiz.entity.AuditLog;
import com.example.smartBiz.repository.AuditLogRepo;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepo auditLogRepo;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Ensure log is saved even if main transaction fails
    public void log(String action, String resource, Long resourceId, String details) {
        try {
            CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
            
            AuditLog.AuditLogBuilder builder = AuditLog.builder()
                    .action(action)
                    .resource(resource)
                    .resourceId(resourceId)
                    .details(details);

            if (principal != null) {
                builder.userId(principal.getUserId())
                       .username(principal.getUsername())
                       .businessId(principal.getBusinessId());
            } else {
                builder.userId(0L)
                       .username("SYSTEM")
                       .details(details != null ? details + " (No active user context)" : "No active user context");
            }

            auditLogRepo.save(builder.build());
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }
}
