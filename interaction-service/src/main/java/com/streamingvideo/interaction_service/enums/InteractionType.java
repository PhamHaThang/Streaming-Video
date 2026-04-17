package com.streamingvideo.interaction_service.enums;

import lombok.Getter;

/**
 * Enum for interaction
 * Each interaction has a weight used for recommendations.
 */
@Getter
public enum InteractionType {
    VIEW(1.0),
    LIKE(3.0),
    UNLIKE(-3.0),
    COMPLETE(5.0),      // View all video (>90%)
    SHARE(4.0),
    COMMENT(3.5),
    WATCH_TIME(0.0),    // Dynamic weight based on watch_percentage
    SEARCH(0.5);
    private final double weight;

    InteractionType(double weight) {
        this.weight = weight;
    }

}
