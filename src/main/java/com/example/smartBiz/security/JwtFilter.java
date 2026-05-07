package com.example.smartBiz.security;

import com.example.smartBiz.dto.ErrorResponse;
import com.example.smartBiz.entity.Business;
import com.example.smartBiz.exception.ResourceNotFoundException;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final BusinessRepo businessRepo;

    public JwtFilter(JwtUtil jwtUtil, BusinessRepo businessRepo) {
        this.jwtUtil = jwtUtil;
        this.businessRepo = businessRepo;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // ✅ 1. Skip public endpoints
        if (path.startsWith("/v1/api/auth/")
                || path.startsWith("/v1/api/payments/notify")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader("Authorization");

        // ✅ 2. Reject if token missing (for protected routes)
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authorization header missing", request);
            return;
        }

        String token = auth.substring(7);

        try {
            // ✅ 3. Parse token
            Claims claims = jwtUtil.getAllClaims(token);

            Long userId = claims.get("userId", Number.class).longValue();

            Long businessId = claims.get("businessId", Number.class) != null
                    ? claims.get("businessId", Number.class).longValue()
                    : null;

            // ✅ 4. Extract roles
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            Object roleClaim = claims.get("role");
            if (roleClaim instanceof String r) {
                addAuthority(authorities, r);
            }

            Object rolesClaim = claims.get("roles");
            if (rolesClaim instanceof Collection<?>) {
                ((Collection<?>) rolesClaim).forEach(r -> {
                    if (r instanceof String) {
                        addAuthority(authorities, (String) r);
                    }
                });
            }

            // ✅ 5. Business validation (non-admin only)
            boolean isAdmin = authorities.stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                if (businessId == null) {
                    writeError(response, HttpServletResponse.SC_FORBIDDEN, "Business context missing", request);
                    return;
                }

                Business business = businessRepo.findById(businessId)
                        .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

                if (Boolean.FALSE.equals(business.getActive())) {
                    writeError(response, HttpServletResponse.SC_FORBIDDEN, "Business disabled", request);
                    return;
                }
            }

            // ✅ 6. Set authentication
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, businessId);

            var authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // ❌ Invalid / expired token
            SecurityContextHolder.clearContext();

            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token", request);
            return;
        }

        // ✅ Continue filter chain
        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String message,
                            HttpServletRequest request) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(status);
        ErrorResponse error = ErrorResponse.builder()
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        new ObjectMapper().writeValue(response.getOutputStream(), error);
    }

    // ✅ Helper method
    private void addAuthority(List<SimpleGrantedAuthority> list, String role) {
        if (role == null || role.isBlank()) return;

        String finalRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(finalRole);

        if (!list.contains(authority)) {
            list.add(authority);
        }
    }
}