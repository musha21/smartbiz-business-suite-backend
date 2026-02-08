package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.*;
import com.example.smartBiz.entity.*;
import com.example.smartBiz.repository.*;

import com.example.smartBiz.security.JwtUtil;
import com.example.smartBiz.service.AuthService;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepo userRepo;
    private final BusinessRepo businessRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtService;
    private final AuthenticationManager authManager;

    public AuthServiceImpl(UserRepo userRepo, BusinessRepo businessRepo, PasswordEncoder encoder, JwtUtil jwtService, AuthenticationManager authManager) {
        this.userRepo = userRepo;
        this.businessRepo = businessRepo;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.authManager = authManager;
    }


    @Override
    public AuthResponseDto register(RegisterRequestDto dto) {

        if (userRepo.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        Business business = new Business();
        business.setName(dto.getBusinessName());
        Business savedBusiness = businessRepo.save(business);

        AppUser user = new AppUser();
        user.setEmail(dto.getEmail());
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setRole("OWNER");
        user.setBusiness(savedBusiness);

        AppUser savedUser = userRepo.save(user);

        String token = jwtService.generateToken(
                savedUser.getEmail(),
                savedUser.getId(),
                savedBusiness.getId(),
                savedUser.getRole()
        );

        return new AuthResponseDto(token, savedUser.getId(), savedBusiness.getId(), savedUser.getRole());
    }

    @Override
    public AuthResponseDto login(LoginRequestDto dto) {

        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );

        AppUser user = userRepo.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getId(),
                user.getBusiness().getId(),
                user.getRole()
        );

        return new AuthResponseDto(token, user.getId(), user.getBusiness().getId(), user.getRole());
    }
}
