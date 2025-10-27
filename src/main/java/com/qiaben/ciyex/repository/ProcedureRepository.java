


package com.qiaben.ciyex.repository;


import com.qiaben.ciyex.entity.Procedure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProcedureRepository extends JpaRepository<Procedure, Long> {
    List<Procedure> findByPatientId(Long patientId);
    List<Procedure> findByPatientIdAndEncounterId(Long patientId, Long encounterId);
    Optional<Procedure> findByPatientIdAndEncounterIdAndId(Long patientId, Long encounterId, Long id);
}
