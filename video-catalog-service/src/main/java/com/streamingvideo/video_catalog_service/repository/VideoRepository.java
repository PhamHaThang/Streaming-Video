package com.streamingvideo.video_catalog_service.repository;

import com.streamingvideo.video_catalog_service.entity.Video;
import com.streamingvideo.video_catalog_service.enums.VideoStatus;
import com.streamingvideo.video_catalog_service.enums.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {
    /**
     * GET video PUBLIC, READY, PAGINATION
     */
    Page<Video> findByVisibilityAndStatusOrderByCreatedAtDesc(
            Visibility visibility, VideoStatus status,  Pageable pageable
    );

    /**
     * GET video of user
     */
    Page<Video> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * GET video PUBLIC by category
     */
    Page<Video> findByCategoryIdAndVisibilityAndStatusOrderByCreatedAtDesc(
            UUID categoryId, Visibility visibility, VideoStatus status, Pageable pageable);

    /**
     * GET video by title (full-text search)
     */
    @Query("SELECT v FROM Video v " +
            "WHERE v.visibility = 'PUBLIC' AND v.status = 'READY' " +
            "AND LOWER(v.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY v.createdAt DESC")
    Page<Video> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    /**
     * GET video trending (top view_count)
     */
    @Query("SELECT v FROM Video v " +
            "WHERE v.visibility = 'PUBLIC' AND v.status = 'READY' " +
            "ORDER BY v.viewCount DESC")
    Page<Video> findTrending(Pageable pageable);

    /**
     * GET video by list ids (for recommendation)
     */
    List<Video> findByIdInAndVisibilityAndStatus(
            List<UUID> ids, Visibility visibility, VideoStatus status);

    /**
     * Increase video view
     */
    @Modifying
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1 WHERE v.id = :videoId")
    void incrementViewCount(@Param("videoId") UUID videoId);

    /**
     * Increase video like
     */
    @Modifying
    @Query("UPDATE Video v SET v.likeCount = v.likeCount + 1 WHERE v.id = :videoId")
    void incrementLikeCount(@Param("videoId") UUID videoId);

    /**
     * Decrease video view
     */
    @Modifying
    @Query("UPDATE Video v SET v.likeCount = v.likeCount - 1 WHERE v.id = :videoId AND v.likeCount > 0")
    void decrementLikeCount(@Param("videoId") UUID videoId);
}
