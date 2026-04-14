package com.streamingvideo.user_service.service;

import com.streamingvideo.user_service.dto.request.ChangePasswordRequest;
import com.streamingvideo.user_service.dto.request.UpdateProfileRequest;
import com.streamingvideo.user_service.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {
    public UserResponse getUserById(UUID userId);

    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request);

    public void changePassword(UUID userId, ChangePasswordRequest request);


}
