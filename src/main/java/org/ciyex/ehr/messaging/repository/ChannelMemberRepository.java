package org.ciyex.ehr.messaging.repository;

import org.ciyex.ehr.messaging.entity.ChannelMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelMemberRepository extends JpaRepository<ChannelMember, UUID> {

    List<ChannelMember> findByChannelIdOrderByDisplayNameAsc(UUID channelId);

    Optional<ChannelMember> findByChannelIdAndUserId(UUID channelId, String userId);

    boolean existsByChannelIdAndUserId(UUID channelId, String userId);

    void deleteByChannelIdAndUserId(UUID channelId, String userId);

    long countByChannelId(UUID channelId);
}
