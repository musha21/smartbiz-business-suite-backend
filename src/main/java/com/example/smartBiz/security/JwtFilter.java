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
            FilterChain filterChain
    ) throws ServletException, IOException {

        String auth = request.getHeader("Authorization");

        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);

            try {
                // ✅ Parse & validate token
                Claims claims = jwtUtil.getAllClaims(token);

                String role = claims.get("role", String.class);
                Long userId = claims.get("userId", Number.class).longValue();
                Long businessId = claims.get("businessId", Number.class) != null
                        ? claims.get("businessId", Number.class).longValue()
                        : null;

                // ✅ BLOCK disabled business (non-admin only)
                if (!"ADMIN".equals(role)) {
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

                // ✅ Create authority ROLE_ADMIN / ROLE_OWNER
                var authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );

                var authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        authorities
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                // ❌ Invalid token
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
