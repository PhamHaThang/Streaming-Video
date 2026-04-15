package com.streamingvideo.user_service.controller;

import com.streamingvideo.common.constant.AppHeaders;
import com.streamingvideo.common.dto.response.ApiResponse;
import com.streamingvideo.user_service.dto.request.ChangePasswordRequest;
import com.streamingvideo.user_service.dto.request.UpdateProfileRequest;
import com.streamingvideo.user_service.dto.response.UserResponse;
import com.streamingvideo.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * [GET] /api/users/me
     * Get info of current user
     * userId is injected into the X-User-Id header by the gateway after JWT
     * authentication.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @RequestHeader(AppHeaders.X_USER_ID) String userId) {
        UserResponse userResponse = userService.getUserById(UUID.fromString(userId));
        return ResponseEntity.ok(
                ApiResponse.success("Lấy profile user thành công", userResponse));
    }

    /**
     * [GET] /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable UUID id) {
        UserResponse userResponse = userService.getUserById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Lấy user với id: " + id + "  thành công", userResponse));
    }

    /**
     * [PUT] /api/users/me
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @RequestHeader(AppHeaders.X_USER_ID) String userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse userResponse = userService.updateProfile(UUID.fromString(userId), request);
        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật Profile thành công", userResponse));
    }

    /**
     * [PUT] /api/users/me/password
     */
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<UserResponse>> changePassword(
            @RequestHeader(AppHeaders.X_USER_ID) String userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(UUID.fromString(userId), request);
        return ResponseEntity.ok(
                ApiResponse.success("Đổi mật khẩu thành công", null));
    }
}
