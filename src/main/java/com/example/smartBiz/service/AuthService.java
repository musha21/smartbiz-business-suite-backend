package com.example.smartBiz.service;

import com.example.smartBiz.dto.AuthResponseDto;
import com.example.smartBiz.dto.LoginRequestDto;
import com.example.smartBiz.dto.RegisterRequestDto;

import com.example.smartBiz.dto.AuthTokenWrapperDto;

public interface AuthService {
    AuthTokenWrapperDto register(RegisterRequestDto dto);

    AuthTokenWrapperDto login(LoginRequestDto dto);

    AuthResponseDto getMe(Long userId);

    AuthTokenWrapperDto refreshToken(String refreshToken);
}
