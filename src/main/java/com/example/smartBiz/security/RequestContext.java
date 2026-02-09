package com.example.smartBiz.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestContext {

    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;

    public RequestContext(JwtUtil jwtUtil, HttpServletRequest request) {
        this.jwtUtil = jwtUtil;
        this.request = request;
    }

    public Long getBusinessId() {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        return jwtUtil.getBusinessId(auth.substring(7));
    }

    public Long getUserId() {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        return jwtUtil.getUserId(auth.substring(7));
    }

    public String getRole() {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        return jwtUtil.getRole(auth.substring(7));
    }
}
