package com.streamingvideo.interaction_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamingvideo.common.constant.KafkaTopics;
import com.streamingvideo.common.dto.event.UserInteractionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publist interaction event to Kafka
     * Recommendation Service consume events from this topic
     * Key = userId
     * (Ensure that the same user's events are correctly ordered)
     */
    public void publishInteractionEvent(UserInteractionEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(
                    KafkaTopics.USER_INTERACTIONS,
                    event.getUserId().toString(),
                    message
            ).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish interaction event: userId={}, videoId={}",
                            event.getUserId(), event.getVideoId(), ex);
                } else {
                    log.debug("Published interaction event: type={}, userId={}, videoId={}",
                            event.getInteractionType(),
                            event.getUserId(),
                            event.getVideoId());
                }
            });

        } catch (Exception e) {
            log.error("Failed to serialize UserInteractionEvent", e);
        }
    }
}
