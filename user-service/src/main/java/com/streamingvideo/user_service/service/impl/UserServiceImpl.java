package com.streamingvideo.user_service.service.impl;

import com.streamingvideo.common.exception.AppException;
import com.streamingvideo.common.exception.ErrorCode;
import com.streamingvideo.user_service.dto.request.ChangePasswordRequest;
import com.streamingvideo.user_service.dto.request.UpdateProfileRequest;
import com.streamingvideo.user_service.dto.response.UserResponse;
import com.streamingvideo.user_service.entity.User;
import com.streamingvideo.user_service.mapper.UserMapper;
import com.streamingvideo.user_service.repository.UserRepository;
import com.streamingvideo.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return UserMapper.toUserResponse(user);

    }

    @Transactional
    @Override
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        UserMapper.updateUser(user, request);
        user = userRepository.save(user);
        log.info("Cập nhật profile thành công: userId={}", userId);
        return UserMapper.toUserResponse(user);
    }

    @Transactional
    @Override
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Đổi mật khẩu thành công: userId={}", userId);
    }
}
