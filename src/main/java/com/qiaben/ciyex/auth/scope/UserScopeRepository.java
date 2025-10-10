package com.qiaben.ciyex.auth.scope;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserScopeRepository extends JpaRepository<UserScope, Long> {

    @Query("""
           select us.scope.code
             from UserScope us
            where us.userId = :userId
              and us.active = true
              and us.scope.active = true
           """)
    List<String> findActiveScopeCodesByUserId(Long userId);

    boolean existsByUserIdAndScope_Id(Long userId, Long scopeId);
}
