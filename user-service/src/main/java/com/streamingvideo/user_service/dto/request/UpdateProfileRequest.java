package com.streamingvideo.user_service.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    @Size(max = 100, message = "Tên hiển thị tối đa 100 ký tự")
    private String displayName;

    @Size(max = 500, message = "URL avatar tối đa 500 ký tự")
    private String avatarUrl;

    @Size(max = 1000, message = "Bio tối đa 1000 ký tự")
    private String bio;
}
