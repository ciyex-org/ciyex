package org.ciyex.ehr.messaging.repository;

import org.ciyex.ehr.messaging.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {

    List<MessageReaction> findByMessageId(UUID messageId);

    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(UUID messageId, String userId, String emoji);

    void deleteByMessageIdAndUserIdAndEmoji(UUID messageId, String userId, String emoji);
}
