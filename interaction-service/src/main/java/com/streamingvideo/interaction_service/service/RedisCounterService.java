package com.streamingvideo.interaction_service.service;

import com.streamingvideo.interaction_service.enums.InteractionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service manage real-time counters in Redis
 * <p>
 * Redis keys:
 * - video:views:{videoId}     → Total views
 * - video:likes:{videoId}     → Total likes
 * - video:shares:{videoId}    → Total shares
 * -> Realtime, fast (O(1))
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCounterService {
    private final RedisTemplate<String, String> redisTemplate;

    public void incrementCounter(UUID videoId, InteractionType type) {
        String key = getRedisKey(videoId, type);
        if (key != null) {
            redisTemplate.opsForValue().increment(key, 1);
        }
    }

    public void decrementCounter(UUID videoId, InteractionType type) {
        String key = getRedisKey(videoId, type);
        if (key != null) {
            redisTemplate.opsForValue().decrement(key, 1);
        }
    }

    public Long getCounter(UUID videoId, InteractionType type) {
        String key = getRedisKey(videoId, type);
        if (key == null) return 0L;
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }

    private String getRedisKey(UUID videoId, InteractionType type) {
        return switch (type) {
            case LIKE -> "video:likes:" + videoId;
            case VIEW -> "video:views:" + videoId;
            case SHARE -> "video:shares:" + videoId;
            default -> null;
        };
    }
}
