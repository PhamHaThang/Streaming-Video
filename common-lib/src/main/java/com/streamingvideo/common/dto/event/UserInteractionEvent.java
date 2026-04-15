package com.streamingvideo.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Event DTO dùng chung: Tương tác người dùng.
 *
 * Producer: Interaction Service
 * Consumer: Recommendation Service
 * Topic: "user-interactions"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInteractionEvent {
    private UUID userId;
    private UUID videoId;
    private String interactionType;
    private double weight;
    private Map<String, Object> metadata;
    private String timestamp;
}
