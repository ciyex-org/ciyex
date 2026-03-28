package org.ciyex.ehr.kiosk.repository;

import org.ciyex.ehr.kiosk.entity.KioskConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KioskConfigRepository extends JpaRepository<KioskConfig, Long> {
    Optional<KioskConfig> findByOrgAlias(String orgAlias);
}
