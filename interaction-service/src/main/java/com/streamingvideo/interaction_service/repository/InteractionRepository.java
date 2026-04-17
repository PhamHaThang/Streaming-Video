package com.streamingvideo.interaction_service.repository;

import com.streamingvideo.interaction_service.entity.Interaction;
import com.streamingvideo.interaction_service.enums.InteractionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, UUID> {
    List<Interaction> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Interaction> findByVideoIdAndInteractionType(UUID videoId, InteractionType type);
    long countByVideoIdAndInteractionType(UUID videoId, InteractionType type);
    long countByUserIdAndInteractionType(UUID userId, InteractionType type);

    /**
     * Get all interactions of user for recommendation
     */
    @Query("SELECT i FROM Interaction i WHERE i.userId = :userId " +
            "AND i.interactionType IN :types " +
            "ORDER BY i.createdAt DESC "
    )
    List<Interaction> findUserPositiveInteractions(
            @Param("userId") UUID userId,
            @Param("types") List<InteractionType> types);
}
