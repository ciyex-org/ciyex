package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.InventorySettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventorySettingsRepository extends JpaRepository<InventorySettings, Long> {
    Optional<InventorySettings> findFirstByOrderByIdAsc();
}
