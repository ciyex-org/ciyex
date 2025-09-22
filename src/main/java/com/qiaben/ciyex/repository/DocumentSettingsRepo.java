package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.DocumentSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentSettingsRepo extends JpaRepository<DocumentSettings, Integer> {
    Optional<DocumentSettings> findByOrgId(Long orgId);
}

