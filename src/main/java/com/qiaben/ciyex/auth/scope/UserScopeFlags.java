package com.qiaben.ciyex.auth.scope;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "auth_user_scope_flags")   // ⬅ change table name
@Getter @Setter
public class UserScopeFlags {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Packed flags string: one slot per master scope (ordered by auth_scope.id asc)
    // Slot value: "1" = granted, "" (empty) = not granted
    @Lob
    @Column(name = "flags", nullable = false)
    private String flags;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
