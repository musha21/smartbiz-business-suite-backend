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

        String auth = request.getHeader("Authorization");

        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);

            try {
                // ✅ Parse & validate token
                Claims claims = jwtUtil.getAllClaims(token);

                Long userId = claims.get("userId", Number.class).longValue();
                Long businessId = claims.get("businessId", Number.class) != null
                        ? claims.get("businessId", Number.class).longValue()
                        : null;

                // ✅ 1) Extract Authorities (Dynamically support 'role' and 'roles' claims)
                java.util.List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();

                // Handle claim "role": "ADMIN"
                Object roleClaim = claims.get("role");
                if (roleClaim instanceof String r) {
                    addAuthority(authorities, r);
                }

                // Handle claim "roles": ["ADMIN", "OWNER"]
                Object rolesClaim = claims.get("roles");
                if (rolesClaim instanceof java.util.Collection<?>) {
                    ((java.util.Collection<?>) rolesClaim).forEach(r -> {
                        if (r instanceof String)
                            addAuthority(authorities, (String) r);
                    });
                }

                // ✅ 2) Ownership check (non-admin only)
                boolean isAdmin = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

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

                // ✅ 3) DEBUG LOG: Verify extracted authorities
                System.out.println("[DEBUG JWT] User: " + userId + " | Authorities: " + authorities);

                // ✅ 4) Set Authentication
                CustomUserPrincipal principal = new CustomUserPrincipal(userId, businessId);
                var authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                // ❌ Invalid token
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private void addAuthority(java.util.List<SimpleGrantedAuthority> list, String role) {
        if (role == null || role.isBlank())
            return;
        // Map "ADMIN" -> "ROLE_ADMIN" while keeping "ROLE_ADMIN" as is
        String finalRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        SimpleGrantedAuthority auth = new SimpleGrantedAuthority(finalRole);
        if (!list.contains(auth)) {
            list.add(auth);
        }
    }
}
