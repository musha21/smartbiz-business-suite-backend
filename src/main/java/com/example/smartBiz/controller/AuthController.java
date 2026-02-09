package com.example.smartBiz.controller;

import com.example.smartBiz.dto.*;
import com.example.smartBiz.entity.AppUser;
import com.example.smartBiz.entity.Business;
import com.example.smartBiz.enums.Role;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.BusinessRepo;
import com.example.smartBiz.repository.UserRepo;
import com.example.smartBiz.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/auth")
@CrossOrigin
public class AuthController {

    private final UserRepo userRepo;
    private final BusinessRepo businessRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepo userRepo, BusinessRepo businessRepo, PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.businessRepo = businessRepo;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@RequestBody RegisterRequestDto req) {

        if (userRepo.existsByEmail(req.getEmail())) {
            throw new ResourceNotFoundException("EMAIL_ALREADY_EXISTS");
        }

        if (req.getBusinessName() == null || req.getBusinessName().isBlank()) {
            throw new ResourceNotFoundException("BUSINESS_NAME_REQUIRED");
        }

        // ✅ 1) create business properly
        Business business = new Business();
        business.setName(req.getBusinessName());
        business.setActive(true);
        Business savedBusiness = businessRepo.save(business);

        // ✅ 2) create user and link business
        AppUser user = new AppUser();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setRole(Role.OWNER);
        user.setBusiness(savedBusiness);

        AppUser savedUser = userRepo.save(user);

        String token = jwtUtil.generateToken(
                savedUser.getId(),
                savedBusiness.getId(),
                savedUser.getRole().name()
        );

        return ResponseEntity.ok(new AuthResponseDto(
                token,
                savedUser.getId(),
                savedBusiness.getId(),
                savedUser.getRole().name()
        ));
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto req) {

        AppUser user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!encoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        Long businessId = (user.getBusiness() != null) ? user.getBusiness().getId() : null;

        String token = jwtUtil.generateToken(
                user.getId(),
                businessId,
                user.getRole().name()
        );

        return ResponseEntity.ok(new AuthResponseDto(
                token,
                user.getId(),
                businessId,
                user.getRole().name()
        ));
    }
}
