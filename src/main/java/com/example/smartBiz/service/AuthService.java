package com.example.smartBiz.service;

import com.example.smartBiz.dto.AuthResponseDto;
import com.example.smartBiz.dto.LoginRequestDto;
import com.example.smartBiz.dto.RegisterRequestDto;

public interface AuthService {
    AuthResponseDto register(RegisterRequestDto dto);

    AuthResponseDto login(LoginRequestDto dto);

    AuthResponseDto getMe(Long userId);
}
