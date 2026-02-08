package com.example.smartBiz.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestContext {

    private final JwtUtil jwtService;
    private final HttpServletRequest request;

    public RequestContext(JwtUtil jwtService, HttpServletRequest request) {
        this.jwtService = jwtService;
        this.request = request;
    }

    public Long getBusinessId() {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        return jwtService.getBusinessId(auth.substring(7));
    }


}
