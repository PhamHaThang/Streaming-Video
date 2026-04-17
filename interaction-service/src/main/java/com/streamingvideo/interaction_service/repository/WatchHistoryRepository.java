package com.streamingvideo.interaction_service.repository;

import com.streamingvideo.interaction_service.entity.WatchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, UUID> {
    Optional<WatchHistory> findByUserIdAndVideoId(UUID userId, UUID videoId);

    Page<WatchHistory> findByUserIdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);
    @Query("SELECT SUM(w.watchDuration) FROM WatchHistory w WHERE w.userId = :userId")
    Long sumWatchTimeByUserId(@Param("userId") UUID userId);

}
