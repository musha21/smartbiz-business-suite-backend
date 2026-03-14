package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.AuthResponseDto;
import com.example.smartBiz.dto.AuthTokenWrapperDto;
import com.example.smartBiz.dto.LoginRequestDto;
import com.example.smartBiz.dto.RegisterRequestDto;
import com.example.smartBiz.entity.AppUser;
import com.example.smartBiz.entity.Business;
import com.example.smartBiz.enums.Role;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.BusinessRepo;
import com.example.smartBiz.repository.UserRepo;
import com.example.smartBiz.security.JwtUtil;
import com.example.smartBiz.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepo userRepo;
    private final BusinessRepo businessRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserRepo userRepo, BusinessRepo businessRepo, PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.businessRepo = businessRepo;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public AuthTokenWrapperDto register(RegisterRequestDto req) {

        if (req.getName() == null || req.getName().isBlank()) {
            throw new ResourceNotFoundException("NAME_REQUIRED");
        }

        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new ResourceNotFoundException("EMAIL_REQUIRED");
        }

        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new ResourceNotFoundException("PASSWORD_REQUIRED");
        }

        if (userRepo.existsByEmail(req.getEmail())) {
            throw new ResourceNotFoundException("EMAIL_ALREADY_EXISTS");
        }

        if (req.getBusinessName() == null || req.getBusinessName().isBlank()) {
            throw new ResourceNotFoundException("BUSINESS_NAME_REQUIRED");
        }

        // 1) Create business
        Business business = new Business();
        business.setName(req.getBusinessName().trim());
        business.setActive(true);
        Business savedBusiness = businessRepo.save(business);

        // 2) Create owner user and link business
        AppUser user = new AppUser();
        user.setName(req.getName().trim());
        user.setEmail(req.getEmail().trim().toLowerCase());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setRole(Role.OWNER);
        user.setBusiness(savedBusiness);

        AppUser savedUser = userRepo.save(user);

        // 3) Generate token
        String token = jwtUtil.generateToken(
                savedUser.getId(),
                savedBusiness.getId(),
                savedUser.getRole().name());

        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getId());

        AuthResponseDto responseDto = new AuthResponseDto(
                token,
                savedUser.getId(),
                savedBusiness.getId(),
                savedUser.getRole().name(),
                savedUser.getName(),
                savedBusiness.getName());

        return new AuthTokenWrapperDto(responseDto, refreshToken);
    }

    @Override
    public AuthTokenWrapperDto login(LoginRequestDto req) {

        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new ResourceNotFoundException("EMAIL_REQUIRED");
        }

        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new ResourceNotFoundException("PASSWORD_REQUIRED");
        }

        AppUser user = userRepo.findByEmail(req.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("INVALID_CREDENTIALS"));

        if (!encoder.matches(req.getPassword(), user.getPassword())) {
            throw new ResourceNotFoundException("INVALID_CREDENTIALS");
        }

        // Block disabled business users (OWNER / STAFF)
        if (user.getRole() != Role.ADMIN) {

            if (user.getBusiness() == null) {
                throw new ResourceNotFoundException("BUSINESS_MISSING");
            }

            if (Boolean.FALSE.equals(user.getBusiness().getActive())) {
                throw new ResourceNotFoundException("BUSINESS_DISABLED");
            }
        }

        Long businessId = (user.getBusiness() != null) ? user.getBusiness().getId() : null;

        String token = jwtUtil.generateToken(
                user.getId(),
                businessId,
                user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        AuthResponseDto responseDto = new AuthResponseDto(
                token,
                user.getId(),
                businessId,
                user.getRole().name(),
                user.getName(),
                user.getBusiness() != null ? user.getBusiness().getName() : null);

        return new AuthTokenWrapperDto(responseDto, refreshToken);
    }

    @Override
    public AuthResponseDto getMe(Long userId) {
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND"));

        Long businessId = (user.getBusiness() != null) ? user.getBusiness().getId() : null;
        String businessName = (user.getBusiness() != null) ? user.getBusiness().getName() : null;

        return new AuthResponseDto(
                null, // no new token needed for /me
                user.getId(),
                businessId,
                user.getRole().name(),
                user.getName(),
                businessName);
    }

    @Override
    public AuthTokenWrapperDto refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResourceNotFoundException("REFRESH_TOKEN_MISSING");
        }

        try {
            // Validate the refresh token, this will throw JwtException if expired/invalid
            jwtUtil.getAllClaims(refreshToken);
            
            Long userId = jwtUtil.getUserId(refreshToken);
            AppUser user = userRepo.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND"));

            // Block disabled users
            if (user.getRole() != Role.ADMIN) {
                if (user.getBusiness() == null || Boolean.FALSE.equals(user.getBusiness().getActive())) {
                    throw new ResourceNotFoundException("BUSINESS_DISABLED");
                }
            }

            Long businessId = (user.getBusiness() != null) ? user.getBusiness().getId() : null;

            // Generate new access token
            String accessToken = jwtUtil.generateToken(user.getId(), businessId, user.getRole().name());
            
            // Optionally rotate refresh token
            String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

            AuthResponseDto responseDto = new AuthResponseDto(
                    accessToken,
                    user.getId(),
                    businessId,
                    user.getRole().name(),
                    user.getName(),
                    user.getBusiness() != null ? user.getBusiness().getName() : null);

            return new AuthTokenWrapperDto(responseDto, newRefreshToken);

        } catch (Exception e) {
            throw new ResourceNotFoundException("INVALID_REFRESH_TOKEN");
        }
    }
}
