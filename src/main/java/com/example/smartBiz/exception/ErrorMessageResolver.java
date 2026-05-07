package com.example.smartBiz.exception;

import java.util.Map;

/**
 * Maps raw error keys to human-friendly messages.
 * Falls back to the raw message if no mapping exists.
 */
public class ErrorMessageResolver {

    private static final Map<String, String> MESSAGES = Map.ofEntries(
            Map.entry("EMAIL_REQUIRED", "Email is required"),
            Map.entry("PASSWORD_REQUIRED", "Password is required"),
            Map.entry("NAME_REQUIRED", "Name is required"),
            Map.entry("BUSINESS_NAME_REQUIRED", "Business name is required"),
            Map.entry("EMAIL_ALREADY_EXISTS", "This email address is already registered"),
            Map.entry("INVALID_CREDENTIALS", "Invalid email or password"),
            Map.entry("BUSINESS_MISSING", "Business account not found"),
            Map.entry("BUSINESS_DISABLED", "Your business account has been disabled. Contact support."),
            Map.entry("REFRESH_TOKEN_MISSING", "Refresh token is missing"),
            Map.entry("INVALID_REFRESH_TOKEN", "Your session has expired. Please log in again."),
            Map.entry("USER_NOT_FOUND", "User not found"),
            Map.entry("PLAN_NOT_FOUND", "Plan not found"),
            Map.entry("SUBSCRIPTION_NOT_FOUND", "Subscription not found")
    );

    public static String resolve(String rawMessage) {
        if (rawMessage == null) return "Something went wrong";
        return MESSAGES.getOrDefault(rawMessage.trim().toUpperCase(), rawMessage);
    }
}
