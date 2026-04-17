package com.streamingvideo.interaction_service.dto.response;

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
public class WatchHistoryResponse {
    private UUID id;
    private UUID videoId;
    private Integer lastPosition;
    private Double watchPercentage;
    private LocalDateTime updatedAt;
}
