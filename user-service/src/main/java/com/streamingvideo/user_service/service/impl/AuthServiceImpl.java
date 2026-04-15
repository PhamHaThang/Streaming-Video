package com.streamingvideo.user_service.service.impl;

import com.streamingvideo.common.exception.AppException;
import com.streamingvideo.common.exception.ErrorCode;
import com.streamingvideo.user_service.dto.request.LoginRequest;
import com.streamingvideo.user_service.dto.request.RefreshTokenRequest;
import com.streamingvideo.user_service.dto.request.RegisterRequest;
import com.streamingvideo.user_service.dto.response.AuthResponse;
import com.streamingvideo.user_service.entity.RefreshToken;
import com.streamingvideo.user_service.entity.User;
import com.streamingvideo.user_service.mapper.UserMapper;
import com.streamingvideo.user_service.repository.UserRepository;
import com.streamingvideo.user_service.service.AuthService;
import com.streamingvideo.user_service.service.JwtTokenProvider;
import com.streamingvideo.user_service.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register new account
     * Flow:
     * 1. Check exists email/username?
     * 2. Encode password by BCrypt
     * 3. Create user entity and save
     * 4. Create Jwt tokens
     * 5. Return AuthResponse
     */
    @Transactional
    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("Đăng ký tài khoản mới: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        User user = UserMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user = userRepository.save(user);
        log.info("Tạo tài khoản thành công: userId={}", user.getId());
        return buildAuthResponse(user);
    }

    /**
     * Login
     * Flow:
     * 1. Find user by email or password
     * 2. Check password
     * 3. Check isActive account
     * 4. Create Jwt tokens
     * 5. Return AuthResponse
     */
    @Transactional
    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Đăng nhập: {}", request.getUsernameOrEmail());
        User user = userRepository.findByEmailOrUsername(
                        request.getUsernameOrEmail(),
                        request.getUsernameOrEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }

        if (!user.getIsActive()) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        log.info("Đăng nhập thành công: userId={}", user.getId());
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService
                .findByToken(request.getRefreshToken());

        if (refreshToken.isExpired()) {
            refreshTokenService.deleteByUser(refreshToken.getUser());
            throw new AppException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        User user = refreshToken.getUser();
        log.info("Refresh token thành công: userId={}", user.getId());

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationInSeconds())
                .user(UserMapper.toUserResponse(user))
                .build();
    }
}
