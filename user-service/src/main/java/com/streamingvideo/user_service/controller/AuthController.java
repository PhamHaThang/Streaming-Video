package com.streamingvideo.user_service.controller;

import com.streamingvideo.common.dto.response.ApiResponse;
import com.streamingvideo.user_service.dto.request.LoginRequest;
import com.streamingvideo.user_service.dto.request.RefreshTokenRequest;
import com.streamingvideo.user_service.dto.request.RegisterRequest;
import com.streamingvideo.user_service.dto.response.AuthResponse;
import com.streamingvideo.user_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * [POST] /api/auth/register
     *
     * @param request RegisterRequest (username, email, password, displayName)
     * @return AuthResponse (access token, refresh token, user info,...)
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse authResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Đăng ký thành công", authResponse));
    }

    /**
     * [POST] /api/auth/login
     *
     * @param request LoginRequest (username/email, password)
     * @return AuthResponse
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(
                ApiResponse.success("Đăng nhập thành công", authResponse)
        );
    }

    /**
     * [POST] /api/auth/refresh-token
     *
     * @param request RefreshToken
     * @return AuthResponse
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        AuthResponse authResponse = authService.refreshToken(request);
        return ResponseEntity.ok(
                ApiResponse.success("Refresh Token thành công", authResponse)
        );
    }
}
