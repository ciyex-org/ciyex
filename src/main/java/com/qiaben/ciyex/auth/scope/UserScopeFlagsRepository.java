package com.qiaben.ciyex.auth.scope;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserScopeFlagsRepository extends JpaRepository<UserScopeFlags, Long> {
    Optional<UserScopeFlags> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
