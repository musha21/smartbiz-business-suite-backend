package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.*;
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
    public AuthResponseDto register(RegisterRequestDto req) {

        if (userRepo.existsByEmail(req.getEmail())) {
            throw new ResourceNotFoundException("EMAIL_ALREADY_EXISTS");
        }

        if (req.getBusinessName() == null || req.getBusinessName().isBlank()) {
            throw new ResourceNotFoundException("Business name is required");
        }

        // ✅ 1) Create Business properly
        Business business = new Business();
        business.setName(req.getBusinessName());
        business.setActive(true);
        Business savedBusiness = businessRepo.save(business);
        if (req.getName() == null || req.getName().isBlank()) {
            throw new ResourceNotFoundException("NAME_REQUIRED");
        }
        // ✅ 2) Create OWNER user linked to business
        AppUser user = new AppUser();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setRole(Role.OWNER);
        user.setBusiness(savedBusiness);

        AppUser savedUser = userRepo.save(user);

        // ✅ 3) Token contains: userId, businessId, role
        String token = jwtUtil.generateToken(
                savedUser.getId(),
                savedBusiness.getId(),
                savedUser.getRole().name()
        );

        return new AuthResponseDto(
                token,
                savedUser.getId(),
                savedBusiness.getId(),
                savedUser.getRole().name()
        );
    }

    @Override
    public AuthResponseDto login(LoginRequestDto req) {

        AppUser user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password"));

        if (!encoder.matches(req.getPassword(), user.getPassword())) {
            throw new ResourceNotFoundException("Invalid email or password");
        }

        // ✅ Block disabled business users (OWNER / STAFF)
        if (user.getRole() != Role.ADMIN) {

            if (user.getBusiness() == null) {
                throw new ResourceNotFoundException("Business missing for this user");
            }

            if (Boolean.FALSE.equals(user.getBusiness().getActive())) {
                throw new ResourceNotFoundException("Business is disabled. Contact admin.");
            }
        }

        Long businessId = (user.getBusiness() != null) ? user.getBusiness().getId() : null;

        String token = jwtUtil.generateToken(
                user.getId(),
                businessId,
                user.getRole().name()
        );

        return new AuthResponseDto(
                token,
                user.getId(),
                businessId,
                user.getRole().name()
        );
    }

}
