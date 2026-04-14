package com.streamingvideo.user_service.repository;

import com.streamingvideo.user_service.entity.RefreshToken;
import com.streamingvideo.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);

    /**
     * Delete all refresh token of user.
     * When change password, logout all devices
     */
    @Modifying
    void deleteByUser(User user);
}
