package org.ciyex.ehr.messaging.repository;

import org.ciyex.ehr.messaging.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByChannelIdAndDeletedFalseOrderByCreatedAtAsc(UUID channelId);

    @Query("""
        SELECT m FROM Message m
        WHERE m.channelId = :channelId
          AND m.deleted = false
          AND m.parentId IS NULL
        ORDER BY m.createdAt ASC
        """)
    List<Message> findTopLevelMessages(@Param("channelId") UUID channelId);

    @Query("""
        SELECT m FROM Message m
        WHERE m.channelId = :channelId
          AND m.deleted = false
        ORDER BY m.createdAt DESC
        """)
    List<Message> findRecentMessages(@Param("channelId") UUID channelId, Pageable pageable);

    List<Message> findByParentIdAndDeletedFalseOrderByCreatedAtAsc(UUID parentId);

    long countByParentIdAndDeletedFalse(UUID parentId);

    @Query(value = """
        SELECT m FROM Message m
        WHERE m.channelId = :channelId
          AND m.pinned = true
          AND m.deleted = false
        ORDER BY m.createdAt DESC
        """)
    List<Message> findPinnedMessages(@Param("channelId") UUID channelId);

    /** Count messages in a channel created after a given instant */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.channelId = :channelId AND m.deleted = false AND m.createdAt > :since")
    long countMessagesAfter(@Param("channelId") UUID channelId, @Param("since") java.time.Instant since);

    /** Count all non-deleted messages in a channel */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.channelId = :channelId AND m.deleted = false")
    long countMessages(@Param("channelId") UUID channelId);

    @Query(value = """
        SELECT m.* FROM message m
        WHERE m.org_alias = :orgAlias
          AND m.is_deleted = false
          AND to_tsvector('english', m.content) @@ plainto_tsquery('english', :query)
        ORDER BY m.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Message> searchMessages(@Param("orgAlias") String orgAlias,
                                 @Param("query") String query,
                                 @Param("limit") int limit);

    @Query(value = """
        SELECT m.* FROM message m
        WHERE m.channel_id = :channelId
          AND m.org_alias = :orgAlias
          AND m.is_deleted = false
          AND to_tsvector('english', m.content) @@ plainto_tsquery('english', :query)
        ORDER BY m.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Message> searchMessagesInChannel(@Param("orgAlias") String orgAlias,
                                          @Param("channelId") UUID channelId,
                                          @Param("query") String query,
                                          @Param("limit") int limit);
}
