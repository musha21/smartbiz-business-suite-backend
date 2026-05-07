package com.example.smartBiz.service.impl;

import com.example.smartBiz.dto.AuthResponseDto;
import com.example.smartBiz.dto.AuthTokenWrapperDto;
import com.example.smartBiz.dto.LoginRequestDto;
import com.example.smartBiz.dto.RegisterRequestDto;
import com.example.smartBiz.entity.AppUser;
import com.example.smartBiz.entity.Business;
import com.example.smartBiz.entity.Plan;
import com.example.smartBiz.entity.Subscription;
import com.example.smartBiz.enums.BillingCycle;
import com.example.smartBiz.enums.Role;
import com.example.smartBiz.enums.SubscriptionStatus;
import com.example.smartBiz.exception.ResourceNotFoundException;
import com.example.smartBiz.repository.BusinessRepo;
import com.example.smartBiz.repository.PlanRepo;
import com.example.smartBiz.repository.SubscriptionRepo;
import com.example.smartBiz.repository.UserRepo;
import com.example.smartBiz.security.JwtUtil;
import com.example.smartBiz.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepo userRepo;
    private final BusinessRepo businessRepo;
    private final PlanRepo planRepo;
    private final SubscriptionRepo subscriptionRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserRepo userRepo, BusinessRepo businessRepo,
                           PlanRepo planRepo, SubscriptionRepo subscriptionRepo,
                           PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.businessRepo = businessRepo;
        this.planRepo = planRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
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

        // 3) Auto-assign FREE plan
        assignFreePlan(savedBusiness);

        // 4) Generate token
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

    /**
     * Auto-assigns the FREE plan (7-day trial) to a newly registered business.
     * After 7 days the subscription expires and the user must upgrade.
     * If no FREE plan exists in the DB, registration still succeeds but the
     * business will have no subscription until an admin assigns one.
     */
    private static final int FREE_TRIAL_DAYS = 7;

    private void assignFreePlan(Business business) {
        planRepo.findByCode("FREE").ifPresentOrElse(
                freePlan -> {
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime trialEnd = now.plusDays(FREE_TRIAL_DAYS);

                    // Create ACTIVE subscription — 7-day free trial
                    Subscription sub = new Subscription();
                    sub.setBusiness(business);
                    sub.setPlan(freePlan);
                    sub.setBillingCycle(BillingCycle.MONTHLY);
                    sub.setStatus(SubscriptionStatus.ACTIVE);
                    sub.setStartAt(now);
                    sub.setEndAt(trialEnd); // expires after 7 days
                    subscriptionRepo.save(sub);

                    // Sync business entity
                    business.setPlanId(freePlan.getId());
                    business.setPlan(freePlan.getName());
                    business.setSubscriptionStart(now);
                    business.setSubscriptionEnd(trialEnd);
                    businessRepo.save(business);

                    log.info("Auto-assigned FREE trial ({} days) to business {} (ID: {}), expires: {}",
                            FREE_TRIAL_DAYS, business.getName(), business.getId(), trialEnd);
                },
                () -> log.warn("FREE plan not found in DB — business {} registered without a plan. "
                        + "Create a plan with code='FREE' via admin panel.", business.getId())
        );
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
