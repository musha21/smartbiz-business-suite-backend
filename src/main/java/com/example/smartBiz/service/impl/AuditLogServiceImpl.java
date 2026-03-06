package com.example.smartBiz.service.impl;

import com.example.smartBiz.entity.AdminLog;
import com.example.smartBiz.repository.AdminLogRepository;
import com.example.smartBiz.service.AuditLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AdminLogRepository adminLogRepository;

    public AuditLogServiceImpl(AdminLogRepository adminLogRepository) {
        this.adminLogRepository = adminLogRepository;
    }

    @Override
    public void log(String level, String message) {
        AdminLog log = new AdminLog();
        log.setLevel(level);
        log.setMessage(message);
        log.setCreatedAt(LocalDateTime.now());
        adminLogRepository.save(log);
    }

    @Override
    public void info(String message) {
        log("INFO", message);
    }

    @Override
    public void warn(String message) {
        log("WARN", message);
    }

    @Override
    public void error(String message) {
        log("ERROR", message);
    }
}
