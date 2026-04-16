package com.streamingvideo.interaction_service.entity;

import com.streamingvideo.common.dto.BaseEntity;
import com.streamingvideo.interaction_service.enums.InteractionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "interactions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Interaction extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "video_id", nullable = false)
    private UUID videoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false, length = 20)
    private InteractionType interactionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Objects> metadata;
}
