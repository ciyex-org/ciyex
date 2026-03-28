package org.ciyex.ehr.settings.repository;

import org.ciyex.ehr.settings.entity.PracticeLogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PracticeLogoRepository extends JpaRepository<PracticeLogo, Long> {
    Optional<PracticeLogo> findByOrgId(String orgId);
    void deleteByOrgId(String orgId);
}
