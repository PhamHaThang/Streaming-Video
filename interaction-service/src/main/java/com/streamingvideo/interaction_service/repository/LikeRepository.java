package com.streamingvideo.interaction_service.repository;

import com.streamingvideo.interaction_service.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {
    Optional<Like> findByUserIdAndVideoId(UUID userId, UUID videoId);

    boolean existsByUserIdAndVideoId(UUID userId, UUID videoId);

    void deleteByUserIdAndVideoId(UUID userId, UUID videoId);

    long countByUserId(UUID userId);
}
