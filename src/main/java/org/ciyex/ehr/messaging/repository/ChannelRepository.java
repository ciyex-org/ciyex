package org.ciyex.ehr.messaging.repository;

import org.ciyex.ehr.messaging.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {

    List<Channel> findByOrgAliasAndArchivedFalseOrderByNameAsc(String orgAlias);

    @Query("""
        SELECT c FROM Channel c
        WHERE c.orgAlias = :orgAlias
          AND c.archived = false
          AND (c.type IN ('public')
               OR c.id IN (SELECT cm.channelId FROM ChannelMember cm WHERE cm.userId IN :userIds))
        ORDER BY c.name
        """)
    List<Channel> findAccessibleChannels(@Param("orgAlias") String orgAlias, @Param("userIds") List<String> userIds);

    Optional<Channel> findByOrgAliasAndNameAndArchivedFalse(String orgAlias, String name);

    @Query("""
        SELECT c FROM Channel c
        WHERE c.orgAlias = :orgAlias
          AND c.type = 'dm'
          AND c.archived = false
          AND c.id IN (SELECT cm1.channelId FROM ChannelMember cm1 WHERE cm1.userId = :user1)
          AND c.id IN (SELECT cm2.channelId FROM ChannelMember cm2 WHERE cm2.userId = :user2)
        """)
    Optional<Channel> findDmBetweenUsers(@Param("orgAlias") String orgAlias,
                                          @Param("user1") String user1,
                                          @Param("user2") String user2);
}
