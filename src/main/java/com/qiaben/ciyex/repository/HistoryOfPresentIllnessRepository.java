package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.HistoryOfPresentIllness;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HistoryOfPresentIllnessRepository extends JpaRepository<HistoryOfPresentIllness, Long> {

    List<HistoryOfPresentIllness> findByOrgIdAndPatientId(Long orgId, Long patientId);

    List<HistoryOfPresentIllness> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);

    Optional<HistoryOfPresentIllness> findByOrgIdAndPatientIdAndEncounterIdAndId(
            Long orgId, Long patientId, Long encounterId, Long id
    );
}
