package com.example.smartBiz.service;

public interface AuditLogService {
    void log(String level, String message);

    void info(String message);

    void warn(String message);

    void error(String message);
}
