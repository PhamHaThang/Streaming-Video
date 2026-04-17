package com.streamingvideo.interaction_service.dto.response;

import com.streamingvideo.interaction_service.enums.InteractionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InteractionResponse {
    private UUID id;
    private UUID userId;
    private UUID videoId;
    private String interactionType;
    private LocalDateTime createdAt;
}
