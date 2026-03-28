package org.ciyex.ehr.inventory.repository;

import org.ciyex.ehr.inventory.entity.InvSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvSettingsRepository extends JpaRepository<InvSettings, Long> {

    Optional<InvSettings> findByOrgAlias(String orgAlias);
}
