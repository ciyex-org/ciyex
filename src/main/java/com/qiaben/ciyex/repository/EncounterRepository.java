package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Encounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EncounterRepository extends JpaRepository<Encounter, Long> {
    List<Encounter> findByOrgId(Long orgId);
    List<Encounter> findByPatientIdAndOrgId(Long patientId, Long orgId);

    Optional<Encounter> findByIdAndPatientIdAndOrgId(Long id, Long patientId, Long orgId);

    // Delete with scoping
    long deleteByIdAndPatientIdAndOrgId(Long id, Long patientId, Long orgId);
    // Additional query methods can be added here if necessary
}
