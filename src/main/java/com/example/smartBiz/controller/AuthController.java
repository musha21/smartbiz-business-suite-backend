package com.example.smartBiz.controller;

import com.example.smartBiz.dto.AuthResponseDto;
import com.example.smartBiz.dto.AuthTokenWrapperDto;
import com.example.smartBiz.dto.LoginRequestDto;
import com.example.smartBiz.dto.RegisterRequestDto;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/auth")
@CrossOrigin
@Tag(name = "Auth", description = "Registration, login, token refresh & logout")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a new business owner", description = "Creates a new user with OWNER role and a new business. Returns JWT access token; refresh token is set as an HttpOnly cookie.", security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Validation error or email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto req) {
        AuthTokenWrapperDto wrapper = authService.register(req);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(wrapper.getRefreshToken(), 7 * 24 * 60 * 60).toString())
                .body(wrapper.getResponseDto());
    }

    @Operation(summary = "Login with email & password", description = "Returns JWT access token in body and refresh token as HttpOnly cookie. Rate limited: 5 attempts/min per IP.", security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "429", description = "Too many login attempts")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto req) {
        AuthTokenWrapperDto wrapper = authService.login(req);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(wrapper.getRefreshToken(), 7 * 24 * 60 * 60).toString())
                .body(wrapper.getResponseDto());
    }

    @Operation(summary = "Get current user profile", description = "Returns the authenticated user's profile details based on the JWT token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/me")
    public ResponseEntity<AuthResponseDto> getMe() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        return ResponseEntity.ok(authService.getMe(principal.getUserId()));
    }

    @Operation(summary = "Refresh access token", description = "Uses the HttpOnly refresh_token cookie to issue a new access token and rotate the refresh token.", security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed"),
            @ApiResponse(responseCode = "401", description = "Refresh token missing or expired")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refreshToken(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        AuthTokenWrapperDto wrapper = authService.refreshToken(refreshToken);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(wrapper.getRefreshToken(), 7 * 24 * 60 * 60).toString())
                .body(wrapper.getResponseDto());
    }

    @Operation(summary = "Logout", description = "Clears the refresh_token cookie.")
    @ApiResponse(responseCode = "200", description = "Logged out successfully")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie("", 0).toString())
                .build();
    }

    private ResponseCookie createRefreshTokenCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(false) // Set to true in production if using HTTPS
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax") // or "Strict"
                .build();
    }
}
