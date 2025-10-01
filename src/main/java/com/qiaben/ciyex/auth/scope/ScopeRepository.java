package com.qiaben.ciyex.auth.scope;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScopeRepository extends JpaRepository<Scope, Long> {
    Optional<Scope> findByCode(String code);
    boolean existsByCode(String code);
    // NEW helpers (stable ordering by ID)
    List<Scope> findAllByOrderByIdAsc();
    List<Scope> findAllByActiveTrueOrderByIdAsc();
}
