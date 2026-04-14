package com.streamingvideo.user_service.service;

import com.streamingvideo.user_service.dto.request.LoginRequest;
import com.streamingvideo.user_service.dto.request.RefreshTokenRequest;
import com.streamingvideo.user_service.dto.request.RegisterRequest;
import com.streamingvideo.user_service.dto.response.AuthResponse;

public interface AuthService {
    public AuthResponse register(RegisterRequest request);

    public AuthResponse login(LoginRequest request);

    public AuthResponse refreshToken(RefreshTokenRequest request);
}
