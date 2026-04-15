package com.streamingvideo.video_catalog_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamingvideo.common.constant.KafkaTopics;
import com.streamingvideo.common.dto.event.VideoUploadedEvent;
import com.streamingvideo.common.exception.AppException;
import com.streamingvideo.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service publish event to Kafka
 * Topics:
 * - video-uploaded: When video upload successfully
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishVideoUploadedEvent(VideoUploadedEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopics.VIDEO_UPLOADED,
                    event.getVideoId().toString(),
                    message
            ).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish video-uploaded event: videoId={}",
                            event.getVideoId(), ex);
                } else {
                    log.info("Published video-uploaded event: videoId={}, partition={}, offset={}",
                            event.getVideoId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            }
        );
    } catch (JsonProcessingException e) {
            log.error("Failed to serialize VideoUploadedEvent", e);
            throw new AppException(ErrorCode.SERIALIZATION_ERROR);
        }
    }
}
