package com.example.smartBiz.security;

import com.example.smartBiz.entity.Business;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.BusinessRepo;
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
        if (path.startsWith("/v1/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader("Authorization");

        // ✅ 2. Reject if token missing (for protected routes)
        if (auth == null || !auth.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Authorization header missing");
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
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("Business context missing");
                    return;
                }

                Business business = businessRepo.findById(businessId)
                        .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

                if (Boolean.FALSE.equals(business.getActive())) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("Business disabled");
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

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or expired token");
            return;
        }

        // ✅ Continue filter chain
        filterChain.doFilter(request, response);
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