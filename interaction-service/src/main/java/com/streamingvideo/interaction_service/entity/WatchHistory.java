package com.streamingvideo.interaction_service.entity;

import com.streamingvideo.common.dto.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "watch_history",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "video_id"}))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WatchHistory extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Column(name = "watch_duration")
    @Builder.Default
    private Integer watchDuration = 0;

    @Column(name = "video_duration")
    private Integer videoDuration;

    @Column(name = "watch_percentage", precision = 5, scale = 2)
    private BigDecimal watchPercentage;

    @Column(name = "last_position")
    @Builder.Default
    private Integer lastPosition = 0;

    @Column(name = "watch_count")
    @Builder.Default
    private Integer watchCount = 1;
}
