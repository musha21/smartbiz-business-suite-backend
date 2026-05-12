package com.example.smartBiz.security;

import com.example.smartBiz.dto.ErrorResponse;
import com.example.smartBiz.entity.Business;
import com.example.smartBiz.repository.BusinessRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final BusinessRepo businessRepo;
    private final ObjectMapper objectMapper;

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/v1/api/auth/login",
            "/v1/api/auth/register",
            "/v1/api/auth/refresh",
            "/v1/api/auth/logout",
            "/v1/api/payments/notify",
            "/v1/api/payments/status",
            "/v1/api/plans/active",
            "/v1/api/public/testimonials",
            "/swagger-ui",
            "/v3/api-docs"
    );

    public JwtFilter(JwtUtil jwtUtil,
                     BusinessRepo businessRepo,
                     ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.businessRepo = businessRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // ✅ 1. Skip public endpoints safely
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        // ❌ Missing token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, 401, "Missing Authorization token", request);
            return;
        }

        String token = authHeader.substring(7);

        try {

            Claims claims = jwtUtil.getAllClaims(token);

            // ✅ SAFE extraction (null-safe)
            Long userId = getLong(claims, "userId");
            Long businessId = getLong(claims, "businessId");

            // ❌ invalid token payload
            if (userId == null) {
                writeError(response, 401, "Invalid token payload", request);
                return;
            }

            // ✅ Roles
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            Object role = claims.get("role");
            if (role instanceof String r) {
                addRole(authorities, r);
            }

            Object roles = claims.get("roles");
            if (roles instanceof Collection<?> list) {
                for (Object r : list) {
                    if (r instanceof String s) addRole(authorities, s);
                }
            }

            boolean isAdmin = authorities.stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            // ✅ Business validation (safe)
            if (!isAdmin) {

                if (businessId == null) {
                    writeError(response, 403, "Business context missing", request);
                    return;
                }

                Optional<Business> businessOpt = businessRepo.findById(businessId);

                if (businessOpt.isEmpty()) {
                    writeError(response, 403, "Business not found", request);
                    return;
                }

                Business business = businessOpt.get();

                if (Boolean.FALSE.equals(business.getActive())) {
                    writeError(response, 403, "Business disabled", request);
                    return;
                }
            }

            // ✅ Authentication set
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, businessId);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            authorities
                    );

            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            writeError(response, 401, "Invalid or expired token", request);
            return;
        }

        filterChain.doFilter(request, response);
    }

    // ✅ PUBLIC PATH CHECK (SAFE)
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    // ✅ SAFE LONG PARSER
    private Long getLong(Claims claims, String key) {
        Object value = claims.get(key);
        if (value instanceof Number n) return n.longValue();
        return null;
    }

    // ✅ ROLE HANDLER
    private void addRole(List<SimpleGrantedAuthority> list, String role) {
        if (role == null || role.isBlank()) return;

        String formatted = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        SimpleGrantedAuthority auth = new SimpleGrantedAuthority(formatted);

        if (!list.contains(auth)) {
            list.add(auth);
        }
    }

    // ✅ ERROR RESPONSE
    private void writeError(HttpServletResponse response,
                            int status,
                            String message,
                            HttpServletRequest request) throws IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(status);

        ErrorResponse error = ErrorResponse.builder()
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}