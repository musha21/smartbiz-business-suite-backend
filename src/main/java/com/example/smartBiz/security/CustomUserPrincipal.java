package com.example.smartBiz.security;

import org.springframework.security.core.context.SecurityContextHolder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomUserPrincipal {
    private final Long userId;
    private final String username;
    private final Long businessId;

    public static CustomUserPrincipal getCurrent() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserPrincipal p) {
            return p;
        }
        return null;
    }
}
