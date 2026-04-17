package com.streamingvideo.interaction_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InteractionRequest {
    @NotNull(message = "Video ID không được để trống")
    private UUID videoId;

    @NotBlank(message = "Loại tương tác không được để trống")
    private String interactionType; // VIEW, LIKE, UNLIKE, SHARE, COMMENT...

    private Map<String, Object> metadata;  // Dữ liệu bổ sung
}
