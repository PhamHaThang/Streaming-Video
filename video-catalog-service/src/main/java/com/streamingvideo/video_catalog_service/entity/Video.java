package com.streamingvideo.video_catalog_service.entity;

import com.streamingvideo.common.dto.BaseEntity;
import com.streamingvideo.video_catalog_service.enums.VideoStatus;
import com.streamingvideo.video_catalog_service.enums.Visibility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "videos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video extends BaseEntity {
    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ── Info upload user ──
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // ── Category ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    private List<String> tags;

    // ── Info file ──
    @Column(name = "original_file_name", length = 500)
    private String originalFileName;

    @Column(name = "original_file_url", length = 1000)
    private String originalFileUrl;

    @Column(name = "hls_url", length = 1000)
    private String hlsUrl;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "file_size")
    private Long fileSize;

    // ── Info video ──
    @Column(name = "duration")
    private Integer duration;  // second

    @Column(name = "resolution", length = 20)
    private String resolution;

    // ── Analytics ──
    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "like_count")
    @Builder.Default
    private Long likeCount = 0L;

    // ── Status ──
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VideoStatus status = VideoStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;
}
