package com.example.smartBiz.controller;

import com.example.smartBiz.dto.AuthResponseDto;
import com.example.smartBiz.dto.AuthTokenWrapperDto;
import com.example.smartBiz.dto.LoginRequestDto;
import com.example.smartBiz.dto.RegisterRequestDto;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/auth")
@CrossOrigin
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto req) {
        AuthTokenWrapperDto wrapper = authService.register(req);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(wrapper.getRefreshToken(), 7 * 24 * 60 * 60).toString())
                .body(wrapper.getResponseDto());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto req) {
        AuthTokenWrapperDto wrapper = authService.login(req);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(wrapper.getRefreshToken(), 7 * 24 * 60 * 60).toString())
                .body(wrapper.getResponseDto());
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponseDto> getMe() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        return ResponseEntity.ok(authService.getMe(principal.getUserId()));
    }

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
