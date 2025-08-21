package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Procedure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProcedureRepository extends JpaRepository<Procedure, Long> {
    List<Procedure> findByOrgIdAndPatientId(Long orgId, Long patientId);
    List<Procedure> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
    Optional<Procedure> findByOrgIdAndPatientIdAndEncounterIdAndId(Long orgId, Long patientId, Long encounterId, Long id);
}
