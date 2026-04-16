package com.streamingvideo.transcoding_service.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamingvideo.common.constant.KafkaTopics;
import com.streamingvideo.common.dto.event.TranscodeCompletedEvent;
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
     * Publish transcoding result to Kafka
     * Video Catalog Service will consume this event to update metadata
     */
    public void publishTranscodeCompleted(TranscodeCompletedEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopics.TRANSCODE_COMPLETED,
                    event.getVideoId().toString(),
                    message
            ).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish transcode-completed: videoId={}",
                            event.getVideoId(), ex);
                } else {
                    log.info("Published transcode-completed: videoId={}, success={}",
                            event.getVideoId(), event.isSuccess());
                }
            });
        } catch (Exception e) {
            log.error("Failed to serialize TranscodeCompletedEvent", e);
        }
    }
}
