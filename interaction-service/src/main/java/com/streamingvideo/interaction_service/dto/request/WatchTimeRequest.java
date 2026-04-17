package com.streamingvideo.interaction_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

// Update watch time
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WatchTimeRequest {
    @NotNull(message = "Video ID không được để trống")
    private UUID videoId;

    @Min(value = 0, message = "Thời gian xem phải >= 0")
    private Integer watchDuration; // Tong thoi gian xem (s)

    @Min(value = 0, message = "Tổng thời lượng video phải >= 0")
    private Integer videoDuration; // Tong thoi luong video (s)

    @Min(value = 0, message = " Vị trí hiện tại  >= 0")
    private Integer lastPosition; // Vi tri hien tai (s)
}
