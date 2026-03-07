package com.example.smartBiz.controller;

import com.example.smartBiz.dto.AuthResponseDto;
import com.example.smartBiz.dto.LoginRequestDto;
import com.example.smartBiz.dto.RegisterRequestDto;
import com.example.smartBiz.security.CustomUserPrincipal;
import com.example.smartBiz.service.AuthService;
import jakarta.validation.Valid;
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
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponseDto> getMe() {
        CustomUserPrincipal principal = CustomUserPrincipal.getCurrent();
        return ResponseEntity.ok(authService.getMe(principal.getUserId()));
    }
}
