package com.streamingvideo.interaction_service.mapper;

import com.streamingvideo.common.exception.AppException;
import com.streamingvideo.common.exception.ErrorCode;
import com.streamingvideo.interaction_service.dto.request.InteractionRequest;
import com.streamingvideo.interaction_service.dto.response.InteractionResponse;
import com.streamingvideo.interaction_service.entity.Interaction;
import com.streamingvideo.interaction_service.enums.InteractionType;

import java.util.Locale;

public class InteractionMapper {
    public static InteractionResponse toInteractionResponse(Interaction interaction) {
        return InteractionResponse.builder()
                .id(interaction.getId())
                .userId(interaction.getUserId())
                .videoId(interaction.getVideoId())
                .interactionType(interaction.getInteractionType().name())
                .createdAt(interaction.getCreatedAt())
                .build();
    }

    public static Interaction toInteraction(InteractionRequest request) {
        InteractionType type;
        try {
            type = InteractionType.valueOf(request.getInteractionType().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.INVALID_INTERACTION_TYPE,
                    "interactionType=" + request.getInteractionType());
        }

        return Interaction.builder()
                .videoId(request.getVideoId())
                .interactionType(type)
                .metadata(request.getMetadata())
                .build();
    }
}
