package org.ciyex.ehr.tabconfig.repository;

import org.ciyex.ehr.tabconfig.entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpecialtyRepository extends JpaRepository<Specialty, UUID> {
    @Query("SELECT s FROM Specialty s WHERE s.active = true AND (s.orgId = :orgId OR s.orgId = '*') ORDER BY s.name")
    List<Specialty> findAllForOrg(String orgId);

    Optional<Specialty> findByCodeAndOrgId(String code, String orgId);

    @Query("SELECT s FROM Specialty s WHERE s.code = :code AND (s.orgId = :orgId OR s.orgId = '*')")
    Optional<Specialty> findByCodeForOrg(String code, String orgId);

    void deleteByCodeAndOrgId(String code, String orgId);
}
