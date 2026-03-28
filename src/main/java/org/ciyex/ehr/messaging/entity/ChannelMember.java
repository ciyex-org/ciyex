package org.ciyex.ehr.messaging.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "channel_member", uniqueConstraints = @UniqueConstraint(columnNames = {"channel_id", "user_id"}))
public class ChannelMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "member";

    @Column(name = "last_read_at")
    private Instant lastReadAt;

    @Column(name = "is_muted", nullable = false)
    @Builder.Default
    private boolean muted = false;

    @Column(name = "org_alias", nullable = false, length = 100)
    private String orgAlias;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @PrePersist
    void prePersist() {
        joinedAt = Instant.now();
    }
}
