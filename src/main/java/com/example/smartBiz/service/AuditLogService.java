package com.example.smartBiz.service;

public interface AuditLogService {
    void log(String action, String resource, Long resourceId, String details);

    default void info(String details) {
        log("INFO", "SYSTEM", null, details);
    }

    default void warn(String details) {
        log("WARN", "SYSTEM", null, details);
    }

    default void error(String details) {
        log("ERROR", "SYSTEM", null, details);
    }

    default void info(String resource, String details) {
        log("INFO", resource, null, details);
    }

    default void warn(String resource, String details) {
        log("WARN", resource, null, details);
    }

    default void error(String resource, String details) {
        log("ERROR", resource, null, details);
    }
}
