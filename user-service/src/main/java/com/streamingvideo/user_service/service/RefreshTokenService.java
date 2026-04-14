package com.streamingvideo.user_service.service;

import com.streamingvideo.user_service.entity.RefreshToken;
import com.streamingvideo.user_service.entity.User;

public interface RefreshTokenService {
    public RefreshToken createRefreshToken(User user);

    public RefreshToken findByToken(String token);

    public void deleteByUser(User user);
}
